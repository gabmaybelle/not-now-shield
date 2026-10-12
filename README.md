# Not Now, Shield

A tiny Fabric mod for Minecraft 26.2 and 26.3: tools change blocks even while a shield is in your off-hand. (Mod ID: `shieldstrip`.)

- **26.2:** axes strip logs, scrape copper and remove wax.
- **26.3:** axes strip, hoes till and shovels make paths.

Vanilla skips these right-click actions whenever a blocking item is in the off-hand (unless you sneak), so the shield goes up instead ([MC-273378](https://bugs-legacy.mojang.com/browse/MC-273378)). This mod lets the tool try first. When there is nothing to change, the click falls through and the shield raises as normal.

It also works for modded tools that hand their right-click to the vanilla axe, hoe or shovel.

## How it works

One jar covers both versions with two mixins that make the off-hand check, `playerHasBlockingItemUseIntent`, return `false`:

| Mixin | Minecraft | Target |
|---|---|---|
| [`AxeItemMixin`](src/main/java/com/gabbymaybelle/shieldstrip/mixin/AxeItemMixin.java) | 26.2 | `AxeItem` |
| [`BlockTransformerMixin`](src/main/java/com/gabbymaybelle/shieldstrip/mixin/BlockTransformerMixin.java) | 26.3 | `BlockTransformer` (data-driven axe, hoe and shovel actions) |

A small mixin plugin, [`ShieldstripMixinPlugin`](src/main/java/com/gabbymaybelle/shieldstrip/ShieldstripMixinPlugin.java), checks the Minecraft version and applies only the mixin that fits, so the other target is never looked up and the log stays clean. Both mixins also name their targets by string and are `@Pseudo`, since each class only exists on one of the two versions.

## Building

Requires JDK 25.

```
./gradlew build
```

The mod jar is written to `build/libs/not-now-shield-<version>.jar`. Copy it into your instance's `mods` folder.

## License

CC0-1.0
