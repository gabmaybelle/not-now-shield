package com.gabbymaybelle.shieldstrip;

import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Films the second YouTube Short, "Your shield was hiding this", with {@link Film}: a screenshot
 * every tick, in a tall 1080x1920 window. Only runs with
 * `./gradlew :fabric:runClientGameTest -PvideoTour` (on 26.3). Frames go in
 * build/run/clientGameTest/video_tour.
 *
 * <p>First person, survival, a sword in the hand and a shield in the off-hand, at the cottage's
 * garden edge at sunset, looking out to the treeline. The shield starts at vanilla's height (the
 * filming-only {@link #lift}), with a creeper standing behind it; the shield drops to the mod's
 * height and the creeper is there. Then the shield comes up and blocks its blast (the test checks
 * the hearts), the walk back to the cottage with the view clear, and the first shot's angle again,
 * so the Short loops. Each scene sets itself up, so any one can be filmed alone. With
 * {@code -PvideoWorld=<zip>} the set sits beside a saved world's kept "cottage" spot; without it,
 * in a plain spot near spawn.
 */
public class VideoTour implements FabricClientGameTest {
	/** Names the system properties: shieldstrip.videoTour, shieldstrip.videoScenes... */
	private static final String PREFIX = "shieldstrip";
	/** Sunset: warm sky in the west, still light enough for a creeper to read on a phone. */
	private static final int SUNSET = 12000;
	/** The game's field of view (the default, 70 degrees up and down): 2 tan(35 degrees). */
	private static final double VIEW = 2 * Math.tan(Math.toRadians(35));

	/**
	 * Filming only: how far {@code FilmShieldMixin} puts the shield back up, in the hand renderer's
	 * units. {@link LowerShield#DROP} draws it exactly at vanilla's height; 0 (always, outside these
	 * scenes) leaves the mod's own drawing alone.
	 */
	public static volatile float lift;

	private Film film;
	/** The cottage's spot: the end of its front path, facing north; y is the ground the player stands on. */
	private int sx, sy, sz;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Film.on(PREFIX)) {
			return;
		}
		film = new Film(context, PREFIX, "video_tour");
		film.take(() -> film.newWorld("not now shield"), this::stage, this::scenes);
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

	private Vec3 feet(double east, double south) {
		return new Vec3(sx + east, sy, sz + south);
	}

	private void cmd(String format, Object... args) {
		film.server.runCommand(String.format(Locale.ROOT, format, args));
	}

	/**
	 * Which way the reveal looks, as Minecraft's yaw: 75 just south of west (the setting sun off to
	 * the right, not behind the crosshair), 0 south (the woods), -90 east (the timber house).
	 * {@code -PvideoFacing=<yaw>} picks one.
	 */
	private double yaw() {
		return Double.parseDouble(film.prop("videoFacing", "75"));
	}

	/**
	 * Where the reveal stops for each facing: on the lawn round the cottage with open lawn ahead,
	 * the view tipped up a little so the creeper sits low on screen.
	 */
	private Vec3 stop() {
		double y = yaw();
		if (y == 0) {
			return feet(0.5, 6.5);
		}
		return y > 0 ? feet(8.5, 8.5) : feet(-0.5, 8.5);
	}

	/** Flat along the facing, one block long. */
	private Vec3 ahead() {
		double y = Math.toRadians(yaw());
		return new Vec3(-Math.sin(y), 0, Math.cos(y));
	}

	/** The reveal starts two blocks back and walks slowly to {@link #stop}. */
	private Vec3 start() {
		return stop().subtract(ahead().scale(2));
	}

	/**
	 * Degrees the view is tipped up (Minecraft's pitch is this, negated). A creeper's head is level
	 * with the player's eyes, so on flat ground it shows at the eye-level line wherever it stands:
	 * tipped up 14.8 degrees, that line is 0.19 screen heights below the centre, under the vanilla
	 * shield's top edge (12 degrees left the head peeking over the shield in the first take).
	 */
	private static final double UP = 14.8;

	/**
	 * Where the creeper's middle sits on screen at the stop, from the centre, in screen heights
	 * (x to the right, y down): between vanilla's resting shield's top edge (measured from a take:
	 * 0.16 below the centre over the creeper) and the mod's (0.32 below). With {@link #UP} this
	 * stands it about 13 blocks off, its head about 0.18 below the centre and its feet about 0.29.
	 */
	private static final double CREEPER_X = -0.19, CREEPER_Y = 0.235;

	/** A unit vector along the view, tipped up {@link #UP} degrees. */
	private Vec3 forward() {
		double p = Math.toRadians(UP);
		return ahead().scale(Math.cos(p)).add(0, Math.sin(p), 0);
	}

	/** A point the view looks at from {@code feet}: six blocks along it. */
	private Vec3 lookFrom(Vec3 feet) {
		return feet.add(0, 1.62, 0).add(forward().scale(6));
	}

	/**
	 * Where to stand the creeper so that, seen from {@link #stop}, its middle is at
	 * ({@link #CREEPER_X}, {@link #CREEPER_Y}) on screen: along that ray, where it falls to the
	 * creeper's middle height (0.85 above the ground the player stands on).
	 */
	private Vec3 creeperFeet() {
		Vec3 eye = stop().add(0, 1.62, 0);
		Vec3 f = forward();
		// The screen's right, flat; up is right x forward.
		double y = Math.toRadians(yaw());
		Vec3 right = new Vec3(-Math.cos(y), 0, -Math.sin(y));
		Vec3 up = right.cross(f);
		Vec3 ray = f.add(right.scale(CREEPER_X * VIEW)).add(up.scale(-CREEPER_Y * VIEW));
		double t = (eye.y - (sy + 0.85)) / -ray.y;
		Film.check(t > 0, "the creeper's ray falls to the ground");
		Vec3 mid = eye.add(ray.scale(t));
		return new Vec3(mid.x, sy, mid.z);
	}

	/**
	 * Every scene starts from this: no creeper left over, Normal difficulty (a creeper vanishes on
	 * Peaceful), no blocks broken by a blast, sunset, survival with the HUD, first person, the
	 * shield at the mod's height, walking speed and full health.
	 */
	private void reset() {
		lift = 0;
		cmd("kill @e[type=minecraft:creeper]");
		cmd("difficulty normal");
		cmd("gamerule spawn_mobs false");
		cmd("gamerule mob_griefing false");
		cmd("time set %d", SUNSET);
		film.calm();
		film.hud(true);
		film.survival();
		film.ctx.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			// A slower walk would narrow the view: keep the field of view still.
			mc.options.fovEffectScale().set(0.0);
		});
		cmd("effect clear @a");
		cmd("effect give @a minecraft:instant_health 1 10 true");
		cmd("effect give @a minecraft:saturation 1 10 true");
		// Tall grass and ferns on the lawn between the player and the creeper would stand in front of it.
		for (String plant : List.of("short_grass", "tall_grass", "fern", "large_fern")) {
			cmd("fill %d %d %d %d %d %d air replace %s", sx - 8, sy, sz + 2, sx + 11, sy + 1, sz + 20, plant);
		}
	}

	/** A sword in the hand (selected) and a shield in the off-hand; waits for the sword's name to fade. */
	private void kit() {
		cmd("clear @a");
		cmd("item replace entity @a hotbar.0 with iron_sword");
		cmd("item replace entity @a weapon.offhand with shield");
		film.ctx.getInput().pressKey(o -> o.keyHotbarSlots[0]);
		film.ctx.waitTicks(45);
	}

	/** Standing at {@code feet} looking at {@code look}, set up for a scene. */
	private void stand(Vec3 feet, Vec3 look) {
		film.camera(feet, look);
		kit();
		film.turnHead(look);
		film.ctx.waitTicks(4);
		speed(0.1);
	}

	/** A creeper standing still (no AI), facing the player at {@code eye}, unlit. */
	private void creeper(Vec3 at, Vec3 eye) {
		float yaw = (float) Math.toDegrees(Math.atan2(-(eye.x - at.x), eye.z - at.z));
		cmd("summon minecraft:creeper %.3f %.3f %.3f {NoAI:1b,PersistenceRequired:1b,Fuse:30,Rotation:[%.1ff,0f]}", at.x, at.y, at.z, yaw);
		Film.check(film.server.computeOnServer(s -> !s.overworld().getBlockState(net.minecraft.core.BlockPos.containing(at.x, at.y - 0.5, at.z)).isAir()),
				"the creeper stands on the lawn at " + at);
	}

	/**
	 * Sets the walking speed off camera, on the server and on the client: the client moves the
	 * player, and the command's value never reached it (a scout walked at full speed). Halved: the
	 * tick rate slows the world but not the player, who gets two of their own ticks per saved frame,
	 * so a walk at half speed plays at the game's real pace at 20 frames a second.
	 */
	private void speed(double walk) {
		double speed = walk / 2;
		cmd("attribute @a minecraft:movement_speed base set %s", speed);
		film.ctx.runOnClient(mc -> mc.player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed));
		film.ctx.waitTicks(2);
		Film.check(Math.abs(clientSpeed() - speed) < 1e-4, "the client walks at " + speed + " (it has " + clientSpeed() + ")");
	}

	private double clientSpeed() {
		return film.ctx.computeOnClient(mc -> mc.player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
	}

	/** Holds the walk key until the feet reach {@code to} along the facing (set the speed first), filming. */
	private void walkTo(Vec3 to) {
		Vec3 a = ahead();
		double end = to.dot(a) - 0.04;
		film.ctx.getInput().holdKey(o -> o.keyUp);
		film.log("walk", "");
		for (int i = 0; i < 200 && film.ctx.computeOnClient(mc -> mc.player.position().dot(a)) < end; i++) {
			film.record(1);
		}
		film.ctx.getInput().releaseKey(o -> o.keyUp);
	}

	/** Moves the shield from {@code from} to {@code to} (filming-only lift) over {@code n} frames, eased. */
	private void shieldTo(float from, float to, int n) {
		for (int i = 1; i <= n; i++) {
			lift = (float) (from + (to - from) * Film.ease(i / (double) n));
			film.record(1);
		}
	}

	// ------------------------------------------------------------------ the scenes

	private void scenes() {
		readSet();
		film.window(1080, 1920, 3);
		if (film.scouting()) {
			// The set and the claim, as stills before any action: the lawn from the stop with the
			// mod's shield and no creeper, then vanilla's shield with the creeper behind it.
			// Which way to look: the same still facing each way (the take uses videoFacing).
			String facing = film.prop("videoFacing", "75");
			for (String[] way : new String[][] {{"south", "0"}, {"west", "75"}, {"east", "-90"}}) {
				film.scene("x_set_" + way[0], () -> {
					System.setProperty(PREFIX + ".videoFacing", way[1]);
					reset();
					stand(stop(), lookFrom(stop()));
					film.record(1);
				});
			}
			System.setProperty(PREFIX + ".videoFacing", facing);
			film.scene("x_hidden", () -> {
				reset();
				creeper(creeperFeet(), stop().add(0, 1.62, 0));
				stand(stop(), lookFrom(stop()));
				lift = LowerShield.DROP;
				film.record(1);
			});
		}
		film.scene("s_reveal", this::reveal);
		film.scene("s_block", this::block);
		film.scene("s_walk", this::walk);
		film.scene("s_loop", this::loop);
		lift = 0;
	}

	/**
	 * Frame 1: the shield at vanilla's height in the lower left, a slow step forward across the lawn
	 * at sunset, nothing wrong. Then the shield drops to the mod's height, and a creeper is standing
	 * where it was.
	 */
	private void reveal() {
		reset();
		creeper(creeperFeet(), stop().add(0, 1.62, 0));
		stand(start(), lookFrom(start()));
		lift = LowerShield.DROP;
		film.note("creeper", creeperFeet().toString());
		speed(0.02);
		film.record(3);
		walkTo(stop());
		film.record(8);
		film.log("drop", "");
		shieldTo(LowerShield.DROP, 0, 6);
		film.record(16);
	}

	/**
	 * The creeper, closer now, starts to hiss; the shield comes up and takes the blast. The test
	 * checks the player was blocking and the hearts barely moved.
	 */
	private void block() {
		reset();
		Vec3 eye = stop().add(0, 1.62, 0);
		// Closer now, a little left of the view.
		Vec3 left = new Vec3(ahead().z, 0, -ahead().x);
		Vec3 at = stop().add(ahead().scale(3.2)).add(left.scale(0.5));
		creeper(at, eye);
		Vec3 look = at.add(0, 1.0, 0);
		stand(stop(), look);
		float before = film.ctx.computeOnClient(mc -> mc.player.getHealth());
		film.rolling();
		film.server.runOnServer(s -> s.overworld().getEntitiesOfClass(Creeper.class, new AABB(at, at).inflate(2)).forEach(Creeper::ignite));
		film.log("fuse", "");
		film.record(4);
		film.ctx.getInput().holdKey(o -> o.keyUse);
		film.log("shield_up", "");
		film.until(mc -> mc.player.isBlocking());
		film.untilServer(s -> s.overworld().getEntitiesOfClass(Creeper.class, new AABB(at, at).inflate(4)).isEmpty());
		film.log("boom", "");
		film.record(16);
		float after = film.ctx.computeOnClient(mc -> mc.player.getHealth());
		film.note("hearts", before + " -> " + after);
		Film.check(after >= before - 2, "the shield took the blast: health " + before + " -> " + after);
		film.ctx.getInput().releaseKey(o -> o.keyUse);
		film.log("shield_down", "");
		film.record(8);
	}

	/** Turned round, walking back towards the cottage with the shield lowered and the view clear. */
	private void walk() {
		reset();
		Vec3 from = feet(0.5, 10);
		Vec3 look = new Vec3(sx + 0.5, sy + 3.5, sz - 9);
		stand(from, look);
		speed(0.05);
		film.record(2);
		film.ctx.getInput().holdKey(o -> o.keyUp);
		film.log("walk", "");
		for (int i = 0; i < 56; i++) {
			film.turnHead(look);
			film.record(1);
		}
		film.ctx.getInput().releaseKey(o -> o.keyUp);
		film.record(6);
	}

	/** Back at the first shot's spot and angle, stepping into it, so the Short loops into its first frame. */
	private void loop() {
		reset();
		Vec3 from = start().subtract(ahead());
		stand(from, lookFrom(from));
		speed(0.02);
		film.record(2);
		walkTo(start());
		film.record(10);
	}
}
