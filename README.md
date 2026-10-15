# Not Now, Shield

A tiny Fabric mod for Minecraft 26.2 and 26.3: tools change blocks even while a shield is in your off-hand. (Mod ID: `shieldstrip`.)

- **26.2:** axes strip logs, scrape copper and remove wax.
- **26.3:** axes strip, hoes till and shovels make paths.

Vanilla skips these right-click actions whenever a blocking item is in the off-hand (unless you sneak), so the shield goes up instead ([MC-273378](https://bugs.mojang.com/browse/MC/issues/MC-273378)). This mod lets the tool try first. When there is nothing to change, the click falls through and the shield raises as normal.

It also works for modded tools that hand their right-click to the vanilla axe, hoe or shovel.

Your shield is also drawn lower on your screen, resting or raised, so it covers less of your view. It still blocks just the same; only the picture moves, and only on your own screen. Modded shields are lowered too.

## Other versions

Minecraft 1.20.1, on Fabric and on Forge, is on the [`1.20.1` branch](https://github.com/gabmaybelle/not-now-shield/tree/1.20.1), with its download on the [1.20.1 release page](https://github.com/gabmaybelle/not-now-shield/releases/tag/v1.2.0%2B1.20.1). That version only lowers the shield: 1.20.1's axe has no off-hand check, so there is nothing to fix for tools. Downloads for 26.2 and 26.3 are on the [Releases page](https://github.com/gabmaybelle/not-now-shield/releases).

## How it works

One jar covers both versions. Two mixins make the off-hand check, `playerHasBlockingItemUseIntent`, return `false`:

| Mixin | Minecraft | Target |
|---|---|---|
| [`AxeItemMixin`](src/main/java/com/gabbymaybelle/shieldstrip/mixin/AxeItemMixin.java) | 26.2 | `AxeItem` |
| [`BlockTransformerMixin`](src/main/java/com/gabbymaybelle/shieldstrip/mixin/BlockTransformerMixin.java) | 26.3 | `BlockTransformer` (data-driven axe, hoe and shovel actions) |

Two more, on the game client only, lower the shield in first person through [`LowerShield`](src/main/java/com/gabbymaybelle/shieldstrip/LowerShield.java): each moves the hand down by 0.2 right after the hand renderer starts drawing a hand that holds anything with the `blocks_attacks` component.

| Mixin | Minecraft | Target |
|---|---|---|
| [`ItemInHandRendererMixin`](src/main/java/com/gabbymaybelle/shieldstrip/mixin/ItemInHandRendererMixin.java) | 26.2 | `ItemInHandRenderer` |
| [`FirstPersonHandsAndItemsRendererMixin`](src/main/java/com/gabbymaybelle/shieldstrip/mixin/FirstPersonHandsAndItemsRendererMixin.java) | 26.3 | `FirstPersonHandsAndItemsRenderer` (its new name) |

A small mixin plugin, [`ShieldstripMixinPlugin`](src/main/java/com/gabbymaybelle/shieldstrip/ShieldstripMixinPlugin.java), checks the Minecraft version and applies only the mixins that fit (and the drawing ones only on a game client), so the other targets are never looked up and the log stays clean. All four name their targets by string and are `@Pseudo`, since each class only exists on one of the two versions.

## Building

Requires JDK 25.

```
./gradlew build
```

The mod jar is written to `build/libs/not-now-shield-<version>.jar`. Copy it into your instance's `mods` folder.

The icon is Gabby's own sprite, drawn in Aseprite: `art/icon.ase` is the source, and `src/main/resources/assets/shieldstrip/icon.png` is it scaled up to 512 pixels with each pixel kept sharp.

`./gradlew runClientGameTest` starts the game and right-clicks an axe, a hoe and a shovel with a shield in the off-hand, checking each one changes its block, that an axe on stone leaves the click for the shield, and that a shield is drawn lowered, resting and raised (with a screenshot of each). It runs on 26.2; add `-Pminecraft_version=26.3 -Pfabric_api_version=0.162.0+26.3` to run it on 26.3. The tests use Fabric API; the mod itself doesn't.

## License

[MIT](LICENSE), from version 1.1.2. Versions up to 1.1.1 were released under CC0-1.0, and copies of those stay CC0.
