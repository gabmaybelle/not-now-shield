package com.gabbymaybelle.shieldstrip;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
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
 * player actions, the same path a real click takes. Run with `./gradlew runClientGameTest`
 * (add `-Pminecraft_version=26.3 -Pfabric_api_version=0.162.0+26.3` for 26.3).
 */
public class ShieldstripClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		boolean is263 = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow()
				.getMetadata().getVersion().getFriendlyString().startsWith("26.3");
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamemode survival @a");
			String failures = world.getServer().computeOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos base = player.blockPosition().offset(2, 0, 0);
				StringBuilder out = new StringBuilder();
				player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));

				expectChange(out, player, level, base, Items.IRON_AXE, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
				if (is263) {
					// 26.3 moved hoes and shovels onto the same check, so they're covered too.
					expectChange(out, player, level, base.east(), Items.IRON_HOE, Blocks.DIRT, Blocks.FARMLAND);
					expectChange(out, player, level, base.east(2), Items.IRON_SHOVEL, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH);
				}
				// Nothing to strip: the click isn't taken, so the game goes on to raise the shield.
				if (use(player, level, base.south(), Items.IRON_AXE, Blocks.STONE)) {
					out.append("\n  an axe on stone took the click, so the shield wouldn't go up");
				}
				player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return out.toString();
			});
			if (!failures.isEmpty()) {
				throw new AssertionError("Failed (Minecraft " + (is263 ? "26.3" : "26.2") + "):" + failures);
			}
		}
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
