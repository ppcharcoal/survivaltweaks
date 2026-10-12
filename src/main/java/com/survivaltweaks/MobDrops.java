package com.survivaltweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;

/**
 * Drowned holding a trident drop it 70% of the time (vanilla: 8.5%).
 *
 * A drowned's trident is not loot-table loot: it's an EQUIPMENT drop chance stored
 * on the mob. Whenever a drowned enters the world (spawned or loaded from disk)
 * holding a trident, its main-hand drop chance is raised to TRIDENT_DROP_CHANCE.
 * Vanilla rules still apply: only when killed by a player, and dropped tridents
 * get random durability damage. A higher chance that is already set (e.g. 100%
 * for a trident the drowned picked up) is never lowered.
 *
 * APIs verified in the 26.3 jar: Mob.setDropChance(EquipmentSlot, float),
 * Mob.getDropChances().byEquipment(EquipmentSlot), LivingEntity.getItemBySlot.
 * Wither skeleton skulls are loot-table loot instead: see LootTweaks.
 */
public final class MobDrops {
    private MobDrops() {
    }

    private static final float TRIDENT_DROP_CHANCE = 0.70f;

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity.getType() == EntityTypes.DROWNED
                    && entity instanceof Mob mob
                    && mob.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.TRIDENT)
                    && mob.getDropChances().byEquipment(EquipmentSlot.MAINHAND) < TRIDENT_DROP_CHANCE) {
                mob.setDropChance(EquipmentSlot.MAINHAND, TRIDENT_DROP_CHANCE);
            }
        });
    }
}
