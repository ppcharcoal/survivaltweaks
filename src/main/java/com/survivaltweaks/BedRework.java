package com.survivaltweaks;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Beds no longer skip the night. Instead, lying in one heals half a heart (1 HP)
 * every 5 seconds.
 *
 * The day-skip is disabled through Fabric's official ALLOW_RESETTING_TIME sleep
 * event (it makes vanilla's "has this player slept long enough?" check answer no),
 * instead of the old players_sleeping_percentage gamerule hack.
 *
 * Everything else about beds stays vanilla: you can still only get in at night or
 * during a thunderstorm, monsters nearby still stop you, and the bed still sets
 * your spawn point. Vanilla wakes you up when morning comes.
 */
public final class BedRework {
    private BedRework() {
    }

    private static final int TICKS_PER_HEAL = 100; // 5 seconds
    private static final float HEAL_AMOUNT = 1.0f;  // half a heart

    public static void register() {
        EntitySleepEvents.ALLOW_RESETTING_TIME.register(player -> false);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSleeping() && player.tickCount % TICKS_PER_HEAL == 0) {
                    player.heal(HEAL_AMOUNT);
                }
            }
        });
    }
}
