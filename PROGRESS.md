# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends, saying what changed, what the game test counted, what the proof run was (the feature off,
which checks fail, code put back), what was learned, and what was deliberately not done.

## Where things stand

- **Done:** Not Now, Shield 1.2.0 (mod ID `shieldstrip`): tools change blocks with a shield in the off-hand, on 26.2 and 26.3; the shield drawn lower in first person, and an icon with a shield behind the axe (see the 2026-10-15 Lower shield row); a client game test, CI in game on both versions, Gabby's own pixel-art icon. The live line is branch `public`, pushed to the remote `public` as `main`.
- **Broken:** Nothing known as of this writing (not re-verified).
- **Next:** Gabby uploads 1.2.0 to Modrinth (`modrinth/listing.md`, Step 3) and updates the project icon. Keep it building on new Minecraft versions.

## Log

| Date | Session | What happened |
| --- | --- | --- |
| 2026-10-15 | Lower shield | The shield is drawn 0.2 lower in first person, resting and raised, so it covers less of the view (Gabby asked for it "like other lower shield mods"). `LowerShield` moves the pose down right after the hand renderer's `pushPose` in `submitArmWithItem`, for any stack with `blocks_attacks` (modded shields too). 26.2's `ItemInHandRenderer` is `FirstPersonHandsAndItemsRenderer` on 26.3, so one mixin each; the plugin adds them only on a client. Game test: the hook runs with the shield resting and raised (raised by holding the use key; `startUsingItem` on the client alone didn't hold). Proof run with `apply` switched off: "a resting shield wasn't drawn lower", "a raised shield wasn't drawn lower", code put back. Passed on 26.2 and 26.3; screenshots read: raised, the shield's top sits well below the crosshair (vanilla reaches it); resting, it's mostly below the hotbar. Icon: Gabby asked for a Minecraft-style shield, and a 9x11 oak-and-iron one (drawn in the icon's own palette, not copied from Mojang's texture) is on its own `Shield` layer under her axe layer in `art/icon.ase`, written by script since Aseprite isn't on this PC; `icon.png` re-exported at 512. Version 1.2.0; Modrinth guide updated. Not done: no setting for the amount. |
| 2026-10-15 | Notes | `PROGRESS.md` added, with the rows below reconstructed from the commit history (one row per day, the commits' own words); the two shared rules (commits one at a time by hand, proof runs for data changes) added to `CLAUDE.md`. No code change. |
| 2026-10-14 | (from history) | Drop the template's modid comment from settings.gradle. |
| 2026-10-13 | (from history) | A new icon: Gabby's own pixel-art axe; CI caches Gradle and runs once a push; CI tests in game on Minecraft 26.2 and 26.3 on every push; Notes for Claude: how this repo works and the rules every session follows; Client game test: tools work with a shield in the off-hand. |
| 2026-10-12 | (from history) | Trim the build file to what the mod uses; Homepage link, so Mod Menu's Website button works; MIT License from 1.1.2; Working bug-report link, and Mod Menu links to the source and issues; Pin Loom to 1.18.4 instead of the moving 1.18-SNAPSHOT; Not Now, Shield 1.1.2: no "Error loading class" line on 26.3. |
| 2026-10-09 | (from history) | Replace placeholder icon with shield-and-axe pixel art; Rename Gradle project to not-now-shield; Not Now, Shield 1.1.1. |
