package com.survivaltweaks;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Anvil rework: repairing and renaming cost NO levels (only the materials) and do
 * NOT raise the item's "prior work penalty" (the hidden repair_cost that makes
 * later enchant-combining more expensive). Combining enchantments works exactly
 * like vanilla, cost and penalty included.
 *
 * How "repair/rename" is detected: after vanilla has computed the anvil result, if
 * the result has exactly the same enchantments (and stored enchantments, for books)
 * as the left input, no enchantment was added or upgraded - so the operation was a
 * repair and/or rename. Then: cost -> 0, and the result keeps the left item's
 * original penalty.
 *
 * Also: Unbreaking can never be added to unbreakable gear here (see below).
 *
 * This is the ONLY feature in the mod that needs a mixin (Fabric has no anvil cost
 * event). The mixin (mixin/AnvilMenuMixin) is only two tiny hooks that call this
 * class, and its config is marked non-required: if a future Minecraft renames the
 * hooked methods, the game logs an error and the anvil simply behaves like vanilla
 * instead of crashing. Everything here uses only public, long-standing methods
 * (no private fields).
 */
public final class AnvilRules {
    private AnvilRules() {
    }

    // Vanilla's own public slot constants (verified in the 26.3 jar).
    private static final int LEFT_SLOT = AnvilMenu.INPUT_SLOT;
    private static final int RESULT_SLOT = AnvilMenu.RESULT_SLOT;
    /** The anvil's level cost is its first (and only) synced data value (AnvilMenu.cost DataSlot). */
    private static final int COST_DATA_ID = 0;

    /** Which open anvil menus currently show a free (repair/rename) result. */
    private static final Map<AnvilMenu, Boolean> FREE = Collections.synchronizedMap(new WeakHashMap<>());

    /** Called by the mixin right after vanilla's AnvilMenu.createResult(). */
    public static void afterCreateResult(AnvilMenu menu) {
        ItemStack left = menu.getSlot(LEFT_SLOT).getItem();
        ItemStack result = menu.getSlot(RESULT_SLOT).getItem();

        // Unbreaking can never be ADDED to unbreakable gear (netherite, elytra) in
        // the anvil - not even in creative mode, where vanilla skips its own
        // "can this be enchanted" check. No result = nothing to take.
        if (!result.isEmpty()
                && result.has(DataComponents.UNBREAKABLE)
                && hasUnbreaking(result)
                && !hasUnbreaking(left)) {
            menu.getSlot(RESULT_SLOT).set(ItemStack.EMPTY);
            menu.setData(COST_DATA_ID, 0);
            FREE.put(menu, false);
            return;
        }

        boolean free = !left.isEmpty()
                && !result.isEmpty()
                && Objects.equals(left.get(DataComponents.ENCHANTMENTS), result.get(DataComponents.ENCHANTMENTS))
                && Objects.equals(left.get(DataComponents.STORED_ENCHANTMENTS), result.get(DataComponents.STORED_ENCHANTMENTS));

        FREE.put(menu, free);

        if (free) {
            menu.setData(COST_DATA_ID, 0);
            // Keep the original penalty instead of vanilla's increased one.
            result.set(DataComponents.REPAIR_COST, left.getOrDefault(DataComponents.REPAIR_COST, 0));
        }
    }

    private static boolean hasUnbreaking(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            return false;
        }
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            if (enchantment.is(Enchantments.UNBREAKING)) {
                return true;
            }
        }
        return false;
    }

    /** Called by the mixin: vanilla refuses to hand out results that cost 0 levels. */
    public static boolean isFree(AnvilMenu menu) {
        return Boolean.TRUE.equals(FREE.get(menu));
    }
}
