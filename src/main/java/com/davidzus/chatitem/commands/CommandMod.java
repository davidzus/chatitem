package com.davidzus.chatitem.commands;

import com.davidzus.chatitem.Chatitem;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandMod{

    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 15_000L;

    public static void registerCommands() {
        Chatitem.LOGGER.info("Registering Mod for " + Chatitem.MOD_ID);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(
                    CommandManager.literal("chatitem")
                            .executes(ctx -> executeWithCooldown(ctx, EquipmentSlot.MAINHAND))
                            .then(CommandManager.literal("offhand")
                                    .executes(ctx -> executeWithCooldown(ctx, EquipmentSlot.OFFHAND)))
            );
        });
    }

    private static int executeWithCooldown(CommandContext<ServerCommandSource> ctx, EquipmentSlot slot)
            throws CommandSyntaxException {
        ServerCommandSource src = ctx.getSource();

        if (src.getEntity() == null) {
            src.sendError(Text.literal("This command cannot be run from the server/console."));
            return 0;
        }

        if (!(src.getEntity() instanceof ServerPlayerEntity)) {
            src.sendError(Text.literal("You must be a player to run this command."));
            return 0;
        }

        PlayerEntity player = src.getPlayer();
        assert player != null;
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("You must be holding an item in your hand to run this."), false);
            return 0;
        }

        UUID uuid = player.getUuid();

        long now = System.currentTimeMillis();
        Long last = LAST_USE.get(uuid);
        if (last != null && now - last < COOLDOWN_MS) {
            long secsLeft = (COOLDOWN_MS - (now - last) + 999) / 1000;
            src.sendFeedback(
                    () -> Text.literal("Please wait " + secsLeft + " more second(s) before using §4/chatitem§r again."),
                    false
            );
            return 0;
        }

        LAST_USE.put(uuid, now);
        return broadcastEquipped(ctx, slot);
    }

    private static int broadcastEquipped(CommandContext<ServerCommandSource> ctx, EquipmentSlot slot) {
        ServerCommandSource src = ctx.getSource();
        PlayerEntity player = src.getPlayer();
        ItemStack stack = player.getEquippedStack(slot);

        Text hoverableItem = stack.toHoverableText();

        Text message = Text.literal("")
                .append(Text.literal(player.getName().getString()).formatted(Formatting.GOLD))
                .append(Text.literal(" is holding "))
                .append(hoverableItem);

        for (ServerPlayerEntity p : src.getServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(message, false);
        }

        return 1;
    }
}
