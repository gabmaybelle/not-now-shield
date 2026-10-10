package com.gabbymaybelle.shieldstrip.mixin;

import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 26.3: axe stripping, hoe tilling and shovel flattening all go through
 * the data-driven BlockTransformer, which carries the same off-hand blocking check.
 * Report "no blocking intent" so the tool always tries first; when no transform
 * matches, transformBlock still returns PASS and the off-hand shield is raised.
 *
 * BlockTransformer does not exist in 26.2, so the target is named by string and
 * marked {@link Pseudo}: on 26.2 this mixin is skipped and {@link AxeItemMixin}
 * applies instead.
 */
@Pseudo
@Mixin(targets = "net.minecraft.core.component.BlockTransformer")
public abstract class BlockTransformerMixin {
	@Inject(method = "playerHasBlockingItemUseIntent", at = @At("HEAD"), cancellable = true)
	private static void shieldstrip$toolFirst(UseOnContext context, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
