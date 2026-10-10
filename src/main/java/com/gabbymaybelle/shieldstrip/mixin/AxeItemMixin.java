package com.gabbymaybelle.shieldstrip.mixin;

import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 26.2: vanilla axes refuse to strip, scrape or unwax while a blocking
 * item (a shield) is in the off-hand, unless the player is sneaking. Report "no
 * blocking intent" so the axe always tries first; when there is nothing to change,
 * useOn still returns PASS and the off-hand shield is raised as normal.
 *
 * AxeItem no longer exists in 26.3, so the target is named by string and marked
 * {@link Pseudo}: on 26.3 this mixin is skipped and {@link BlockTransformerMixin}
 * applies instead.
 */
@Pseudo
@Mixin(targets = "net.minecraft.world.item.AxeItem")
public abstract class AxeItemMixin {
	@Inject(method = "playerHasBlockingItemUseIntent", at = @At("HEAD"), cancellable = true)
	private static void shieldstrip$axeFirst(UseOnContext context, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
