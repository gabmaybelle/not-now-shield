package com.gabbymaybelle.shieldstrip.mixin;

import com.gabbymaybelle.shieldstrip.LowerShield;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client only: lowers a shield held in first person. ItemInHandRenderer draws each hand inside
 * one pushPose/popPose; moving the pose down right after the push lowers that hand alone. See
 * {@link LowerShield}. The handler takes the method's own arguments, so no local-variable
 * capture library is needed.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
	private void shieldstrip$lowerShield(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
			float swingProgress, ItemStack stack, float equipProgress, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		LowerShield.apply(stack, pose);
	}
}
