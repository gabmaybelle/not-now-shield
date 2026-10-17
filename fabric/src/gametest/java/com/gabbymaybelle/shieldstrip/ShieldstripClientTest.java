package com.gabbymaybelle.shieldstrip;

import com.gabbymaybelle.shieldstrip.harness.ClientGameTest;
import com.gabbymaybelle.shieldstrip.harness.ClientGameTestContext;
import com.gabbymaybelle.shieldstrip.harness.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * With a shield in the off-hand, a right-click with a tool still changes the block, and when
 * there's nothing to change the click is left for the shield. Driven through the server's own
 * player actions, the same path a real click takes. Then, on the player's screen, a shield is
 * drawn lower, resting and raised. Run with `./gradlew :fabric:runClientGameTest`.
 */
public class ShieldstripClientTest implements ClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.waitForChunksRender();
			String failures = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos base = player.blockPosition().offset(2, 0, 0);
				StringBuilder out = new StringBuilder();
				player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

				expectChange(out, player, level, base, Items.IRON_AXE, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
				// Hoes and shovels have no off-hand check on 1.21.1, so they work with or without the mod.
				expectChange(out, player, level, base.east(), Items.IRON_HOE, Blocks.DIRT, Blocks.FARMLAND);
				expectChange(out, player, level, base.east(2), Items.IRON_SHOVEL, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH);
				// Nothing to strip: the click isn't taken, so the game goes on to raise the shield.
				if (use(player, level, base.south(), Items.IRON_AXE, Blocks.STONE)) {
					out.append("\n  an axe on stone took the click, so the shield wouldn't go up");
				}
				player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return out.toString();
			});
			failures += lowerShield(ctx, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("Failed (Minecraft 1.21.1):" + failures);
			}
		}
	}

	/** A shield in the off-hand is drawn through LowerShield, resting and raised; screenshots of both. */
	private static String lowerShield(ClientGameTestContext ctx, TestSingleplayerContext world) {
		StringBuilder out = new StringBuilder();
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
				.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD)));
		ctx.waitFor(mc -> mc.player != null && mc.player.getOffhandItem().is(Items.SHIELD), 200);
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

	private static void expectChange(StringBuilder out, ServerPlayer player, ServerLevel level, BlockPos pos, Item tool, Block from, Block to) {
		boolean used = use(player, level, pos, tool, from);
		if (!used || !level.getBlockState(pos).is(to)) {
			out.append("\n  ").append(tool).append(" on ").append(from).append(" with a shield in the off-hand gave ")
					.append(level.getBlockState(pos).getBlock()).append(" (wanted ").append(to).append(")");
		}
	}

	/** Puts the block down with air above it and right-clicks its top with the tool; true if the click was taken. */
	private static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Item tool, Block block) {
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
		ItemStack stack = new ItemStack(tool);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit).consumesAction();
	}
}
