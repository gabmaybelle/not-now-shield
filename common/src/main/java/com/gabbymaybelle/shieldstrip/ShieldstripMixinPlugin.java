package com.gabbymaybelle.shieldstrip;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Picks the mixins that fit this Minecraft version: {@code AxeItemMixin} on 26.2,
 * {@code BlockTransformerMixin} on 26.3 and later, and on a game client (never a server) the
 * shield-lowering mixin for that version's hand renderer. Listing both in the mixin config made
 * Mixin look for the class that isn't there and log an "Error loading class" line; handing
 * it only the right one keeps the log clean.
 *
 * Whether this is a client and which Minecraft version is running are asked of the loader, so
 * the Fabric and NeoForge jars each add a small subclass that answers those two questions.
 */
public abstract class ShieldstripMixinPlugin implements IMixinConfigPlugin {
	/** True on a game client, false on a dedicated server. */
	protected abstract boolean isClient();

	/** The running Minecraft version, such as "26.2" or "26.3-rc-1". */
	protected abstract String minecraftVersion();

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
		boolean is263 = blockTransformerVersion(minecraftVersion());
		List<String> mixins = new ArrayList<>();
		mixins.add(is263 ? "BlockTransformerMixin" : "AxeItemMixin");
		if (isClient()) {
			mixins.add(is263 ? "FirstPersonHandsAndItemsRendererMixin" : "ItemInHandRendererMixin");
		}
		return mixins;
	}

	/**
	 * True on Minecraft 26.3 and later, where tools change blocks through BlockTransformer.
	 * Pre-releases of 26.3 count as 26.3 (they already have it), so only the first two numbers
	 * are read: "26.3", "26.3.1" and "26.3-rc-1" are all 26.3.
	 */
	static boolean blockTransformerVersion(String version) {
		String[] parts = version.split("[^0-9]+");
		int index = 0;
		int major = 0;
		int minor = 0;
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (index == 0) {
				major = Integer.parseInt(part);
			} else if (index == 1) {
				minor = Integer.parseInt(part);
			}
			index++;
			if (index == 2) {
				break;
			}
		}
		return major > 26 || (major == 26 && minor >= 3);
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
