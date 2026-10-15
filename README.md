# Not Now, Shield

Your shield, drawn lower. A tiny mod for Minecraft 1.20.1, on Fabric and Forge. (Mod ID: `shieldstrip`.)

This is the Minecraft 1.20.1 version. The 26.2 and 26.3 version (Fabric) is on the `main` branch, where the mod also lets tools change blocks while a shield is in your off-hand.

Your shield is drawn lower on your screen, resting or raised, so it covers less of your view. It still blocks just the same; only the picture moves, and only on your own screen. Modded shields are lowered too, as long as they are raised like a shield.

## What it doesn't do on 1.20.1

The 26.2 and 26.3 version also lets axes, hoes and shovels work with a shield in your off-hand ([MC-273378](https://bugs.mojang.com/browse/MC/issues/MC-273378)). Minecraft 1.20.1 doesn't have that problem (its axe doesn't check the off-hand), so there is nothing to fix and this version leaves tools alone.

## Requirements

- Minecraft 1.20.1, and Java 17
- On Fabric: Fabric Loader 0.15 or newer (Fabric API isn't needed)
- On Forge: Forge 47 or newer

It is client-side only: it works on any server, and the server doesn't need it.

## How it works

One client-only mixin, [`ItemInHandRendererMixin`](common/src/main/java/com/gabbymaybelle/shieldstrip/mixin/ItemInHandRendererMixin.java), moves the hand down by 0.2 right after `ItemInHandRenderer` starts drawing it, through [`LowerShield`](common/src/main/java/com/gabbymaybelle/shieldstrip/LowerShield.java), when the item is raised like a shield (the block use animation). Fabric and Forge each have a small part that starts the mod; the rest is shared.

## Building

Requires JDK 25 to run Gradle, and a Java 17 install that Gradle can find (Forge's build tools run on it):

```
./gradlew build -Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64
```

(Or `-Dorg.gradle.java.installations.paths=<path to Java 17>`.) The two jars are written to `fabric/build/libs/` and `forge/build/libs/`, named `not-now-shield-fabric-<version>.jar` and `not-now-shield-forge-<version>.jar`. Copy the one for your loader into your instance's `mods` folder. The first Forge build takes a few minutes.

The icon is Gabby's own sprite, drawn in Aseprite: `art/icon.ase` is the source, and `common/src/main/resources/assets/shieldstrip/icon.png` is it scaled up to 512 pixels with each pixel kept sharp.

`./gradlew build` runs the unit tests (which items count as a shield) in `common` and again on Forge's game classes. 1.20.1 has no Fabric in-game test, so the finished jars are checked in a real game by hand.

## License

[MIT](LICENSE), from version 1.1.2. Versions up to 1.1.1 were released under CC0-1.0, and copies of those stay CC0.

Not Now, Shield is made by Gabby with help from Claude, an AI assistant.
