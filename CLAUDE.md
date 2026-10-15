# Not Now, Shield

A tiny Fabric mod for Minecraft 26.2 and 26.3 (mod ID `shieldstrip`): tools change blocks even
while a shield is in the off-hand, and the shield is drawn lower in first person so it covers less
of the view. One jar covers both versions. See README.md.

`PROGRESS.md` says what's done, what's broken and what's next, with a row per session: read
it first, and add this session's row before ending.

## Layout

- `src/main/java/com/gabbymaybelle/shieldstrip/`: two mixins (`AxeItemMixin` for 26.2,
  `BlockTransformerMixin` for 26.3); two client-only drawing mixins that call `LowerShield`
  (`ItemInHandRendererMixin` for 26.2, `FirstPersonHandsAndItemsRendererMixin` for 26.3, the
  same renderer renamed); and `ShieldstripMixinPlugin`, which applies only the ones that fit the
  running version, and the drawing ones only on a game client.
- `src/gametest/`: the client game test (`ShieldstripClientTest`).
- `art/icon.ase`: Gabby's own Aseprite sprite (16x16) for the icon. `icon.png` is it scaled to
  512x512 with nearest-neighbour. Never replace it with generated art; re-export from this file.

## Build and test

- `./gradlew build`: builds against 26.2 (the jar players get).
- `./gradlew runClientGameTest`: right-clicks tools with a shield in the off-hand, then holds
  right-click to raise the shield and checks it's drawn lower (screenshots `shield-resting` and
  `shield-raised` in `build/run/clientGameTest/screenshots/`). Add
  `-Pminecraft_version=26.3 -Pfabric_api_version=0.162.0+26.3` to run it on 26.3. Run both.

## Gotchas

- **The mod needs only Fabric Loader, not Fabric API.** Fabric API is added for the tests alone
  (`gametestImplementation`); keep it out of the mod's own dependencies.
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
all three branches.) For a Gradle game test use a game watcher that reports a verdict (in the world, error screen, crashed or hung),
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

**Minecraft.** Fabric on Minecraft 26.3 (Mojang names; Loom 1.18.4, Java 25). Vanilla names
changed a lot in 26.x: check the merged jar rather than older docs (for example `iron_chain`,
`pink_petals[flower_amount=N]`, timelines instead of `time query daytime`).
