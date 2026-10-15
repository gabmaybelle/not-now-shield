package com.gabbymaybelle.shieldstrip;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Draws a shield lower in first person, idle or raised, so it covers less of the screen.
 * It still blocks exactly as before: only the picture moves. Anything that is raised like a
 * shield (the BLOCK use animation) counts, so modded shields are lowered too.
 */
public final class LowerShield {
	/** How far down a shield is drawn, in the hand renderer's units (the hand sits at -0.52). */
	public static final float DROP = 0.2F;

	private LowerShield() {
	}

	/** Whether an item with this use animation is raised like a shield. */
	public static boolean blocks(UseAnim animation) {
		return animation == UseAnim.BLOCK;
	}

	/** Called right after the hand renderer starts a hand's pose, before it places the arm. */
	public static void apply(ItemStack stack, PoseStack pose) {
		if (blocks(stack.getUseAnimation())) {
			pose.translate(0.0F, -DROP, 0.0F);
		}
	}
}
