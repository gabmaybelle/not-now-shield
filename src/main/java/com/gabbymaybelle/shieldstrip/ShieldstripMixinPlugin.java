package com.gabbymaybelle.shieldstrip;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Picks the mixins that fit this Minecraft version: {@code AxeItemMixin} on 26.2,
 * {@code BlockTransformerMixin} on 26.3 and later, and on a game client (never a server) the
 * shield-lowering mixin for that version's hand renderer. Listing both in the mixin config made
 * Mixin look for the class that isn't there and log an "Error loading class" line; handing
 * it only the right one keeps the log clean.
 */
public class ShieldstripMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		boolean is263 = blockTransformerVersion();
		List<String> mixins = new ArrayList<>();
		mixins.add(is263 ? "BlockTransformerMixin" : "AxeItemMixin");
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			mixins.add(is263 ? "FirstPersonHandsAndItemsRendererMixin" : "ItemInHandRendererMixin");
		}
		return mixins;
	}

	/** True on Minecraft 26.3 and later, where tools change blocks through BlockTransformer. */
	private static boolean blockTransformerVersion() {
		try {
			return VersionPredicate.parse(">=26.3-").test(
					FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion());
		} catch (VersionParsingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
