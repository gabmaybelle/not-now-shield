# Not Now, Shield

A tiny Fabric and NeoForge mod for Minecraft 1.21.1 (this branch; `main` is 26.2 and 26.3, `1.20.1` is Fabric and Forge; mod ID `shieldstrip`): axes strip, scrape and unwax even while a shield is in the off-hand, and the shield is drawn lower in first person so it covers less of the view. See README.md.

`PROGRESS.md` says what's done, what's broken and what's next, with a row per session: read
it first, and add this session's row before ending.

## Layout

- `common/src/main/java/com/gabbymaybelle/shieldstrip/`: `AxeItemMixin` (makes the axe's off-hand shield check, `playerHasShieldUseIntent`, return false), the client-only `ItemInHandRendererMixin` that calls `LowerShield`, and `shieldstrip.mixins.json` in the resources. Hoes and shovels have no such check on 1.21.1.
- `fabric/`: `fabric.mod.json`, and `src/gametest/` (the client game test `ShieldstripClientTest` plus its own small harness in `harness/`, since Fabric's client game test API doesn't exist for 1.21.1).
- `neoforge/`: `neoforge.mods.toml` and `src/clienttest/` (the in-game test). No code of its own: a NeoForge mod with no `@Mod` class loads fine.
- `art/icon.ase`: Gabby's own Aseprite sprite (16x16) for the icon. `icon.png` is it scaled to
  512x512 with nearest-neighbour. Never replace it with generated art; re-export from this file.

## Build and test

- `./gradlew build`: Java 21, official Mojang names (Loom remaps for Fabric): `fabric/build/libs/not-now-shield-fabric-<version>.jar` and `neoforge/build/libs/not-now-shield-neoforge-<version>.jar`. The unit tests run on both loaders.
- `./gradlew :fabric:runClientGameTest` and `./gradlew :neoforge:runClientTest` open a real game and run the same checks (axe strips with a shield in the off-hand, hoe and shovel as vanilla, axe on stone leaves the click, shield drawn lowered resting and raised).

## Gotchas

- **The mod needs only the loader (Fabric Loader or NeoForge), not Fabric API.** Fabric API is on the Fabric build only for the game test's tick event.
- The GitHub repo is github.com/gabmaybelle/not-now-shield: this branch pushes to `origin` as `1.21.1`.
- Licence: MIT from 1.1.2; versions up to 1.1.1 were CC0-1.0.

## Gabby's mods: who does what (the same in every repo)

One job per mod, decided 2026-10-15, so nothing is done twice and each repo tells the same story:

- **Cozy Core** (cozy-core): the builder rules, each an ordinary game rule: builds stay built (mob
  explosions, endermen, fire), no phantoms, keep your hotbar on death, the Hearth Lantern, cheap
  wood in the stonecutter, re-dyeing and builder trades. Fabric and NeoForge jars, plus a plain
  data pack of the recipes.
- **Espresso Flavoured Core** (espresso-flavoured): the café: coffee you grow and brew, mugs,
  baristas, Bean Coins, the café charms, and Drowsy (coffee's reason to exist). Longer days stay a
  Core setting for now. The survival rules Cozy Core now covers left Core in 0.4.0 (its PLAN-0.4.0.md).
- **Spawn Gazebo** (spawn-gazebo): the gazebo at the world spawn and its starter chest.
- **Nightfall Clock** (nightfall-clock): the clock above the hotbar and the dusk warning.
- **Pocket List** (pocket-list): the gathering checklist.
- **Not Now, Shield** (not-now-shield): the shield drawn lower in first person; on 26.x also
  tools that change blocks while a shield is in the off-hand.
- **Beyond the Dragon** (beyond-the-dragon): the end-game after the Ender Dragon: ruins at the
  edge of the End, a dragon that comes back harder, and the aphelite metal.
- **Forgemark** (forgemark): tempering the tools, weapons and armour you already have, from any
  mod, for a small capped quality bonus.
- **Espresso Flavoured** (espresso-flavoured-pack): the modpack that puts them together. Since
  pack 0.5.0 (2026-10-15) it carries all five: Cozy Core, Espresso Flavoured Core (back as the café
  once it stopped doing Cozy Core's jobs), Spawn Gazebo, Nightfall Clock and Pocket List.
- **Cozy Core Pack** (cozy-core-pack): the builder-friendly modpack around Cozy Core, with Spawn
  Gazebo, Nightfall Clock, Pocket List and Not Now, Shield.

A new feature goes in the mod whose job it is. If none fits, ask Gabby before adding it anywhere.

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
- On Linux without a screen, run game tests under `xvfb-run -a`.

**Writing.**
- README and CHANGELOG are for players: plain, friendly, no jargon. New player-facing changes
  go in the CHANGELOG's newest section.
- Keep the credit line: "made by Gabby with help from Claude, an AI assistant".
- Gabby publishes on Modrinth herself. Don't upload anything there.

**Minecraft.** Minecraft 1.21.1 (official Mojang names; Loom 1.18.4, Java 21). Check the real
jar (`javap`) rather than older docs when a name looks different from other versions.
