package com.gabbymaybelle.shieldstrip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Films the YouTube Short with {@link Film}: a screenshot every tick, in a tall 1080x1920 window.
 * Only runs with `./gradlew :fabric:runClientGameTest -PvideoTour` (on 26.3, where hoes and
 * shovels work too). Frames go in build/run/clientGameTest/video_tour.
 *
 * <p>Every scene is in survival, with a shield in the off-hand. The tool scenes are filmed in third
 * person, since a tall window leaves the main hand off screen in first person: an axe strips logs
 * and scrapes copper, a hoe tills and a shovel makes a path. Then, in first person, the view turns
 * across the scene with the shield resting low, and raises it once. Each scene places its own blocks, so any one can be
 * filmed alone. With {@code -PvideoWorld=<zip>} the set sits beside a saved world's kept
 * "cottage" spot; without it, in a plain spot near spawn. {@code -PvideoSkin=<png>} dresses the
 * player in that (slim-armed) skin, through a resource pack made only for this run.
 */
public class VideoTour implements FabricClientGameTest {
	/** Names the system properties: shieldstrip.videoTour, shieldstrip.videoScenes... */
	private static final String PREFIX = "shieldstrip";

	private Film film;
	/** Where the set is: (x, z) of a spot facing north, y the ground the player stands on. */
	private int sx, sy, sz;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Film.on(PREFIX)) {
			return;
		}
		film = new Film(context, PREFIX, "video_tour");
		skin(context, film.prop("videoSkin", ""));
		film.take(() -> film.newWorld("not now shield"), this::stage, this::scenes);
	}

	/**
	 * Puts the skin in place of every slim default skin (the test player gets a slim one) with a
	 * resource pack in the game folder, and turns the pack on.
	 */
	private static void skin(ClientGameTestContext ctx, String png) {
		if (png.isEmpty()) {
			return;
		}
		Path pack = FabricLoader.getInstance().getGameDir().resolve("resourcepacks/film-skin");
		try {
			Path slim = pack.resolve("assets/minecraft/textures/entity/player/slim");
			Files.createDirectories(slim);
			Files.writeString(pack.resolve("pack.mcmeta"),
					"{\"pack\": {\"description\": \"Filming skin\", \"min_format\": 97, \"max_format\": 97}}");
			for (String name : List.of("alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri")) {
				Files.copy(Path.of(png), slim.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new AssertionError("couldn't make the skin pack", e);
		}
		var reload = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
		ctx.runOnClient(mc -> {
			var repo = mc.getResourcePackRepository();
			repo.reload();
			List<String> on = new ArrayList<>(repo.getSelectedIds());
			on.add("file/film-skin");
			repo.setSelected(on);
			reload.set(mc.reloadResourcePacks());
		});
		ctx.waitFor(mc -> reload.get().isDone(), 1200);
		ctx.waitTicks(5);
		Film.check(ctx.computeOnClient(mc -> mc.getResourcePackRepository().getSelectedIds().contains("file/film-skin")), "the skin pack is on");
	}

	// ------------------------------------------------------------------ the set

	/** A new world (no saved one given): the set goes a little way from spawn, on its ground. */
	private void stage() {
		var spawn = film.server.computeOnServer(s -> s.overworld().getRespawnData().pos());
		int x = spawn.getX() + 20, z = spawn.getZ();
		film.keep("cottage", x + " " + film.ground(x, z) + " " + z);
	}

	private void readSet() {
		String[] s = film.kept("cottage").split(" ");
		sx = Integer.parseInt(s[0]);
		sy = Integer.parseInt(s[1]);
		sz = Integer.parseInt(s[2]);
	}

	/** A block {@code east} blocks east, {@code up} up and {@code south} blocks south of the spot. */
	private BlockPos at(int east, int up, int south) {
		return new BlockPos(sx + east, sy + up, sz + south);
	}

	private Vec3 feet(double east, double south) {
		return new Vec3(sx + east, sy, sz + south);
	}

	private void cmd(String format, Object... args) {
		film.server.runCommand(String.format(java.util.Locale.ROOT, format, args));
	}

	private void fill(BlockPos a, BlockPos b, String block) {
		cmd("fill %d %d %d %d %d %d %s", a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block);
	}

	private void set(BlockPos p, String block) {
		cmd("setblock %d %d %d %s", p.getX(), p.getY(), p.getZ(), block);
	}

	/**
	 * The woodpile south of the cottage's path (three oak logs lying east-west, two high) with a
	 * weathered copper block at its east end, and a strip of fresh grass to its west for the hoe
	 * and shovel. Clears the air round both, so no flowers or tall grass take the click.
	 */
	private void buildSet() {
		fill(at(-6, 0, 1), at(8, 4, 11), "air");
		fill(at(-6, -1, 1), at(8, -1, 11), "grass_block");
		fill(at(3, 0, 2), at(5, 1, 2), "oak_log[axis=x]");
		set(at(6, 0, 2), "weathered_copper");
		fill(at(-4, -1, 3), at(-2, -1, 3), "grass_block");
	}

	/** Axe, hoe, shovel and an empty slot on the hotbar, a shield in the off-hand, the axe selected. */
	private void kit() {
		cmd("clear @a");
		cmd("item replace entity @a hotbar.0 with iron_axe");
		cmd("item replace entity @a hotbar.1 with iron_hoe");
		cmd("item replace entity @a hotbar.2 with iron_shovel");
		cmd("item replace entity @a weapon.offhand with shield");
		slot(0);
		// The new item's name over the hotbar fades before filming starts.
		film.ctx.waitTicks(45);
	}

	private void slot(int i) {
		film.ctx.getInput().pressKey(o -> o.keyHotbarSlots[i]);
	}

	/**
	 * Survival, HUD on, the set fresh, the kit in hand, standing at {@code feet} looking at
	 * {@code look}; seen from behind the player ({@code third}) or through their eyes.
	 */
	private void ready(Vec3 feet, Vec3 look, boolean third) {
		film.hud(true);
		film.survival();
		film.ctx.runOnClient(mc -> mc.options.setCameraType(third ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON));
		// A little further back than the usual 4 blocks, so the block being clicked shows past the head.
		cmd("attribute @a minecraft:camera_distance base set %s", third ? "6" : "4");
		if (third && !film.prop("videoSkin", "").isEmpty()) {
			Film.check(film.ctx.computeOnClient(mc -> mc.player.getSkin().model() == PlayerModelType.SLIM), "the player has a slim skin, so the filming skin shows");
		}
		buildSet();
		film.camera(feet, look);
		kit();
		film.turnHead(look);
		film.ctx.runOnClient(mc -> mc.player.setYBodyRot(mc.player.getYRot()));
		film.ctx.waitTicks(2);
	}

	/**
	 * A point on a block's south face (the side the player looks at), or its top, near its west
	 * edge: in third person the camera looks past the head at the crosshair, so aiming near the
	 * edge leaves most of the block in view.
	 */
	private static Vec3 south(BlockPos p) {
		return new Vec3(p.getX() + 0.12, p.getY() + 0.5, p.getZ() + 0.98);
	}

	private static Vec3 top(BlockPos p) {
		return new Vec3(p.getX() + 0.12, p.getY() + 0.98, p.getZ() + 0.5);
	}

	// ------------------------------------------------------------------ moves

	private Vec3 looking;

	/** Turns the head from where it looks now to {@code to} over {@code n} frames, eased. */
	private void sweep(Vec3 to, int n) {
		Vec3 from = looking == null ? to : looking;
		for (int i = 1; i <= n; i++) {
			film.turnHead(from.lerp(to, Film.ease(i / (double) n)));
			// In third person the body turns with the head, not a step behind it.
			film.ctx.runOnClient(mc -> mc.player.setYBodyRot(mc.player.getYRot()));
			film.record(1);
		}
		looking = to;
	}

	/** Right-clicks the block under the crosshair (it must be {@code block}), then films the result. */
	private void use(BlockPos block, String what, int after) {
		String on = film.ctx.computeOnClient(VideoTour::crosshair);
		Film.check(on.equals(block.toShortString()), what + ": the crosshair is on " + block.toShortString() + " (it is on " + on + ")");
		film.ctx.getInput().pressKey(o -> o.keyUse);
		film.log(what, block.toShortString());
		film.record(after);
	}

	private static String crosshair(Minecraft mc) {
		return mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
				? hit.getBlockPos().toShortString() : "nothing";
	}

	// ------------------------------------------------------------------ the scenes

	private void scenes() {
		readSet();
		film.window(1080, 1920, 3);
		film.scene("s_strip", this::strip);
		film.scene("s_more", this::more);
		film.scene("s_garden", this::garden);
		film.scene("s_view", this::view);
		film.scene("s_loop", this::loop);
		if (film.scouting()) {
			// A scout still is taken before anything happens: these show the results too.
			film.scene("x_stripped", () -> {
				strip0();
				use0(at(4, 1, 2));
				film.record(1);
			});
			film.scene("x_tilled", () -> {
				garden0();
				use0(at(-4, -1, 3));
				film.record(1);
			});
		}
	}

	/** Where the strip scenes stand: 2.5 blocks south of the woodpile, facing it, the cottage beyond. */
	private Vec3 stripStand() {
		return feet(4.5, 5.5);
	}

	/** Frame 1 is the payoff: the axe already swinging at the first log, the shield up beside it. */
	private void strip() {
		BlockPos log = at(4, 1, 2);
		Vec3 start = south(log).add(-0.6, 0.15, 0);
		ready(stripStand(), start, true);
		looking = start;
		sweep(south(log), 3);
		use(log, "strip", 40);
	}

	/** Set up as {@link #strip}, aimed at the first log, nothing filmed. */
	private void strip0() {
		BlockPos log = at(4, 1, 2);
		ready(stripStand(), south(log), true);
	}

	/** Set up as {@link #garden}, the hoe out and aimed at the first grass block, nothing filmed. */
	private void garden0() {
		BlockPos a = at(-4, -1, 3);
		ready(feet(-2.5, 5.3), top(a), true);
		slot(1);
		film.ctx.waitTicks(10);
	}

	/** Right-clicks off camera and waits for the result to show. */
	private void use0(BlockPos block) {
		String on = film.ctx.computeOnClient(VideoTour::crosshair);
		Film.check(on.equals(block.toShortString()), "the crosshair is on " + block.toShortString() + " (it is on " + on + ")");
		film.ctx.getInput().pressKey(o -> o.keyUse);
		film.ctx.waitTicks(4);
	}

	/** Hard cut: two more logs, then the copper scraped twice, back to bright copper. */
	private void more() {
		BlockPos low = at(4, 0, 2), east = at(5, 1, 2), copper = at(6, 0, 2);
		ready(stripStand(), south(east).add(-0.4, 0.1, 0), true);
		set(at(4, 1, 2), "stripped_oak_log[axis=x]");
		looking = south(east).add(-0.4, 0.1, 0);
		sweep(south(east), 3);
		use(east, "strip", 8);
		sweep(south(low), 5);
		use(low, "strip", 8);
		sweep(south(copper), 6);
		use(copper, "scrape", 7);
		use(copper, "scrape", 18);
	}

	/** The grass strip: the hoe tills two blocks, then the shovel makes a path of the third. */
	private void garden() {
		BlockPos a = at(-4, -1, 3), b = at(-3, -1, 3), c = at(-2, -1, 3);
		Vec3 stand = feet(-2.5, 5.3);
		ready(stand, top(a).add(-0.3, 0, 0.2), true);
		slot(1);
		film.ctx.waitTicks(45);
		looking = top(a).add(-0.3, 0, 0.2);
		sweep(top(a), 3);
		use(a, "till", 6);
		sweep(top(b), 4);
		use(b, "till", 8);
		slot(2);
		film.log("slot", "shovel");
		film.record(4);
		sweep(top(c), 4);
		use(c, "path", 16);
	}

	/**
	 * The view turns slowly from the woodpile across the cottage with the shield resting low, then
	 * an empty main hand raises the shield once and lowers it.
	 */
	private void view() {
		Vec3 stand = feet(1.5, 7.5);
		Vec3 pile = south(at(5, 1, 2));
		Vec3 house = new Vec3(sx + 0.5, sy + 3, sz - 9);
		ready(stand, pile, false);
		slot(3);
		film.ctx.waitTicks(5);
		looking = pile;
		sweep(house, 70);
		film.record(6);
		film.ctx.getInput().holdKey(o -> o.keyUse);
		film.log("shield_up", "");
		film.record(22);
		film.ctx.getInput().releaseKey(o -> o.keyUse);
		film.log("shield_down", "");
		film.record(10);
	}

	/** Back at the first shot's angle, the logs fresh again, so the Short loops into its first frame. */
	private void loop() {
		BlockPos log = at(4, 1, 2);
		Vec3 start = south(log).add(-0.6, 0.15, 0);
		ready(stripStand(), south(at(3, 1, 2)), true);
		looking = south(at(3, 1, 2));
		sweep(start, 16);
		film.record(4);
	}
}
