package gamchu.render.api

import gamchu.render.util.NVGRenderer
import gamchu.render.util.helper.Gradient
import net.minecraft.client.Minecraft

class NvgProvider {

    private sealed interface Cmd {
        fun run()

        data class Rect(val x: Float, val y: Float, val w: Float, val h: Float, val color: Int, val radius: Float) : Cmd {
            override fun run() = NVGRenderer.rect(x, y, w, h, color, radius)
        }
        data class RectFlat(val x: Float, val y: Float, val w: Float, val h: Float, val color: Int) : Cmd {
            override fun run() = NVGRenderer.rect(x, y, w, h, color)
        }
        data class HollowRect(val x: Float, val y: Float, val w: Float, val h: Float, val thickness: Float, val color: Int, val radius: Float) : Cmd {
            override fun run() = NVGRenderer.hollowRect(x, y, w, h, thickness, color, radius)
        }
        data class Circle(val x: Float, val y: Float, val radius: Float, val color: Int) : Cmd {
            override fun run() = NVGRenderer.circle(x, y, radius, color)
        }
        data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val thickness: Float, val color: Int) : Cmd {
            override fun run() = NVGRenderer.line(x1, y1, x2, y2, thickness, color)
        }
        data class HalfRoundedRect(val x: Float, val y: Float, val w: Float, val h: Float, val color: Int, val radius: Float, val roundTop: Boolean) : Cmd {
            override fun run() = NVGRenderer.drawHalfRoundedRect(x, y, w, h, color, radius, roundTop)
        }

        data class GradientRect(val x: Float, val y: Float, val w: Float, val h: Float, val c1: Int, val c2: Int, val g: Gradient, val radius: Float) : Cmd {
            override fun run() = NVGRenderer.gradientRect(x, y, w, h, c1, c2, g, radius)
        }
        data class HollowGradientRect(val x: Float, val y: Float, val w: Float, val h: Float, val thickness: Float, val c1: Int, val c2: Int, val g: Gradient, val radius: Float) : Cmd {
            override fun run() = NVGRenderer.hollowGradientRect(x, y, w, h, thickness, c1, c2, g, radius)
        }

        data class Text(val text: String, val x: Float, val y: Float, val size: Float, val color: Int) : Cmd {
            override fun run() = NVGRenderer.text(text, x, y, size, color)
        }
        data class TextShadow(val text: String, val x: Float, val y: Float, val size: Float, val color: Int) : Cmd {
            override fun run() = NVGRenderer.textShadow(text, x, y, size, color)
        }
        data class WrappedText(val text: String, val x: Float, val y: Float, val w: Float, val size: Float, val color: Int) : Cmd {
            override fun run() = NVGRenderer.drawWrappedString(text, x, y, w, size, color)
        }

        data class DrawImage(val resourcePath: String, val x: Float, val y: Float, val w: Float, val h: Float, val radius: Float, val colorMask: Int) : Cmd {
            override fun run() {
                val img = NVGRenderer.createImage(resourcePath)
                NVGRenderer.image(img, x, y, w, h, radius, colorMask)
            }
        }

        object Push : Cmd { override fun run() = NVGRenderer.push() }
        object Pop  : Cmd { override fun run() = NVGRenderer.pop()  }
        data class Translate(val x: Float, val y: Float) : Cmd { override fun run() = NVGRenderer.translate(x, y) }
        data class Scale(val x: Float, val y: Float)     : Cmd { override fun run() = NVGRenderer.scale(x, y)     }
        data class Rotate(val amount: Float)              : Cmd { override fun run() = NVGRenderer.rotate(amount)  }
        data class GlobalAlpha(val amount: Float)         : Cmd { override fun run() = NVGRenderer.globalAlpha(amount) }

        data class PushScissor(val x: Float, val y: Float, val w: Float, val h: Float) : Cmd {
            override fun run() = NVGRenderer.pushScissor(x, y, w, h)
        }
        object PopScissor : Cmd { override fun run() = NVGRenderer.popScissor() }
    }

    private val layers = LinkedHashMap<String, List<Cmd>>()
    private val layerMeta = HashMap<String, LayerMeta>()
    private val lock = Any()
    private var frameCounter = 0L

    private data class LayerMeta(var lastFrame: Long, val lifetime: Int)

    private val callbacks = LinkedHashMap<String, Runnable>()

    @Volatile private var pendingId: String? = null
    private var pendingLifetime = -1
    private val pending = mutableListOf<Cmd>()


    @JvmName("beginLayer")
    fun beginLayer(id: String) {
        pendingId = id
        pendingLifetime = -1
        pending.clear()
    }

    @JvmName("beginLayer")
    fun beginLayer(id: String, lifetime: Int) {
        pendingId = id
        pendingLifetime = lifetime
        pending.clear()
    }

    @JvmName("endLayer")
    fun endLayer() {
        val id = pendingId ?: return
        val snapshot = pending.toList()
        val lt = pendingLifetime
        synchronized(lock) {
            layers[id] = snapshot
            layerMeta[id] = LayerMeta(frameCounter, lt)
        }
        pendingId = null
        pendingLifetime = -1
        pending.clear()
    }

    @JvmName("removeLayer")
    fun removeLayer(id: String) {
        synchronized(lock) { layers.remove(id); layerMeta.remove(id) }
    }

    @JvmName("clearAll")
    fun clearAll() {
        synchronized(lock) { layers.clear(); layerMeta.clear(); callbacks.clear() }
        pendingId = null
        pendingLifetime = -1
        pending.clear()
    }

    @JvmName("getLayerIds")
    fun getLayerIds(): List<String> = synchronized(lock) { layers.keys.toList() }

    fun registerCallback(id: String, callback: Runnable) {
        synchronized(lock) { callbacks[id] = callback }
    }

    fun unregisterCallback(id: String) {
        synchronized(lock) { callbacks.remove(id) }
    }

    internal fun isEmpty(): Boolean = synchronized(lock) { layers.isEmpty() && callbacks.isEmpty() }

    internal fun runAll() {
        frameCounter++
        val layerSnapshot: List<List<Cmd>>
        val cbSnapshot: List<Runnable>
        synchronized(lock) {
            // Prune expired layers
            val expired = layerMeta.entries
                .filter { (_, m) -> m.lifetime > 0 && (frameCounter - m.lastFrame) > m.lifetime }
                .map { it.key }
            for (id in expired) {
                layers.remove(id)
                layerMeta.remove(id)
            }

            layerSnapshot = layers.values.toList()
            cbSnapshot = callbacks.values.toList()
        }

        for (layer in layerSnapshot) {
            for (cmd in layer) {
                try { cmd.run() }
                catch (e: Exception) { System.err.println("[NvgAPI] Command error: ${e.message}") }
            }
        }
        for (cb in cbSnapshot) {
            try { cb.run() }
            catch (e: Exception) { System.err.println("[NvgAPI] Callback error: ${e.message}") }
        }
    }

    @JvmName("getScreenWidth")
    fun getScreenWidth(): Float = Minecraft.getInstance().window.guiScaledWidth.toFloat()

    @JvmName("getScreenHeight")
    fun getScreenHeight(): Float = Minecraft.getInstance().window.guiScaledHeight.toFloat()

    private fun rec(cmd: Cmd) { pending.add(cmd) }

    @JvmName("push")        fun push() = rec(Cmd.Push)
    @JvmName("pop")         fun pop() = rec(Cmd.Pop)
    @JvmName("translate")   fun translate(x: Float, y: Float) = rec(Cmd.Translate(x, y))
    @JvmName("scale")       fun scale(x: Float, y: Float) = rec(Cmd.Scale(x, y))
    @JvmName("rotate")      fun rotate(amount: Float) = rec(Cmd.Rotate(amount))
    @JvmName("globalAlpha") fun globalAlpha(amount: Float) = rec(Cmd.GlobalAlpha(amount))
    @JvmName("pushScissor") fun pushScissor(x: Float, y: Float, w: Float, h: Float) = rec(Cmd.PushScissor(x, y, w, h))
    @JvmName("popScissor")  fun popScissor() = rec(Cmd.PopScissor)

    @JvmName("rect")
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Long, radius: Float) = rec(Cmd.Rect(x, y, w, h, color.toInt(), radius))

    @JvmName("rect")
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Long)  = rec(Cmd.RectFlat(x, y, w, h, color.toInt()))

    @JvmName("hollowRect")
    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Long, radius: Float) = rec(Cmd.HollowRect(x, y, w, h, thickness, color.toInt(), radius))

    @JvmName("circle")
    fun circle(x: Float, y: Float, radius: Float, color: Long) = rec(Cmd.Circle(x, y, radius, color.toInt()))

    @JvmName("line")
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Long) = rec(Cmd.Line(x1, y1, x2, y2, thickness, color.toInt()))

    @JvmName("drawHalfRoundedRect")
    fun drawHalfRoundedRect(x: Float, y: Float, w: Float, h: Float, color: Long, radius: Float, roundTop: Boolean) =
        rec(Cmd.HalfRoundedRect(x, y, w, h, color.toInt(), radius, roundTop))

    @JvmName("gradientRect")
    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Long, color2: Long, gradient: Gradient, radius: Float) =
        rec(Cmd.GradientRect(x, y, w, h, color1.toInt(), color2.toInt(), gradient, radius))

    @JvmName("gradientRect")
    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Long, color2: Long, direction: String, radius: Float) =
        rec(Cmd.GradientRect(x, y, w, h, color1.toInt(), color2.toInt(), parseGradient(direction), radius))

    @JvmName("hollowGradientRect")
    fun hollowGradientRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color1: Long, color2: Long, gradient: Gradient, radius: Float) =
        rec(Cmd.HollowGradientRect(x, y, w, h, thickness, color1.toInt(), color2.toInt(), gradient, radius))

    @JvmName("hollowGradientRect")
    fun hollowGradientRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color1: Long, color2: Long, direction: String, radius: Float) =
        rec(Cmd.HollowGradientRect(x, y, w, h, thickness, color1.toInt(), color2.toInt(), parseGradient(direction), radius))
    @JvmName("text")
    fun text(text: String, x: Float, y: Float, size: Float, color: Long) = rec(Cmd.Text(text, x, y, size, color.toInt()))

    @JvmName("textShadow")
    fun textShadow(text: String, x: Float, y: Float, size: Float, color: Long) = rec(Cmd.TextShadow(text, x, y, size, color.toInt()))

    @JvmName("textWidth")
    fun textWidth(text: String, size: Float): Float = NVGRenderer.textWidth(text, size)

    @JvmName("drawWrappedString")
    fun drawWrappedString(text: String, x: Float, y: Float, w: Float, size: Float, color: Long) =
        rec(Cmd.WrappedText(text, x, y, w, size, color.toInt()))
     @JvmName("image")
    fun image(resourcePath: String, x: Float, y: Float, w: Float, h: Float, radius: Float, colorMask: Long) =
        rec(Cmd.DrawImage(resourcePath, x, y, w, h, radius, colorMask.toInt()))

    @JvmName("image")
    fun image(resourcePath: String, x: Float, y: Float, w: Float, h: Float) =
        rec(Cmd.DrawImage(resourcePath, x, y, w, h, 0f, 0))

    private fun parseGradient(s: String): Gradient = when (s.uppercase()) {
        "TOP_TO_BOTTOM" -> Gradient.TopToBottom
        "DIAGONAL"      -> Gradient.TopLeftToBottomRight
        else            -> Gradient.LeftToRight
    }
}
