package com.gabbymaybelle.shieldstrip;

import static com.gabbymaybelle.shieldstrip.ClientTest.check;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Forge jar's in-game test: the same checks as the Fabric game test (fabric/src/gametest).
 * An axe still strips a log with a shield in the off-hand and leaves a click on stone for the
 * shield (1.20.1's own behaviour, which the mod must not change), and a shield is drawn lower,
 * resting and raised.
 */
final class ShieldstripClientTest {
	/** The lowering hook's count at the last check. */
	private static int before;

	private ShieldstripClientTest() {
	}

	static void steps() {
		createWorld("shieldstrip-clienttest");

		onServer("an axe with a shield in the off-hand", server -> {
			ServerLevel level = server.overworld();
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			BlockPos base = player.blockPosition().offset(2, 0, 0);
			StringBuilder out = new StringBuilder();
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
			if (!use(player, level, base, Blocks.OAK_LOG) || !level.getBlockState(base).is(Blocks.STRIPPED_OAK_LOG)) {
				out.append("\n  an axe on an oak log gave ").append(level.getBlockState(base).getBlock());
			}
			if (use(player, level, base.south(), Blocks.STONE)) {
				out.append("\n  an axe on stone took the click, so the shield wouldn't go up");
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return out.toString();
		});

		// The off-hand shield is drawn through LowerShield, resting and raised; screenshots of both.
		waitUntil("the shield in the player's off-hand", 100,
				mc -> mc.player.getOffhandItem().is(Items.SHIELD) && mc.player.getMainHandItem().isEmpty());
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
