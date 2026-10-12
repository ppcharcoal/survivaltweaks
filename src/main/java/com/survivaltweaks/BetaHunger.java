package com.survivaltweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;

/**
 * Beta 1.7-style hunger (server side; the HUD part is in client/BetaHud.java).
 *
 * How it works - no mixins, just one check per player per tick:
 *
 * The hidden vanilla food level is pinned at FROZEN_FOOD_LEVEL (17) with 0 saturation.
 * At 17, vanilla's own rules already give everything we want:
 *  - 17 > 6        -> sprinting is always allowed (infinite sprint)
 *  - 17 > 0        -> starvation can never happen
 *  - 17 < 18       -> vanilla natural regeneration never triggers
 *  - 17 < 20       -> you can always eat
 *
 * Detecting what was eaten: since saturation is reset to 0 every tick, any
 * saturation found at the end of a tick came from something eaten during that tick
 * (food item, cake slice, suspicious stew... anything that feeds you). Vanilla puts
 * exactly the food's saturation value there, so that is what we heal from:
 *
 *     heal (HP) = food saturation x HP_PER_SATURATION   (min. 1 HP)
 *
 * delivered as Regeneration II (1 HP every 25 ticks = 1.25 s). Eating more while
 * regenerating ADDS to the remaining time instead of resetting it.
 * Examples (HP_PER_SATURATION = 1): steak/porkchop 12.8 sat -> 13 HP (6.5 hearts,
 * ~16 s), bread 6 HP, cooked chicken 7 HP, apple 2 HP, golden carrot 14 HP.
 *
 * Known tiny edge case: if the vanilla exhaustion counter rolls over on the exact
 * tick you finish eating, that food heals 1 HP less. Harmless.
 */
public final class BetaHunger {
    private BetaHunger() {
    }

    /** Hidden food level everyone is kept at. Must stay between 7 and 17 (see above). */
    public static final int FROZEN_FOOD_LEVEL = 17;

    /** HP healed per point of food saturation. 1.0 = heal exactly the saturation value. */
    private static final float HP_PER_SATURATION = 1.0f;

    /** Regeneration level used: 0 = Regen I (1 HP / 50 ticks), 1 = Regen II (1 HP / 25 ticks). */
    private static final int REGEN_AMPLIFIER = 1;
    private static final int TICKS_PER_HP = 50 >> REGEN_AMPLIFIER;

    /** Safety cap so eating a whole stack can't queue hours of regeneration (5 min). */
    private static final int MAX_REGEN_TICKS = 20 * 60 * 5;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        FoodData food = player.getFoodData();
        float saturation = food.getSaturationLevel();

        if (saturation > 0.0f && player.isAlive()) {
            int hp = Math.max(1, Math.round(saturation * HP_PER_SATURATION));
            addHealing(player, hp);
        }

        if (food.getFoodLevel() != FROZEN_FOOD_LEVEL) {
            food.setFoodLevel(FROZEN_FOOD_LEVEL);
        }
        if (saturation != 0.0f) {
            food.setSaturation(0.0f);
        }
    }

    private static void addHealing(ServerPlayer player, int hp) {
        int ticks = hp * TICKS_PER_HP;

        MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
        if (current != null && current.getAmplifier() == REGEN_AMPLIFIER) {
            if (current.getDuration() < 0) {
                return; // infinite regeneration already (e.g. from a command)
            }
            ticks += current.getDuration();
        }

        ticks = Math.min(ticks, MAX_REGEN_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, REGEN_AMPLIFIER));
    }
}
