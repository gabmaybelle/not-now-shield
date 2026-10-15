package com.gabbymaybelle.shieldstrip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.UseAnim;
import org.junit.jupiter.api.Test;

class LowerShieldTest {
	@Test
	void anythingRaisedLikeAShieldIsLowered() {
		assertTrue(LowerShield.blocks(UseAnim.BLOCK));
	}

	@Test
	void otherItemsAreLeftAlone() {
		for (UseAnim animation : UseAnim.values()) {
			if (animation != UseAnim.BLOCK) {
				assertFalse(LowerShield.blocks(animation), animation.name());
			}
		}
	}
}
