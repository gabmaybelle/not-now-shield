package com.gabbymaybelle.shieldstrip;

import static com.gabbymaybelle.shieldstrip.ClientTest.check;
import static com.gabbymaybelle.shieldstrip.ClientTest.command;
import static com.gabbymaybelle.shieldstrip.ClientTest.createWorld;
import static com.gabbymaybelle.shieldstrip.ClientTest.onServer;
import static com.gabbymaybelle.shieldstrip.ClientTest.run;
import static com.gabbymaybelle.shieldstrip.ClientTest.screenshot;
import static com.gabbymaybelle.shieldstrip.ClientTest.waitTicks;
import static com.gabbymaybelle.shieldstrip.ClientTest.waitUntil;

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
 * The NeoForge jar's in-game test: the same checks as the Fabric game test (fabric/src/gametest).
 * Tools change blocks with a shield in the off-hand (axes, and hoes and shovels as vanilla), an axe
 * on stone leaves the click for the shield, and a shield is drawn lowered, resting and raised.
 */
final class ShieldstripClientTest {
	/** The lowering hook's count at the last check. */
	private static int before;

	private ShieldstripClientTest() {
	}

	static void steps() {
		createWorld("shieldstrip-clienttest");
		command("gamemode survival @a");

		onServer("tools with a shield in the off-hand", server -> {
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
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return out.toString();
		});

		// A shield in the off-hand is drawn through LowerShield, resting and raised; screenshots of both.
		waitUntil("the shield in the player's off-hand", 100, mc -> mc.player.getOffhandItem().is(Items.SHIELD));
		run(mc -> {
			// Face north, away from the test blocks (east and south), so the hands are clear to see.
			mc.player.setYRot(180);
			mc.player.setXRot(0);
		});
		waitTicks(20);
		run(mc -> before = LowerShield.lowered);
		waitTicks(5);
		run(mc -> check(LowerShield.lowered != before, "A resting shield wasn't drawn lower"));
		screenshot();
		// Hold right-click like a player: with an empty main hand, the off-hand shield goes up.
		run(mc -> mc.options.keyUse.setDown(true));
		waitTicks(10);
		run(mc -> check(mc.player.isUsingItem(), "The shield didn't go up on the player's side"));
		run(mc -> before = LowerShield.lowered);
		waitTicks(5);
		run(mc -> check(LowerShield.lowered != before, "A raised shield wasn't drawn lower"));
		screenshot();
		run(mc -> mc.options.keyUse.setDown(false));
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
