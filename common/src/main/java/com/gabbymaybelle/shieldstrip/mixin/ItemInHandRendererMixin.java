package com.gabbymaybelle.shieldstrip.mixin;

import com.gabbymaybelle.shieldstrip.LowerShield;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.2, client only: lowers a shield held in first person. ItemInHandRenderer draws each
 * hand inside one pushPose/popPose; moving the pose down right after the push lowers that hand
 * alone. See {@link LowerShield}.
 *
 * The target only exists on 26.2, so it is named by string and marked {@link Pseudo}.
 * ShieldstripMixinPlugin applies {@link FirstPersonHandsAndItemsRendererMixin} on 26.3 and later instead.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.ItemInHandRenderer")
public abstract class ItemInHandRendererMixin {
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
	private void shieldstrip$lowerShield(CallbackInfo ci, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) PoseStack pose) {
		LowerShield.apply(stack, pose);
	}
}
