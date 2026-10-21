package com.gabbymaybelle.shieldstrip.film;

import com.gabbymaybelle.shieldstrip.VideoTour;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Filming only (it lives in the game tests, never in the mod's jar): puts a shield back up by
 * {@link VideoTour#lift}, at the same point and on the same condition as the mod lowers it. With
 * the lift at {@link com.gabbymaybelle.shieldstrip.LowerShield#DROP} the shield is drawn exactly
 * where vanilla draws it, so the Short's "before" frames show vanilla's height in the same take,
 * from the same spot, with nothing else changed. The lift is 0 unless a scene sets it.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer")
public abstract class FilmShieldMixin {
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER), require = 0)
	private void shieldstripFilm$lift(CallbackInfo ci, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) PoseStack pose) {
		float lift = VideoTour.lift;
		if (lift != 0.0F && stack.has(DataComponents.BLOCKS_ATTACKS)) {
			pose.translate(0.0F, lift, 0.0F);
		}
	}
}
