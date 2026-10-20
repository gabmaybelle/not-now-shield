# Not Now, Shield

Your shield stops getting in the way: axes strip logs, scrape copper and remove wax even while a shield is in your off-hand, and your shield is drawn lower so it covers less of your view.

For Fabric and NeoForge on Minecraft 26.2, 26.3 and 1.21.1, and Fabric and Forge on 1.20.1 (the lower shield only there). Download it from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/not-now-shield). (Mod ID: `shieldstrip`.)

This is the Minecraft 1.21.1 version. The 26.2 and 26.3 version is on the `main` branch, where hoes and shovels need the same fix.

Vanilla axes skip these right-click actions whenever a shield is in the off-hand (unless you sneak), so the shield goes up instead ([MC-273378](https://bugs.mojang.com/browse/MC/issues/MC-273378)). This mod lets the axe try first. When there is nothing to change, the click falls through and the shield raises as normal. Hoes and shovels in 1.21.1 never had this problem, so they are left alone. It also works for modded axes that hand their right-click to the vanilla axe.

Your shield is also drawn lower on your screen, resting or raised, so it covers less of your view. It still blocks just the same; only the picture moves, and only on your own screen. Modded shields are lowered too, as long as they are raised like a shield.

## Requirements

- Minecraft 1.21.1, and Java 21
- Fabric: Fabric Loader 0.15 or newer. No Fabric API needed.
- NeoForge: NeoForge 21.1 or newer.

Pick the `fabric` or `neoforge` file for your loader. For multiplayer, install it on the server and on each player's game, so the axe and the shield agree on what happens. The lower shield works on your own game even on a server without the mod.

## Other versions

Minecraft 1.20.1, on Fabric and on Forge, is on the [`1.20.1` branch](https://github.com/gabmaybelle/not-now-shield/tree/1.20.1) (it only lowers the shield: 1.20.1's axe has no off-hand check). Every version's download is on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/not-now-shield) and the [Releases page](https://github.com/gabmaybelle/not-now-shield/releases).

## How it works

[`AxeItemMixin`](common/src/main/java/com/gabbymaybelle/shieldstrip/mixin/AxeItemMixin.java) makes `AxeItem.playerHasShieldUseIntent`, the off-hand check, return `false`.

[`ItemInHandRendererMixin`](common/src/main/java/com/gabbymaybelle/shieldstrip/mixin/ItemInHandRendererMixin.java), on the game client only, moves a hand down by 0.2 right after the hand renderer starts drawing it, through [`LowerShield`](common/src/main/java/com/gabbymaybelle/shieldstrip/LowerShield.java), when the item in it is raised like a shield (the `BLOCK` use animation).

## Building

Requires JDK 21 or newer (the code is compiled for Java 21).

```
./gradlew build
```

The mod jars are written to `fabric/build/libs/not-now-shield-fabric-<version>.jar` and `neoforge/build/libs/not-now-shield-neoforge-<version>.jar`. Copy the one for your loader into your instance's `mods` folder.

The icon is Gabby's own sprite, drawn in Aseprite: `art/icon.ase` is the source, and `common/src/main/resources/assets/shieldstrip/icon.png` is it scaled up to 512 pixels with each pixel kept sharp.

`./gradlew :fabric:runClientGameTest` and `./gradlew :neoforge:runClientTest` start the game and right-click an axe, a hoe and a shovel with a shield in the off-hand, checking each one changes its block, that an axe on stone leaves the click for the shield, and that a shield is drawn lowered, resting and raised (with a screenshot of each). Fabric's game test API doesn't exist for 1.21.1, so the Fabric test brings its own small harness. The tests use Fabric API's tick event; the mod itself doesn't.

## Licence

[MIT](LICENSE), from version 1.1.2. Versions up to 1.1.1 were released under CC0-1.0, and copies of those stay CC0.

Not Now, Shield is made by Gabby with help from Claude, an AI assistant.
