package com.gabbymaybelle.shieldstrip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShieldstripMixinPluginTest {
	@Test
	void twentySixTwoUsesTheAxeMixin() {
		assertFalse(ShieldstripMixinPlugin.blockTransformerVersion("26.2"));
		assertFalse(ShieldstripMixinPlugin.blockTransformerVersion("26.2.1"));
		assertFalse(ShieldstripMixinPlugin.blockTransformerVersion("26.1"));
	}

	@Test
	void twentySixThreeAndLaterUseTheBlockTransformer() {
		assertTrue(ShieldstripMixinPlugin.blockTransformerVersion("26.3"));
		assertTrue(ShieldstripMixinPlugin.blockTransformerVersion("26.3.1"));
		assertTrue(ShieldstripMixinPlugin.blockTransformerVersion("26.4"));
		assertTrue(ShieldstripMixinPlugin.blockTransformerVersion("27.1"));
	}

	@Test
	void preReleasesOfTwentySixThreeCountAsTwentySixThree() {
		assertTrue(ShieldstripMixinPlugin.blockTransformerVersion("26.3-rc-1"));
		assertTrue(ShieldstripMixinPlugin.blockTransformerVersion("26.3-snapshot-2"));
		assertFalse(ShieldstripMixinPlugin.blockTransformerVersion("26.2-rc-1"));
	}

	@Test
	void aServerGetsOnlyTheToolMixin() {
		assertEquals(List.of("AxeItemMixin"), plugin(false, "26.2").getMixins());
		assertEquals(List.of("BlockTransformerMixin"), plugin(false, "26.3").getMixins());
	}

	@Test
	void aClientAlsoGetsTheShieldLoweringMixin() {
		assertEquals(List.of("AxeItemMixin", "ItemInHandRendererMixin"), plugin(true, "26.2").getMixins());
		assertEquals(List.of("BlockTransformerMixin", "FirstPersonHandsAndItemsRendererMixin"), plugin(true, "26.3").getMixins());
	}

	private static ShieldstripMixinPlugin plugin(boolean client, String version) {
		return new ShieldstripMixinPlugin() {
			@Override
			protected boolean isClient() {
				return client;
			}

			@Override
			protected String minecraftVersion() {
				return version;
			}
		};
	}
}
