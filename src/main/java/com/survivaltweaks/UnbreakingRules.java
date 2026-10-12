package com.survivaltweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Objects;

/**
 * Unbreaking is pointless on unbreakable gear, so:
 *  1. It can't be put on any item that has the "unbreakable" component (all
 *     netherite gear and the elytra, see ItemTweaks) - neither from the enchanting
 *     table nor with books in the anvil. Fabric's EnchantmentEvents.ALLOW_ENCHANTING
 *     covers both. Because it checks the component rather than a list of items,
 *     future netherite items are covered automatically.
 *  2. Items that already have it lose it. The typical case: diamond armor/tools
 *     with Unbreaking upgraded to netherite in the smithing table. Every second,
 *     each online player's inventory is checked and Unbreaking is removed from
 *     unbreakable items (also covers loot and old items from before this version).
 */
public final class UnbreakingRules {
    private UnbreakingRules() {
    }

    private static final int CHECK_INTERVAL_TICKS = 20;

    public static void register() {
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment, target, context) ->
                enchantment.is(Enchantments.UNBREAKING) && target.has(DataComponents.UNBREAKABLE)
                        ? TriState.FALSE
                        : TriState.DEFAULT);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                stripFromInventory(player);
            }
        });
    }

    private static void stripFromInventory(ServerPlayer player) {
        Holder<Enchantment> unbreaking = player.level().registryAccess().getOrThrow(Enchantments.UNBREAKING);
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.has(DataComponents.UNBREAKABLE) || !stack.has(DataComponents.ENCHANTMENTS)) {
                continue;
            }
            // Work on a copy and only write back if something actually changed,
            // so untouched items never cause needless inventory updates.
            ItemStack copy = stack.copy();
            EnchantmentHelper.updateEnchantments(copy, enchantments -> enchantments.set(unbreaking, 0));
            if (!Objects.equals(copy.get(DataComponents.ENCHANTMENTS), stack.get(DataComponents.ENCHANTMENTS))) {
                stack.set(DataComponents.ENCHANTMENTS, copy.get(DataComponents.ENCHANTMENTS));
            }
        }
    }
}
