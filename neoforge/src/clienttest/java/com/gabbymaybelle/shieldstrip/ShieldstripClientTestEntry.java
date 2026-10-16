package com.gabbymaybelle.shieldstrip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The mod has no code of its own to start on NeoForge (it's all mixins), so in the test run this
 * is its entry class: it starts the in-game test. Only in dev runs; never in the jar.
 */
@Mod(value = "shieldstrip", dist = Dist.CLIENT)
public class ShieldstripClientTestEntry {
	public ShieldstripClientTestEntry() {
		if (ClientTest.ENABLED) {
			NeoForge.EVENT_BUS.addListener(ClientTest::onTick);
		}
	}
}
