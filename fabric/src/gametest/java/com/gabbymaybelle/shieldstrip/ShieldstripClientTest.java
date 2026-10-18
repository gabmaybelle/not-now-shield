package com.gabbymaybelle.shieldstrip;

import com.gabbymaybelle.shieldstrip.harness.ClientGameTest;
import com.gabbymaybelle.shieldstrip.harness.ClientGameTestContext;
import com.gabbymaybelle.shieldstrip.harness.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * On the player's screen, a shield is drawn lower, resting and raised. And, since 1.20.1's axe
 * has no off-hand check, an axe strips a log with a shield in the off-hand (with or without the
 * mod), and an axe on stone leaves the click for the shield; this guards that the mod never gets
 * in the way. Run with `./gradlew :fabric:runClientGameTest`.
 */
public class ShieldstripClientTest implements ClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.waitForChunksRender();
			String failures = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				BlockPos base = player.blockPosition().offset(2, 0, 0);
				StringBuilder out = new StringBuilder();
				player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
				if (!use(player, level, base, Blocks.OAK_LOG) || !level.getBlockState(base).is(Blocks.STRIPPED_OAK_LOG)) {
					out.append("\n  an axe on an oak log with a shield in the off-hand gave ").append(level.getBlockState(base).getBlock());
				}
				if (use(player, level, base.south(), Blocks.STONE)) {
					out.append("\n  an axe on stone took the click, so the shield wouldn't go up");
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return out.toString();
			});
			failures += lowerShield(ctx);
			if (!failures.isEmpty()) {
				throw new AssertionError("Failed (Minecraft 1.20.1):" + failures);
			}
		}
	}

	/** The off-hand shield is drawn through LowerShield, resting and raised; screenshots of both. */
	private static String lowerShield(ClientGameTestContext ctx) {
		StringBuilder out = new StringBuilder();
		ctx.waitFor(mc -> mc.player != null && mc.player.getOffhandItem().is(Items.SHIELD)
				&& mc.player.getMainHandItem().isEmpty(), 200);
		// Face north, away from the test blocks (east and south), so the hands are clear to see.
		ctx.runOnClient(mc -> {
			mc.player.setYRot(180);
			mc.player.setXRot(0);
		});
		ctx.waitTicks(20);
		int before = ctx.computeOnClient(mc -> LowerShield.lowered);
		ctx.waitTicks(5);
		if (ctx.computeOnClient(mc -> LowerShield.lowered) == before) {
			out.append("\n  a resting shield wasn't drawn lower");
		}
		ctx.takeScreenshot("shield-resting");

		// Hold right-click like a player: with an empty main hand, the off-hand shield goes up.
		ctx.runOnClient(mc -> mc.options.keyUse.setDown(true));
		ctx.waitTicks(10);
		if (!ctx.computeOnClient(mc -> mc.player.isUsingItem())) {
			out.append("\n  the shield didn't go up on the player's side");
		}
		before = ctx.computeOnClient(mc -> LowerShield.lowered);
		ctx.waitTicks(5);
		if (ctx.computeOnClient(mc -> LowerShield.lowered) == before) {
			out.append("\n  a raised shield wasn't drawn lower");
		}
		ctx.takeScreenshot("shield-raised");
		ctx.runOnClient(mc -> mc.options.keyUse.setDown(false));
		return out.toString();
	}

	/** Puts the block down with air above it and right-clicks its top with an iron axe; true if the click was taken. */
	private static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Block block) {
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
		ItemStack stack = new ItemStack(Items.IRON_AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit).consumesAction();
	}
}
