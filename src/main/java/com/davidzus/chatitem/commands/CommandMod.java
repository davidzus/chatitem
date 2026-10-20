package com.davidzus.chatitem.commands;

import com.davidzus.chatitem.Chatitem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /chatitem [offhand] — broadcast held item (hoverable) to all players.
 * Port of Davidzus chatitem 1.0.1 for 26.3.
 */
public final class CommandMod {
	private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();
	private static final long COOLDOWN_MS = 15_000L;

	private CommandMod() {}

	public static void registerCommands() {
		Chatitem.LOGGER.info("Registering Mod for " + Chatitem.MOD_ID);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("chatitem")
				.executes(ctx -> executeWithCooldown(ctx, EquipmentSlot.MAINHAND))
				.then(Commands.literal("offhand")
					.executes(ctx -> executeWithCooldown(ctx, EquipmentSlot.OFFHAND)))
		);
	}

	private static int executeWithCooldown(CommandContext<CommandSourceStack> ctx, EquipmentSlot slot) {
		CommandSourceStack source = ctx.getSource();

		if (source.getEntity() == null) {
			source.sendFailure(Component.literal("This command cannot be run from the server/console."));
			return 0;
		}

		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("You must be a player to run this command."));
			return 0;
		}

		UUID uuid = player.getUUID();
		long now = System.currentTimeMillis();
		Long last = LAST_USE.get(uuid);
		if (last != null && now - last < COOLDOWN_MS) {
			long secsLeft = (COOLDOWN_MS - (now - last) + 999) / 1000;
			source.sendSuccess(
				() -> Component.literal("Please wait " + secsLeft + " more second(s) before using /chatitem again."),
				false);
			return 0;
		}

		ItemStack stack = player.getItemBySlot(slot);
		if (stack.isEmpty()) {
			player.sendSystemMessage(
				Component.literal("You must be holding an item in your hand to run this."));
			return 0;
		}

		LAST_USE.put(uuid, now);
		return broadcastEquipped(ctx, stack);
	}

	private static int broadcastEquipped(CommandContext<CommandSourceStack> ctx, ItemStack stack) {
		MutableComponent message = Component.literal("")
			.append(Component.literal(ctx.getSource().getPlayer().getName().getString()).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" is holding "))
			.append(stack.getDisplayName());

		ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(message, false);
		return 1;
	}
}
