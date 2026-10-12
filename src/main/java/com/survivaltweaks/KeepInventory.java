package com.survivaltweaks;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Partial keep-inventory: on death you KEEP your worn armor, your off-hand item
 * and your hotbar. Everything else (main inventory, XP) drops as usual.
 *
 * How it works:
 *  1. mixin/PlayerDropEquipmentMixin calls beforeDrops() at the very start of
 *     vanilla's Player.dropEquipment(ServerLevel) - the method that drops a dead
 *     player's inventory. The kept items are taken out of the inventory there, so
 *     vanilla never drops them.
 *  2. They are stored on the player as a Fabric data attachment that is SAVED with
 *     the player and COPIED to the respawned player (copyOnDeath). So nothing is
 *     lost even if you quit to the title screen before clicking "Respawn".
 *  3. Fabric's AFTER_RESPAWN puts them back into the same slots.
 *
 * No duplication: verified in the 26.3 jar that Inventory.dropAll() inside
 * dropEquipment is the ONLY place a dead player's items drop (the player loot
 * table is empty). Kept items are removed before it runs, so they never exist as
 * dropped items, not even for a tick, and after respawning they exist only in
 * the new player's inventory.
 *
 * Items with Curse of Vanishing are not kept - vanilla destroys them as usual.
 * If the mixin ever fails to apply (future update), deaths simply drop
 * everything like vanilla; nothing breaks. All members verified in the 26.3 jar.
 */
public final class KeepInventory {
    private KeepInventory() {
    }

    /** Equipment slots kept on death (MAINHAND is a hotbar slot, kept via the hotbar). */
    private static final List<EquipmentSlot> KEPT_EQUIPMENT = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND);

    private static final String HOTBAR_PREFIX = "hotbar_";
    private static final String EQUIPMENT_PREFIX = "equipment_";

    /** slot key -> item. Keys look like "hotbar_3" or "equipment_HEAD". Never rename (saved data). */
    public static final AttachmentType<Map<String, ItemStack>> KEPT_ITEMS = AttachmentRegistry.create(
            SurvivalTweaks.id("kept_items"),
            builder -> builder
                    .persistent(Codec.unboundedMap(Codec.STRING, ItemStack.CODEC))
                    .copyOnDeath());

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                // Clear the dead entity's copy too, so the items exist in exactly one place.
                oldPlayer.removeAttached(KEPT_ITEMS);
                restore(newPlayer);
            }
        });
    }

    /** Called by the mixin right before vanilla drops a dead player's inventory. */
    public static void beforeDrops(Player player) {
        // Only for a player who is really dying (dropEquipment is only called on death
        // in vanilla; this guards against other mods calling it).
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.isDeadOrDying()) {
            return;
        }
        Map<String, ItemStack> kept = new HashMap<>();
        Inventory inventory = serverPlayer.getInventory();

        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (shouldKeep(stack)) {
                kept.put(HOTBAR_PREFIX + i, stack.copy());
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        for (EquipmentSlot slot : KEPT_EQUIPMENT) {
            ItemStack stack = serverPlayer.getItemBySlot(slot);
            if (shouldKeep(stack)) {
                kept.put(EQUIPMENT_PREFIX + slot.name(), stack.copy());
                serverPlayer.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        if (!kept.isEmpty()) {
            // Merge with anything still pending (e.g. died twice without respawning).
            Map<String, ItemStack> pending = serverPlayer.getAttached(KEPT_ITEMS);
            if (pending != null) {
                Map<String, ItemStack> merged = new HashMap<>(pending);
                merged.putAll(kept);
                kept = merged;
            }
            serverPlayer.setAttached(KEPT_ITEMS, Map.copyOf(kept));
        }
    }

    private static boolean shouldKeep(ItemStack stack) {
        return !stack.isEmpty()
                && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP); // Curse of Vanishing
    }

    private static void restore(ServerPlayer player) {
        Map<String, ItemStack> kept = player.removeAttached(KEPT_ITEMS);
        if (kept == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (Map.Entry<String, ItemStack> entry : kept.entrySet()) {
            String key = entry.getKey();
            ItemStack stack = entry.getValue().copy();
            boolean placed = false;
            try {
                if (key.startsWith(HOTBAR_PREFIX)) {
                    int index = Integer.parseInt(key.substring(HOTBAR_PREFIX.length()));
                    if (inventory.getItem(index).isEmpty()) {
                        inventory.setItem(index, stack);
                        placed = true;
                    }
                } else if (key.startsWith(EQUIPMENT_PREFIX)) {
                    EquipmentSlot slot = EquipmentSlot.valueOf(key.substring(EQUIPMENT_PREFIX.length()));
                    if (player.getItemBySlot(slot).isEmpty()) {
                        player.setItemSlot(slot, stack);
                        placed = true;
                    }
                }
            } catch (IllegalArgumentException e) {
                // unknown/renamed slot in old save data: fall through to "add anywhere"
            }
            if (!placed && !inventory.add(stack) && !stack.isEmpty()) {
                player.spawnAtLocation(player.level(), stack); // inventory full: drop at feet
            }
        }
    }
}
