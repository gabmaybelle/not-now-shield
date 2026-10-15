package com.gabbymaybelle.shieldstrip;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a shield lower in first person, idle or raised, so it covers less of the screen.
 * It still blocks exactly as before: only the picture moves. Anything that blocks like a
 * shield (the {@code blocks_attacks} component) counts, so modded shields are lowered too.
 */
public final class LowerShield {
	/** How far down a shield is drawn, in the hand renderer's units (the hand sits at -0.52). */
	public static final float DROP = 0.2F;

	/** Shields drawn lowered so far; the game test reads it to see the hook ran. */
	public static int lowered;

	private LowerShield() {
	}

	/** Called right after the hand renderer starts a hand's pose, before it places the arm. */
	public static void apply(ItemStack stack, PoseStack pose) {
		if (stack.has(DataComponents.BLOCKS_ATTACKS)) {
			pose.translate(0.0F, -DROP, 0.0F);
			lowered++;
		}
	}
}
