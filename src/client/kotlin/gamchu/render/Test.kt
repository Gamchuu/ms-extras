package gamchu.render

import gamchu.event.EventBus
import gamchu.event.annotation.SubscribeEvent
import gamchu.event.render.NvgEvent
import gamchu.render.util.NVGRenderer
import net.minecraft.client.Minecraft

object TestRenderer {
    init {
        EventBus.register(this)
    }

    @SubscribeEvent
    fun onNvg(event: NvgEvent) {
        val mc = Minecraft.getInstance()
        val window = mc.window
        val width = window.guiScaledWidth.toFloat()
        val height = window.guiScaledHeight.toFloat()

        NVGRenderer.beginFrame(width, height)
        
        // Draw a test red background rounded rect
        val colorRed = 0xFFFF0000.toInt() 
        NVGRenderer.rect(10f, 10f, 100f, 100f, colorRed, 10f)

        // Draw a text inside
        val colorWhite = 0xFFFFFFFF.toInt()
        NVGRenderer.text("NanoVG Test", 20f, 50f, 16f, colorWhite)
        
        NVGRenderer.endFrame()
    }
}