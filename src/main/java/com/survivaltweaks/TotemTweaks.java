package com.survivaltweaks;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Totem of Undying rework (players only):
 *  - works from ANYWHERE in the inventory, not just the hands
 *  - saves you from the void: you're teleported to your respawn point
 *    (bed / respawn anchor / world spawn - vanilla's own lookup decides)
 *
 * Effects are exactly vanilla's: health set to 1, all effects cleared, then
 * Regeneration II (45s), Absorption II (5s), Fire Resistance (40s).
 *
 * Runs on Fabric's ALLOW_DEATH, which fires just BEFORE vanilla's own totem check.
 * If you die normally with a totem in your hand, this steps aside and lets vanilla
 * save you - that way the "Postmortal" advancement and all vanilla behaviour stay
 * intact without this mod having to touch advancement code. This mod only acts
 * where vanilla would NOT save you: totem elsewhere in the inventory, or the void.
 */
public final class TotemTweaks {
    private TotemTweaks() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true; // mobs keep vanilla behaviour
            }

            boolean voidDeath = source.is(DamageTypes.FELL_OUT_OF_WORLD);

            // Like vanilla, /kill and similar "unstoppable" damage still kills you.
            // The void is the one exception we deliberately add.
            if (!voidDeath && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return true;
            }

            // Normal death + totem in hand: vanilla handles it (and grants Postmortal).
            if (!voidDeath && hasTotemInHand(player)) {
                return true;
            }

            ItemStack totem = findTotem(player);
            if (totem.isEmpty()) {
                return true;
            }

            totem.shrink(1);

            // Vanilla "times used" statistic.
            player.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));

            player.setHealth(1.0f);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

            if (voidDeath) {
                // false = only read the bed/anchor position, don't use up an anchor charge.
                TeleportTransition transition =
                        player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
                player.teleport(transition);
                player.resetFallDistance();
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            }

            // Totem animation + sound on the client (entity event 35).
            player.level().broadcastEntityEvent(player, (byte) 35);

            return false; // cancel the death
        });
    }

    private static boolean hasTotemInHand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) {
                return true;
            }
        }
        return false;
    }

    /** Hands first (like vanilla), then the rest of the inventory. */
    private static ItemStack findTotem(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return stack;
            }
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
