package gamchu.mixin.mixins;

import gamchu.event.render.NvgEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrameNumber()V", shift = At.Shift.AFTER))
  public void renderNvg(DeltaTracker counter, boolean tick, CallbackInfo callbackInfo) {
    new NvgEvent().post();
  }

}
