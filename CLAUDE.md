# Not Now, Shield

A tiny client-side mod for Minecraft 1.20.1 on Fabric and Forge (mod ID `shieldstrip`; this branch, `main` is 26.2 and
26.3 on Fabric): the shield is drawn lower in first person so it covers less of the view. On
`main` the mod also lets tools change blocks while a shield is in the off-hand; 1.20.1's axe has
no off-hand check, so that half has nothing to fix here. See README.md.

`PROGRESS.md` says what's done, what's broken and what's next, with a row per session: read
it first, and add this session's row before ending.

## Layout

Three Gradle projects, like Nightfall Clock's 1.20.1 branch: `common` holds everything that doesn't care about the loader, and `fabric` and `forge` compile those sources straight into their own jars.

- `common/src/main/java/com/gabbymaybelle/shieldstrip/`: `LowerShield` (the pose change and what counts as a shield) and `mixin/ItemInHandRendererMixin` (client only; takes the renderer's own arguments, no local-capture library). `common/src/main/resources/`: the mixin config and the icon. `common/src/test/`: the unit test.
- `fabric/`: `fabric.mod.json` only (client environment; it needs Fabric Loader, not Fabric API).
- `forge/`: `ShieldstripForge` (an empty `@Mod` class, since Forge needs one), `META-INF/mods.toml`, and `pack.mcmeta` (Forge shows a warning screen without it).
- `art/icon.ase`: Gabby's own Aseprite sprite (16x16) for the icon. `icon.png` is it scaled to
  512x512 with nearest-neighbour. Never replace it with generated art; re-export from this file.

There is no in-game test mod on this branch: 1.20.1 has no Fabric client game test.

## Build and test

Needs JDK 25 to run Gradle, plus a Java 17 install that Gradle can find (Forge's tools run on it): set `-Dorg.gradle.java.installations.paths=<path to Java 17>` or `-Dorg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64`. The code is compiled for Java 17 whichever JDK compiles it.

- `./gradlew build`: `fabric/build/libs/not-now-shield-fabric-<version>.jar` and `forge/build/libs/not-now-shield-forge-<version>.jar`; runs the unit test in `common` and on Forge's classes.
- `./gradlew :fabric:runClient` and `:forge:runClient`: a dev game with the mod.

CI (`.github/workflows/build.yml`) builds both jars and runs the tests, with Java 17 and 25 installed. A live check in a 1.20.1 game (a shield drawn lower, resting and raised) is done by hand before a release.

## Gotchas

- **The mod needs only Fabric Loader, not Fabric API.** Keep it out of the mod's own dependencies.
- **Forge refmap.** The shared `shieldstrip.mixins.json` names no refmap; Loom adds `shieldstrip-refmap.json` on Fabric and the Forge build adds the `refmap` line while copying resources.
- **The mixin target was checked in the build, not in game.** The mixin annotation processor wrote a refmap for `renderArmWithItem` and `PoseStack.pushPose` on both Fabric (intermediary) and Forge (SRG) names, so the target exists; how it draws needs a look in a real 1.20.1 game.
- The GitHub repo is github.com/gabmaybelle/not-now-shield: local `main` pushes to `origin`.
  The remote `shieldstrip-private` is an old private repo and must not be pushed to.
- Licence: MIT from 1.1.2; versions up to 1.1.1 were CC0-1.0.

## Working on Gabby's mods (the same in every repo)

Gabby makes these mods with help from Claude. Several Claude sessions work on them: one on her
computer and others in the cloud. These rules keep their work fitting together.

**One session per repo at a time.**
- A cloud session works on a `claude/...` branch and never pushes to `main`. Gabby's local
  session reviews the branch, runs its tests and merges it as one commit.
- Start from the latest `main` (`git pull`), and say in the branch's last commit or a short note
  what is finished and what isn't.

**Commits are made one at a time, by hand.** Never commit or push from a chained script. Run
the test, read its result (and the screenshot, for anything visual), then commit that one
branch, then push it. A cherry-pick that conflicts stops the work until it is resolved by hand.
A worldgen or balance change is tested on every terrain in the branch's matrix before it goes
to any other branch. (Learned on Beyond the Dragon, 2026-10-14, when scripted chains committed
a conflicted cherry-pick, put one branch's files on another, and sent an untested ore change to
all three branches.) For a Gradle game test use the toolkit's watcher (`~/.claude/CLAUDE.md`),
not a wait loop.

**Commits.**
- Author and committer are always `gabmaybelle <211247938+gabmaybelle@users.noreply.github.com>`:
  `git -c user.name=gabmaybelle -c user.email=211247938+gabmaybelle@users.noreply.github.com commit ...`.
  Never a personal email, and never `Claude <noreply@anthropic.com>` as the author. Claude goes in
  a `Co-Authored-By:` line at the end of the message.
- Ask Gabby before committing or pushing when she's around. Finish the build and the tests first.
- Commit messages say what changed for players, in plain sentences.

**Privacy.** These repos are public. Don't add personal email addresses, real names other than
"Gabby", or anything about her other private projects and modpacks: not in code, docs, commits
or test names. Check the diff for these before every push.

**Tests.**
- `./gradlew build` and `./gradlew runClientGameTest` (where the repo has one) must pass before
  a commit. The game test opens a real game window for a minute or two.
- A new or changed check should fail with the feature switched off: run it that way once, then
  put the code back. A check that compares against the setting it tests proves nothing.
- A data change (worldgen, loot, a template, a pack's mod list) gets the same proof: run the part
  of the test that sees it, and quote the numbers in the progress row.
- On Linux without a screen, run game tests under `xvfb-run -a`. 26.3 also needs
  `SDL_VIDEO_FORCE_EGL=1` with `libegl1 libegl-mesa0` installed, or the game hangs at startup.

**Writing.**
- README and CHANGELOG are for players: plain, friendly, no jargon. New player-facing changes
  go in the CHANGELOG's newest section.
- Keep the credit line: "made by Gabby with help from Claude, an AI assistant".
- Gabby publishes on Modrinth herself. Don't upload anything there.

**Minecraft.** On this branch: Fabric and Forge on Minecraft 1.20.1 (official Mojang names; Loom 1.11.8, ModDevGradle's legacy mode for Forge; code for Java 17, built on JDK 25). `main` is Fabric on 26.x (Loom 1.18.4, Java 25), where vanilla names changed a lot: check the merged jar rather than older docs.
