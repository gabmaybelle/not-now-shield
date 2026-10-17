package com.gabbymaybelle.shieldstrip.mixin;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla axes refuse to strip, scrape or unwax while a shield is in the off-hand, unless the
 * player is sneaking. Report "no shield intent" so the axe always tries first; when there is
 * nothing to change, useOn still returns PASS and the off-hand shield is raised as normal.
 * (Hoes and shovels have no such check in 1.21.1, so the axe is the only tool to fix.)
 */
@Mixin(AxeItem.class)
public abstract class AxeItemMixin {
	@Inject(method = "playerHasShieldUseIntent", at = @At("HEAD"), cancellable = true)
	private static void shieldstrip$axeFirst(UseOnContext context, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
