package com.gabbymaybelle.shieldstrip;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The filming kit: camera moves, frame logging, scene filters, scout passes and saved worlds for a
 * client game test that films a mod's video. A screenshot every tick; `actions.tsv` (what happened
 * at which frame, for the sounds) and `frames.tsv` (the cursor, the tick and any problems seen on
 * that frame) go beside the frames, written even when a scene fails.
 *
 * <p>Gradle passes these as system properties named {@code <prefix>.<name>}:
 * <ul>
 * <li>{@code videoTour}: film at all (without it the test returns at once).
 * <li>{@code videoScenes=a,b,w_*}: film only these scenes (a trailing * matches a prefix).
 * <li>{@code videoScout}: one still per scene, at its first camera position, then on to the next.
 * <li>{@code videoSeed=<seed>}: the seed for a new world.
 * <li>{@code videoWorld=<zip or folder>}: film in this saved world instead of making one.
 * <li>{@code videoStage[=<zip>]}: after staging a new world, save it (by default as
 * {@code build/video_world.zip}, outside the run folder, which each game test clears).
 * </ul>
 */
public final class Film {
	/** Thrown by {@link #record} in a scout pass once a scene's first frame is saved. */
	public static final class Cut extends RuntimeException {
		Cut() {
			super(null, null, false, false);
		}
	}

	/** A spot and the way it faces (a yaw), so shots can be given as "out, to the side, up". */
	public record Spot(double x, double y, double z, float yaw) {
		/** {@code out} blocks the way the spot faces, {@code side} to its right, {@code up} above it. */
		public Vec3 at(double out, double side, double up) {
			double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
			return new Vec3(x + fx * out - fz * side, y + up, z + fz * out + fx * side);
		}
	}

	/** Client ticks so far, logged with each frame so a dropped frame shows up. */
	private static final AtomicInteger TICKS = new AtomicInteger();
	/** The tick of the last chat or action-bar line, so a frame with chat on it is flagged. */
	private static volatile int lastChat = -10_000;
	private static volatile int lastOverlay = -10_000;
	private static boolean hooked;

	public final ClientGameTestContext ctx;
	public TestSingleplayerContext world;
	public TestServerContext server;
	public final Path dir;
	/** Small, uneven pauses (typing, the cursor), the same on every run. */
	public final Random jitter = new Random(31);
	/** Ticks to wait before a scene's first frame: the camera arrives, chunks draw, chat clears. */
	public int settle = 10;

	private final String prefix;
	private final boolean scout;
	private final List<String> only;
	private final List<String> actions = new ArrayList<>();
	private final List<String> frames = new ArrayList<>();
	private final List<String> failures = new ArrayList<>();
	private final Map<String, String> kept = new LinkedHashMap<>();
	private String scene;
	private int frame;
	/** Where the camera is meant to look, for the "something in front" check on a scene's first frame. */
	private Vec3 target;
	/** True while {@link #type} has chat open on purpose, so it isn't flagged. */
	private boolean typing;

	public static boolean on(String prefix) {
		return Boolean.getBoolean(prefix + ".videoTour");
	}

	/** {@code prefix} names the system properties ({@code <prefix>.videoScenes}), {@code folder} the frames' folder in the game folder. */
	public Film(ClientGameTestContext context, String prefix, String folder) {
		ctx = context;
		this.prefix = prefix;
		scout = Boolean.getBoolean(prefix + ".videoScout");
		only = List.of(prop("videoScenes", "").split(",")).stream().map(String::trim).filter(n -> !n.isEmpty()).toList();
		dir = FabricLoader.getInstance().getGameDir().resolve(folder);
		if (!hooked) {
			hooked = true;
			ClientTickEvents.END_CLIENT_TICK.register(mc -> TICKS.incrementAndGet());
			ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
				if (overlay) {
					lastOverlay = TICKS.get();
				} else {
					lastChat = TICKS.get();
				}
			});
			ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, at) -> lastChat = TICKS.get());
		}
	}

	public String prop(String name, String fallback) {
		return System.getProperty(prefix + "." + name, fallback);
	}

	public boolean scouting() {
		return scout;
	}

	// ------------------------------------------------------------------ the take

	/**
	 * Films a take: opens the world ({@code videoWorld} if given, otherwise {@code make} and then
	 * {@code stage}), sets the standard set, runs {@code scenes}, and writes the logs whatever
	 * happens. Fails at the end if any scene failed, after filming every other scene.
	 */
	public void take(Supplier<TestSingleplayerContext> make, Runnable stage, Runnable scenes) {
		clearFolder();
		try {
			try {
				open(make, stage);
				standardSet();
			} catch (AssertionError | RuntimeException e) {
				// Before any scene: note why, so the contact sheet can say.
				note("fail", String.valueOf(e.getMessage()).replace('\t', ' ').replace('\n', ' '));
				throw e;
			}
			scenes.run();
		} finally {
			try {
				Files.write(dir.resolve("actions.tsv"), actions);
				Files.write(dir.resolve("frames.tsv"), frames);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			if (world != null) {
				world.close();
			}
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Failed scenes (the others were filmed): " + String.join("; ", failures));
		}
	}

	/** A new survival world with commands, peaceful, from this seed ({@code videoSeed} wins when given). */
	public TestSingleplayerContext newWorld(String seed) {
		String chosen = prop("videoSeed", seed);
		note("seed", chosen);
		return ctx.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed(chosen);
			state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			state.setDifficulty(Difficulty.PEACEFUL);
			state.setAllowCommands(true);
		}).create();
	}

	private void open(Supplier<TestSingleplayerContext> make, Runnable stage) {
		String saved = prop("videoWorld", "");
		if (!saved.isEmpty()) {
			world = openSaved(Path.of(saved));
			server = world.getServer();
			readKept();
			note("world", saved);
			return;
		}
		world = make.get();
		server = world.getServer();
		stage.run();
		String stageTo = prop("videoStage", "");
		if (!stageTo.isEmpty() && !stageTo.equals("false")) {
			saveStaged(stageTo.equals("true") ? FabricLoader.getInstance().getGameDir().getParent().getParent().resolve("video_world.zip") : Path.of(stageTo));
		}
	}

	/** Keeps a value the scenes need (a spot found while staging) with the saved world. */
	public void keep(String key, String value) {
		kept.put(key, value);
		note("keep", key + " = " + value);
	}

	public String kept(String key) {
		String value = kept.get(key);
		check(value != null, "the staged world kept \"" + key + "\" (stage it again?)");
		return value;
	}

	/**
	 * Saves the staged world as a zip (with the kept values in {@code video_set.txt}), then reopens
	 * it and carries on. Not in the game folder: the next game test clears it.
	 */
	private void saveStaged(Path zip) {
		TestWorldSave save = world.getWorldSave();
		Path saveDir = save.getSaveDirectory();
		server.runCommand("save-all flush");
		ctx.waitTicks(10);
		world.close();
		try {
			writeKept(saveDir);
			Files.createDirectories(zip.toAbsolutePath().getParent());
			try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip)); var walk = Files.walk(saveDir)) {
				for (Path p : walk.filter(Files::isRegularFile).filter(p -> !p.getFileName().toString().equals("session.lock")).toList()) {
					out.putNextEntry(new ZipEntry(saveDir.relativize(p).toString().replace('\\', '/')));
					Files.copy(p, out);
					out.closeEntry();
				}
			}
		} catch (IOException e) {
			throw new AssertionError("couldn't save the staged world", e);
		}
		note("staged", zip.toString());
		world = save.open();
		server = world.getServer();
	}

	/**
	 * Opens a saved world: makes a throwaway world for its folder, puts the saved world's files in
	 * that folder, and opens it. The save must come from the same Minecraft version.
	 */
	private TestSingleplayerContext openSaved(Path from) {
		check(Files.exists(from), "the saved world " + from + " exists");
		TestSingleplayerContext empty = ctx.worldBuilder().create();
		TestWorldSave save = empty.getWorldSave();
		empty.close();
		Path to = save.getSaveDirectory();
		try {
			deleteTree(to);
			Files.createDirectories(to);
			if (Files.isDirectory(from)) {
				Path root = Files.exists(from.resolve("level.dat")) ? from : onlyChild(from);
				try (var walk = Files.walk(root)) {
					for (Path p : walk.filter(Files::isRegularFile).toList()) {
						Path dest = to.resolve(root.relativize(p).toString());
						Files.createDirectories(dest.getParent());
						Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			} else {
				unzip(from, to);
			}
		} catch (IOException e) {
			throw new AssertionError("couldn't copy the saved world " + from, e);
		}
		check(Files.exists(to.resolve("level.dat")), "the saved world " + from + " has a level.dat");
		return save.open();
	}

	/** Unzips a world, dropping one top folder if the zip has its world inside one. */
	private static void unzip(Path zip, Path to) throws IOException {
		String strip = "";
		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
			boolean atRoot = false;
			String top = null;
			for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
				String name = e.getName();
				if (name.equals("level.dat")) {
					atRoot = true;
				} else if (name.endsWith("/level.dat") && name.indexOf('/') == name.length() - "/level.dat".length()) {
					top = name.substring(0, name.indexOf('/') + 1);
				}
			}
			if (!atRoot && top != null) {
				strip = top;
			}
		}
		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
			for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
				if (e.isDirectory() || !e.getName().startsWith(strip) || e.getName().endsWith("session.lock")) {
					continue;
				}
				Path dest = to.resolve(e.getName().substring(strip.length())).normalize();
				if (!dest.startsWith(to)) {
					throw new IOException("a zip entry points outside the world: " + e.getName());
				}
				Files.createDirectories(dest.getParent());
				try (OutputStream out = Files.newOutputStream(dest)) {
					in.transferTo(out);
				}
			}
		}
	}

	private static Path onlyChild(Path folder) throws IOException {
		try (var list = Files.list(folder)) {
			List<Path> worlds = list.filter(p -> Files.exists(p.resolve("level.dat"))).toList();
			if (worlds.size() != 1) {
				throw new IOException(folder + " has no level.dat (nor one world folder holding it)");
			}
			return worlds.getFirst();
		}
	}

	private void writeKept(Path saveDir) throws IOException {
		List<String> lines = new ArrayList<>();
		kept.forEach((k, v) -> lines.add(k + "\t" + v));
		Files.write(saveDir.resolve("video_set.txt"), lines);
	}

	private void readKept() {
		Path file = world.getWorldSave().getSaveDirectory().resolve("video_set.txt");
		if (!Files.exists(file)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(file)) {
				int tab = line.indexOf('\t');
				if (tab > 0) {
					kept.put(line.substring(0, tab), line.substring(tab + 1));
				}
			}
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * The rules every take needs, each from a refilm: no command replies or game-rule lines in chat;
	 * the clock, weather and mobs held still; every advancement granted so none pops up as a toast,
	 * then the experience they gave taken away so no level shows over the hotbar; half tick rate, so
	 * one saved frame is one tick and 20 frames a second plays at the game's real pace.
	 */
	public void standardSet() {
		rule("send_command_feedback", "false");
		rule("advance_time", "false");
		rule("advance_weather", "false");
		rule("spawn_mobs", "false");
		rule("show_advancement_messages", "false");
		server.runCommand("advancement grant @a everything");
		server.runCommand("xp set @a 0 levels");
		server.runCommand("xp set @a 0 points");
		server.runCommand("weather clear");
		server.runCommand("time set 6000");
		server.runCommand("tick rate 10");
		// The game-rule lines reach chat a few ticks after the command: clear it after they arrive.
		ctx.waitTicks(5);
		quiet();
	}

	// ------------------------------------------------------------------ scenes

	/** Whether {@code videoScenes} lets this scene be filmed. */
	public boolean want(String name) {
		return only.isEmpty() || only.stream().anyMatch(o -> o.endsWith("*") ? name.startsWith(o.substring(0, o.length() - 1)) : o.equals(name));
	}

	/**
	 * Films one scene, if {@code videoScenes} wants it. Write each scene so it sets up all it needs
	 * at its start (game mode, HUD, time, items, where the player stands): then any scene can be
	 * filmed alone, and a failed scene doesn't spoil the next. A failure is logged with a still
	 * ({@code fail_<scene>.png}) and the take goes on to the next scene.
	 */
	public void scene(String name, Runnable body) {
		if (!want(name)) {
			return;
		}
		scene = name;
		frame = 0;
		target = null;
		try {
			body.run();
			log("end", "");
		} catch (Cut cut) {
			typing = false;
			log("scout", "");
			// Cut short, perhaps with a chest or chat open: tidy up as a failed scene does.
			recover();
		} catch (AssertionError | RuntimeException e) {
			String why = String.valueOf(e.getMessage());
			failures.add(name + ": " + why);
			log("fail", why.replace('\t', ' ').replace('\n', ' '));
			System.err.println("[film] scene " + name + " failed at frame " + frame + ": " + why);
			try {
				ctx.takeScreenshot(TestScreenshotOptions.of("fail_" + name).disableCounterPrefix().withDestinationDir(dir));
			} catch (RuntimeException ignored) {
				// The still is a bonus.
			}
			recover();
		} finally {
			ctx.getInput().releaseKey(o -> o.keyAttack);
			ctx.getInput().releaseKey(o -> o.keyUse);
			scene = null;
		}
	}

	/** After a failed scene: no screen open, the GUI back to its normal scale. */
	private void recover() {
		try {
			ctx.setScreen(() -> null);
			ctx.runOnClient(Minecraft::resizeGui);
		} catch (RuntimeException ignored) {
			// The next scene sets itself up anyway.
		}
	}

	// ------------------------------------------------------------------ filming

	public void record(int count) {
		for (int i = 0; i < count; i++) {
			shoot();
			ctx.waitTick();
		}
	}

	/** Films until {@code done} (up to twenty seconds of frames). */
	public void until(Predicate<Minecraft> done) {
		for (int i = 0; i < 200; i++) {
			if (ctx.computeOnClient(done::test)) {
				return;
			}
			shoot();
			ctx.waitTick();
		}
		throw new AssertionError(scene + " waited twenty seconds at frame " + frame);
	}

	/** Waits off camera, before a scene's first frame. */
	public void until0(Predicate<Minecraft> done) {
		ctx.waitFor(done, 400);
	}

	private void shoot() {
		check(scene != null, "record() is called inside film.scene(...)");
		if (frame == 0) {
			// The camera arrives, chunks draw, and chat from the set-up clears before the first frame.
			ctx.waitTicks(settle);
			chunks();
			quiet();
		}
		String flags = ctx.computeOnClient(mc -> flags(mc, frame == 0));
		ctx.takeScreenshot(TestScreenshotOptions.of(String.format(Locale.ROOT, "%s_%04d", scene, frame)).disableCounterPrefix().withDestinationDir(dir));
		String cursor = ctx.computeOnClient(mc -> screen(mc) == null ? "-\t-" : (int) mc.mouseHandler.xpos() + "\t" + (int) mc.mouseHandler.ypos());
		frames.add(scene + "\t" + frame + "\t" + cursor + "\t" + TICKS.get() + "\t" + flags);
		frame++;
		if (scout) {
			throw new Cut();
		}
	}

	/**
	 * What's wrong on this frame, as the game sees it: comma-separated, or "-". The first frame of
	 * a scene also checks the camera's spot and its view.
	 */
	private String flags(Minecraft mc, boolean first) {
		List<String> found = new ArrayList<>();
		int now = TICKS.get();
		boolean hud = !hudHidden(mc);
		Screen open = screen(mc);
		if (mc.player.isDeadOrDying() || open instanceof DeathScreen) {
			found.add("dead");
		}
		if (open instanceof ChatScreen) {
			if (!typing) {
				found.add("chat open");
			}
		} else if (hud && now - lastChat < 200) {
			found.add("chat line");
		}
		if (hud && now - lastOverlay < 60) {
			found.add("action bar text");
		}
		if (hud && !mc.player.isSpectator() && !mc.player.isCreative() && mc.player.experienceLevel > 0) {
			found.add("xp level " + mc.player.experienceLevel);
		}
		if (first) {
			Vec3 eye = mc.player.getEyePosition();
			BlockPos at = BlockPos.containing(eye);
			var state = mc.level.getBlockState(at);
			if (!state.getCollisionShape(mc.level, at).isEmpty()) {
				found.add("camera inside " + BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath());
			}
			if (target != null) {
				double whole = eye.distanceTo(target);
				BlockHitResult hit = mc.level.clip(new ClipContext(eye, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
				if (hit.getType() == HitResult.Type.BLOCK) {
					double d = eye.distanceTo(hit.getLocation());
					if (d < whole * 0.6 && hit.getLocation().distanceTo(target) > 4) {
						String block = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(hit.getBlockPos()).getBlock()).getPath();
						found.add(String.format(Locale.ROOT, "%s in front (%.0f of %.0f blocks)", block, d, whole));
					}
				}
			}
		}
		return found.isEmpty() ? "-" : String.join(",", found);
	}

	/** What happened at this frame, for the sound pass: {@code log("chest_open", "")}. */
	public void log(String kind, String what) {
		actions.add(scene + "\t" + frame + "\t" + kind + "\t" + what);
	}

	/** A note about the set itself (seed, where things were built), before any scene. */
	public void note(String kind, String what) {
		System.out.println("[film] " + kind + " " + what);
		actions.add("set\t0\t" + kind + "\t" + what);
	}

	/** Clears chat and toasts. Every scene's first frame does this itself. */
	public void quiet() {
		ctx.runOnClient(mc -> {
			clearToasts(mc);
			chat(mc).clearMessages(false);
		});
		lastChat = -10_000;
		lastOverlay = -10_000;
	}

	/** Waits for the world to draw; a slow draw can take longer than the wait allows. */
	public void chunks() {
		try {
			world.getConnection().waitForChunksRender();
		} catch (AssertionError e) {
			System.err.println("[film] went on before every chunk had drawn");
		}
	}

	/** Sizes the window and the GUI, and checks both took (a screen smaller than the window can shrink it). */
	public void window(int width, int height, int scale) {
		ctx.getInput().resizeWindow(width, height);
		ctx.waitTicks(3);
		ctx.runOnClient(mc -> {
			mc.options.guiScale().set(scale);
			mc.resizeGui();
		});
		ctx.waitTicks(2);
		String got = ctx.computeOnClient(mc -> mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
		check(got.equals(width + "x" + height), "the window is " + width + "x" + height + " (it is " + got + ")");
	}

	/**
	 * A bigger GUI than the options allow, for a container screen that must read on a phone: the
	 * game caps a 1080-wide window at scale 3, but the window takes 5 (4 when it's 1920 wide).
	 * {@link #normalGui} puts it back.
	 */
	public void bigGui() {
		ctx.runOnClient(mc -> mc.getWindow().setGuiScale(mc.getWindow().getWidth() < 1500 ? 5 : 4));
	}

	public void normalGui() {
		ctx.runOnClient(Minecraft::resizeGui);
	}

	// ------------------------------------------------------------------ the camera

	/** Stands the camera at {@code eye}, looking at {@code look} (a teleport; right for spectators). */
	public void camera(Vec3 eye, Vec3 look) {
		target = look;
		server.runCommand(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f facing %.3f %.3f %.3f", eye.x, eye.y, eye.z, look.x, look.y, look.z));
	}

	/**
	 * Turns the player's head from the client. A teleport's facing doesn't always reach a survival
	 * player's pitch, so for survival shots call this after the teleport, and each frame while the
	 * head moves.
	 */
	public void turnHead(Vec3 look) {
		target = look;
		ctx.runOnClient(mc -> {
			Vec3 eye = mc.player.getEyePosition();
			double dx = look.x - eye.x, dy = look.y - eye.y, dz = look.z - eye.z;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
			mc.player.setYRot(yaw);
			mc.player.setXRot(pitch);
			mc.player.setYHeadRot(yaw);
		});
	}

	/** Aims a survival player at a block from the client and checks the crosshair is on it. */
	public void aim(BlockPos block) {
		target = Vec3.atCenterOf(block);
		ctx.getInput().lookAt(block);
		ctx.waitTicks(3);
		String on = ctx.computeOnClient(mc -> mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
				? hit.getBlockPos().toShortString() : "nothing");
		check(on.equals(block.toShortString()), "the player aims at " + block.toShortString() + " (the crosshair is on " + on + ")");
	}

	/** Eases the camera from one shot to another over {@code n} frames. */
	public void dolly(int n, Vec3 eyeFrom, Vec3 eyeTo, Vec3 lookFrom, Vec3 lookTo) {
		for (int i = 0; i < n; i++) {
			double t = ease(i / Math.max(1.0, n - 1.0));
			camera(eyeFrom.lerp(eyeTo, t), lookFrom.lerp(lookTo, t));
			record(1);
		}
	}

	/** Circles {@code centre} from {@code fromDeg} to {@code toDeg} (0 = the spot's facing), looking at {@code look}. */
	public void orbit(int n, Spot centre, double fromDeg, double toDeg, double distance, double up, Vec3 look) {
		for (int i = 0; i < n; i++) {
			double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * ease(i / Math.max(1.0, n - 1.0)));
			camera(centre.at(Math.cos(a) * distance, Math.sin(a) * distance, up), look);
			record(1);
		}
	}

	public static double ease(double t) {
		return t * t * (3 - 2 * t);
	}

	/**
	 * The ground at a column, its chunk generated first: an unloaded chunk reads as the bottom of
	 * the world. Leaves don't count.
	 */
	public int ground(int x, int z) {
		return server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			level.getChunk(x >> 4, z >> 4);
			return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		});
	}

	/**
	 * The highest leaf or block within {@code radius} of a column (chunks generated first). A camera
	 * a few blocks over this sees past the trees: Spawn Gazebo's shots lost whole takes to a tree
	 * between the camera and the subject.
	 */
	public int treeline(int x, int z, int radius) {
		return server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			int top = level.getMinY();
			for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
				for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
					level.getChunk(cx, cz);
				}
			}
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					top = Math.max(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz));
				}
			}
			return top;
		});
	}

	// ------------------------------------------------------------------ the player

	public void spectator() {
		server.runCommand("gamemode spectator @a");
	}

	public void survival() {
		server.runCommand("gamemode survival @a");
	}

	public void hud(boolean show) {
		ctx.runOnClient(mc -> setHud(mc, show));
	}

	/**
	 * Gives an item (a command such as {@code "item replace entity @a weapon.mainhand with iron_pickaxe"})
	 * and waits off camera until its name over the hotbar has faded.
	 */
	public void give(String command) {
		server.runCommand(command);
		ctx.waitTicks(45);
	}

	/** Moves the cursor across an open screen, as fractions of the window, over a few frames. */
	public void glide(double[] from, double[] to, int steps) {
		for (int i = 1; i <= steps; i++) {
			double t = ease(i / (double) steps);
			double fx = from[0] + (to[0] - from[0]) * t, fy = from[1] + (to[1] - from[1]) * t;
			int[] size = ctx.computeOnClient(mc -> new int[] { mc.getWindow().getWidth(), mc.getWindow().getHeight() });
			ctx.getInput().setCursorPos(fx * size[0], fy * size[1]);
			record(1);
		}
	}

	/**
	 * Opens chat and types a command a key at a time with uneven pauses, then sends it. Command
	 * replies stay off, so chat shows only what the player typed; an error still shows (in red),
	 * and the "chat line" check flags it.
	 */
	public void type(String command) {
		typing = true;
		ctx.getInput().pressKey(o -> o.keyChat);
		record(6);
		for (char c : command.toCharArray()) {
			ctx.getInput().typeChar(c);
			log("key", String.valueOf(c));
			record(1 + (jitter.nextInt(5) == 0 ? 2 + jitter.nextInt(3) : jitter.nextInt(2)));
		}
		record(10);
		ctx.getInput().pressKey(InputConstants.KEY_RETURN);
		typing = false;
		log("enter", command);
		record(2);
	}

	// ------------------------------------------------------------------ the version seam
	// The only calls here that change between Minecraft versions (these are 26.x's). Porting the
	// kit to another version means fixing this block; the README lists the older names.

	private static Screen screen(Minecraft mc) {
		return mc.gui.screen();
	}

	private static boolean hudHidden(Minecraft mc) {
		return mc.gui.hud.isHidden();
	}

	private static void setHud(Minecraft mc, boolean show) {
		if (mc.gui.hud.isHidden() == show) {
			mc.gui.hud.toggle();
		}
	}

	private static void clearToasts(Minecraft mc) {
		mc.gui.toastManager().clear();
	}

	private static net.minecraft.client.gui.components.ChatComponent chat(Minecraft mc) {
		return mc.gui.hud.getChat();
	}

	/** Game rules are snake_case in 26.x and camelCase (sendCommandFeedback) before. */
	private void rule(String name, String value) {
		server.runCommand("gamerule " + name + " " + value);
	}

	// ------------------------------------------------------------------ files

	private void clearFolder() {
		try {
			if (Files.isDirectory(dir)) {
				try (var old = Files.list(dir)) {
					for (Path file : old.toList()) {
						deleteTree(file);
					}
				}
			}
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void deleteTree(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		try (var walk = Files.walk(path)) {
			for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		}
	}

	public static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("Failed: " + what);
		}
	}
}
