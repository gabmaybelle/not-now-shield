package com.gabbymaybelle.shieldstrip.fabric;

import com.gabbymaybelle.shieldstrip.ShieldstripMixinPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/** Tells the shared mixin plugin about this Fabric game: client or server, and the Minecraft version. */
public class FabricMixinPlugin extends ShieldstripMixinPlugin {
	@Override
	protected boolean isClient() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

	@Override
	protected String minecraftVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
	}
}
