package com.gabbymaybelle.shieldstrip.neoforge;

import com.gabbymaybelle.shieldstrip.ShieldstripMixinPlugin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

/** Tells the shared mixin plugin about this NeoForge game: client or server, and the Minecraft version. */
public class NeoForgeMixinPlugin extends ShieldstripMixinPlugin {
	@Override
	protected boolean isClient() {
		return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
	}

	@Override
	protected String minecraftVersion() {
		return FMLLoader.getCurrent().getVersionInfo().mcVersion();
	}
}
