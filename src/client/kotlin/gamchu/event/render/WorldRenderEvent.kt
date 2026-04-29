package gamchu.event.render

import com.mojang.blaze3d.vertex.PoseStack
import gamchu.event.Event
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.culling.Frustum

@Suppress("UNUSED_PARAMETER")
abstract class WorldRenderEvent(val context: WorldRenderContext) : Event() {

    class Start(context: WorldRenderContext) : WorldRenderEvent(context)
    class Last(context: WorldRenderContext) : WorldRenderEvent(context)

}

class WorldRenderContext {

    var matrixStack: PoseStack? = null
    lateinit var consumers: MultiBufferSource
    lateinit var camera: Camera
    lateinit var frustum: Frustum

}