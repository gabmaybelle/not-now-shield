package com.gabbymaybelle.shieldstrip;

import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Films Part 1 of the series "Small mods that still feel vanilla" (mods-media not-now-shield/SHORT-3-PLAN.md)
 * with {@link Film}: tall, a screenshot every tick. Only runs with
 * `./gradlew :fabric:runClientGameTest -PvideoTour -PvideoPart1` (on 26.3, where hoes and shovels need the
 * fix too); {@link VideoTour} steps aside then. Frames go in build/run/clientGameTest/video_tour.
 *
 * <p>What only this mod does leads: tools work with a shield in the off-hand (vanilla raises the shield
 * instead). At the cozy cottage's woodpile at golden hour, in third person in Gabby's skin: the axe strips two
 * logs with the shield in view; the hoe tills and the shovel makes a path; then, in first person (the lowered
 * shield is drawn only there), the shield comes up and blocks an arrow, sitting lower than vanilla's; and the
 * woodpile again, so the Short loops. Each scene sets itself up.
 */
public class Part1 implements FabricClientGameTest {
	private static final String PREFIX = "shieldstrip";
	/** Golden hour: the sun low and warm, just before Short 2's sunset (11000 was still a midday sky: first scout). */
	private static final int GOLDEN = 11900;

	private Film film;
	private int sx, sy, sz;
	private Vec3 looking;

	/** Whether this run films Part 1: {@code -PvideoPart1} with {@code -PvideoTour}. */
	public static boolean on() {
		return Film.on(PREFIX) && Boolean.getBoolean(PREFIX + ".videoPart1");
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!on()) {
			return;
		}
		film = new Film(context, PREFIX, "video_tour");
		film.take(() -> film.newWorld("not now shield"), this::stage, this::scenes);
	}

	// ------------------------------------------------------------------ the set (as the first Short's)

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

	private BlockPos at(int east, int up, int south) {
		return new BlockPos(sx + east, sy + up, sz + south);
	}

	private Vec3 feet(double east, double south) {
		return new Vec3(sx + east, sy, sz + south);
	}

	private void cmd(String format, Object... args) {
		film.server.runCommand(String.format(Locale.ROOT, format, args));
	}

	private void fill(BlockPos a, BlockPos b, String block) {
		cmd("fill %d %d %d %d %d %d %s", a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block);
	}

	private void set(BlockPos p, String block) {
		cmd("setblock %d %d %d %s", p.getX(), p.getY(), p.getZ(), block);
	}

	/** The woodpile south of the cottage's path (three oak logs, two high) and a grass strip to its west. */
	private void buildSet() {
		fill(at(-6, 0, 1), at(8, 4, 11), "air");
		fill(at(-6, -1, 1), at(8, -1, 11), "grass_block");
		fill(at(3, 0, 2), at(5, 1, 2), "oak_log[axis=x]");
		fill(at(-5, -1, 3), at(-1, -1, 3), "grass_block");
	}

	/** Axe, hoe, shovel and an empty slot on the hotbar, a shield in the off-hand, the axe selected. */
	private void kit() {
		cmd("clear @a");
		cmd("item replace entity @a hotbar.0 with iron_axe");
		cmd("item replace entity @a hotbar.1 with iron_hoe");
		cmd("item replace entity @a hotbar.2 with iron_shovel");
		cmd("item replace entity @a weapon.offhand with shield");
		slot(0);
		film.ctx.waitTicks(45);
	}

	private void slot(int i) {
		film.ctx.getInput().pressKey(o -> o.keyHotbarSlots[i]);
	}

	/** Golden hour, survival with the HUD, the set fresh, the kit, standing at {@code feet} looking at {@code look}. */
	private void ready(Vec3 feet, Vec3 look, boolean third) {
		VideoTour.lift = 0;
		cmd("time set %d", GOLDEN);
		cmd("difficulty normal");
		cmd("gamerule spawn_mobs false");
		film.calm();
		film.hud(true);
		film.survival();
		film.thirdPerson(third, 6);
		film.ctx.runOnClient(mc -> mc.options.fovEffectScale().set(0.0));
		cmd("effect give @a minecraft:instant_health 1 10 true");
		buildSet();
		film.camera(feet, look);
		kit();
		film.turnHead(look);
		film.ctx.runOnClient(mc -> mc.player.setYBodyRot(mc.player.getYRot()));
		film.ctx.waitTicks(2);
	}

	private static Vec3 south(BlockPos p) {
		return new Vec3(p.getX() + 0.12, p.getY() + 0.5, p.getZ() + 0.98);
	}

	private static Vec3 top(BlockPos p) {
		return new Vec3(p.getX() + 0.12, p.getY() + 0.98, p.getZ() + 0.5);
	}

	private void sweep(Vec3 to, int n) {
		Vec3 from = looking == null ? to : looking;
		for (int i = 1; i <= n; i++) {
			film.turnHead(from.lerp(to, Film.ease(i / (double) n)));
			film.ctx.runOnClient(mc -> mc.player.setYBodyRot(mc.player.getYRot()));
			film.record(1);
		}
		looking = to;
	}

	/** Right-clicks the block under the crosshair (it must be {@code block}), then films the result. */
	private void use(BlockPos block, String what, String after, int frames) {
		String on = film.ctx.computeOnClient(Part1::crosshair);
		Film.check(on.equals(block.toShortString()), what + ": the crosshair is on " + block.toShortString() + " (it is on " + on + ")");
		film.ctx.getInput().pressKey(o -> o.keyUse);
		film.log(what, block.toShortString());
		film.record(frames);
		String now = film.server.computeOnServer(s -> net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getKey(s.overworld().getBlockState(block).getBlock()).getPath());
		Film.check(now.equals(after), what + " turned " + block.toShortString() + " into " + after + " with the shield in the off-hand (it is " + now + ")");
	}

	private static String crosshair(Minecraft mc) {
		return mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
				? hit.getBlockPos().toShortString() : "nothing";
	}

	// ------------------------------------------------------------------ the scenes

	private void scenes() {
		readSet();
		film.window(1080, 1920, 3);
		film.scene("p_strip", this::strip);
		film.scene("p_garden", this::garden);
		film.scene("p_block", this::block);
		film.scene("p_loop", this::loop);
		VideoTour.lift = 0;
	}

	private Vec3 stripStand() {
		return feet(4.5, 5.5);
	}

	/** Frame 1 is the payoff: the axe already at the first log, the shield beside it; then the next log. */
	private void strip() {
		BlockPos log = at(4, 1, 2), next = at(5, 1, 2);
		Vec3 start = south(log).add(-0.6, 0.15, 0);
		ready(stripStand(), start, true);
		film.subject(Vec3.atCenterOf(log));
		looking = start;
		sweep(south(log), 3);
		use(log, "strip", "stripped_oak_log", 18);
		film.subject(Vec3.atCenterOf(next));
		sweep(south(next), 5);
		use(next, "strip", "stripped_oak_log", 22);
	}

	/**
	 * The grass strip: the hoe tills three blocks, then the shovel makes a path of the fourth (three tills
	 * since her voice-over: the line "Now it strips logs, tills dirt, makes paths" runs about 3.2 s).
	 */
	private void garden() {
		BlockPos a = at(-4, -1, 3), b = at(-3, -1, 3), b2 = at(-2, -1, 3), c = at(-1, -1, 3);
		ready(feet(-2.5, 5.3), top(a).add(-0.3, 0, 0.2), true);
		slot(1);
		film.ctx.waitTicks(45);
		film.subject(Vec3.atCenterOf(b));
		looking = top(a).add(-0.3, 0, 0.2);
		sweep(top(a), 3);
		use(a, "till", "farmland", 6);
		sweep(top(b), 4);
		use(b, "till", "farmland", 8);
		sweep(top(b2), 5);
		use(b2, "till", "farmland", 8);
		film.subject(Vec3.atCenterOf(c));
		slot(2);
		film.log("slot", "shovel");
		film.record(4);
		sweep(top(c), 5);
		use(c, "path", "dirt_path", 18);
	}

	/**
	 * First person (the lowered shield is drawn only there), looking out over the lawn: the shield comes
	 * up and an arrow from the dusk thunks into it. The test checks it was blocking and the hearts held.
	 */
	private void block() {
		// West over the lawn towards the low sun, Short 2's view (south looked at a dirt bank in the first scout).
		Vec3 stand = feet(8.5, 8.5);
		Vec3 west = new Vec3(-Math.sin(Math.toRadians(75)), 0, Math.cos(Math.toRadians(75)));
		Vec3 look = stand.add(west.scale(12)).add(0, 1.6, 0);
		ready(stand, look, false);
		slot(3);
		film.ctx.waitTicks(10);
		float before = film.ctx.computeOnClient(mc -> mc.player.getHealth());
		film.rolling();
		film.record(6);
		film.ctx.getInput().holdKey(o -> o.keyUse);
		film.log("shield_up", "");
		film.until(mc -> mc.player.isBlocking());
		// An arrow from 14 blocks out, straight at her chest, at a bow's full speed.
		Vec3 from = stand.add(west.scale(14)).add(0, 1.4, 0);
		Vec3 to = stand.add(0, 1.3, 0);
		Vec3 v = to.subtract(from).normalize().scale(2.5);
		cmd("summon arrow %.3f %.3f %.3f {Motion:[%.3f,%.3f,%.3f],pickup:0b,damage:4.0d}", from.x, from.y, from.z, v.x, v.y + 0.06, v.z);
		film.log("arrow", "");
		film.record(16);
		film.log("blocked", "");
		film.record(8);
		film.ctx.getInput().releaseKey(o -> o.keyUse);
		film.log("shield_down", "");
		film.record(10);
		float after = film.ctx.computeOnClient(mc -> mc.player.getHealth());
		film.note("hearts", before + " -> " + after);
		Film.check(after >= before, "the raised shield blocked the arrow: health " + before + " -> " + after);
	}

	/** Back at the first shot's angle, the logs fresh again, so the Short loops into its first frame. */
	private void loop() {
		BlockPos log = at(4, 1, 2);
		Vec3 start = south(log).add(-0.6, 0.15, 0);
		ready(stripStand(), south(at(3, 1, 2)), true);
		film.subject(Vec3.atCenterOf(log));
		looking = south(at(3, 1, 2));
		// Slow, since her voice-over: the last line and the follow line run about 3.5 s over it.
		sweep(start, 52);
		film.record(20);
	}
}
