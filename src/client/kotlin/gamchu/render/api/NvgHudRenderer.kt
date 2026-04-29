package gamchu.render.api

import gamchu.event.EventBus
import gamchu.event.annotation.SubscribeEvent
import gamchu.event.render.NvgEvent
import gamchu.render.util.NVGRenderer
import net.minecraft.client.Minecraft

object NvgHudRenderer {

    private val provider: NvgProvider get() = NvgAPI.getProvider()

    init {
        EventBus.register(this)
    }

    @SubscribeEvent
    fun onNvg(event: NvgEvent) {
        val mc = Minecraft.getInstance()
        val window = mc.window
        val width = window.guiScaledWidth.toFloat()
        val height = window.guiScaledHeight.toFloat()

        // Nothing to draw? Skip the whole frame to avoid unnecessary GL work.
        if (provider.isEmpty()) return

        NVGRenderer.beginFrame(width, height)
        provider.runAll()
        NVGRenderer.endFrame()
    }
}
