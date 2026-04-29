package gamchu.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import gamchu.event.render.WorldRenderContext;
import gamchu.event.render.WorldRenderEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

  @Unique
  private final WorldRenderContext ctx = new WorldRenderContext();

  @Shadow
  @Final
  private RenderBuffers renderBuffers;

  @Inject(method = "renderLevel", at = @At("HEAD"))
  private void render(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo callbackInfo) {
    ctx.setConsumers(renderBuffers.bufferSource());
    ctx.setCamera(camera);
    new WorldRenderEvent.Start(ctx).post();
  }


  @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/client/renderer/culling/Frustum;"))
  private Frustum onSetupFrustum(Frustum frustum) {
    ctx.setFrustum(frustum);
    return frustum;
  }

  @Inject(method = "method_62214", at = @At("RETURN"))
  private void postRender(GpuBufferSlice gpuBufferSlice, LevelRenderState levelRenderState, ProfilerFiller profilerFiller, Matrix4f matrix4f, ResourceHandle resourceHandle, ResourceHandle resourceHandle2, boolean bl, ResourceHandle resourceHandle3, ResourceHandle resourceHandle4, CallbackInfo callbackInfo) {
    new WorldRenderEvent.Last(ctx).post();
  }

  @ModifyExpressionValue(method = "method_62214", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
  private PoseStack setInternalStack(PoseStack original) {
    ctx.setMatrixStack(original);
    return original;
  }

}
