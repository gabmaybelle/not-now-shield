# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends, saying what changed, what the game test counted, what the proof run was (the feature off,
which checks fail, code put back), what was learned, and what was deliberately not done.

## Where things stand

- **Done:** Not Now, Shield 1.1.2 (mod ID `shieldstrip`): tools change blocks with a shield in the off-hand, on 26.2 and 26.3; a client game test, CI in game on both versions, Gabby's own pixel-art icon. The live line is branch `public`, pushed to the remote `public` as `main`.
- **Broken:** Nothing known as of this writing (not re-verified).
- **Next:** Nothing planned; keep it building on new Minecraft versions.

## Log

| Date | Session | What happened |
| --- | --- | --- |
| 2026-10-15 | Notes | `PROGRESS.md` added, with the rows below reconstructed from the commit history (one row per day, the commits' own words); the two shared rules (commits one at a time by hand, proof runs for data changes) added to `CLAUDE.md`. No code change. |
| 2026-10-14 | (from history) | Drop the template's modid comment from settings.gradle. |
| 2026-10-13 | (from history) | A new icon: Gabby's own pixel-art axe; CI caches Gradle and runs once a push; CI tests in game on Minecraft 26.2 and 26.3 on every push; Notes for Claude: how this repo works and the rules every session follows; Client game test: tools work with a shield in the off-hand. |
| 2026-10-12 | (from history) | Trim the build file to what the mod uses; Homepage link, so Mod Menu's Website button works; MIT License from 1.1.2; Working bug-report link, and Mod Menu links to the source and issues; Pin Loom to 1.18.4 instead of the moving 1.18-SNAPSHOT; Not Now, Shield 1.1.2: no "Error loading class" line on 26.3. |
| 2026-10-09 | (from history) | Replace placeholder icon with shield-and-axe pixel art; Rename Gradle project to not-now-shield; Not Now, Shield 1.1.1. |
