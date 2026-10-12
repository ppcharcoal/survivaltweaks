package com.survivaltweaks;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

/**
 * Port of the datapack's "only_undead_surface" / "safe_surface" mechanic, plus the
 * villager/illager/witch removal.
 *
 * Datapack behaviour being reproduced:
 *  - Before the dragon dies: creepers and spiders that would appear where they can
 *    see the sky are removed (so they only exist in caves).
 *  - After the dragon dies: every "mundane hostile" (zombies, skeletons, creepers,
 *    spiders, slimes, endermen...) is removed if it can see the sky OR is at y >= 63
 *    in the Overworld (same predicate as main:surface_spawn).
 *
 * Instead of teleporting mobs into the void a tick later (the datapack way), this
 * uses Fabric's ServerEntityEvents.ALLOW_LOAD, which runs right before an entity is
 * inserted into the world. Cancelled mobs never exist at all - no flicker, no lag,
 * and no mixin required.
 *
 * The "dragon has been killed" flag is stored by WorldState in a plain text file
 * inside the world folder, so future versions of the mod can always read it.
 */
public final class SpawnRules {
    private SpawnRules() {
    }

    /** Never allowed to exist: villagers, illagers, witches and their relatives. */
    public static final TagKey<EntityType<?>> NEVER_SPAWN = tag("never_spawn");
    /** Cave-only from the start (creepers, spiders). */
    public static final TagKey<EntityType<?>> CAVE_ONLY = tag("cave_only");
    /** Also cave-only once the dragon has been killed (the datapack's mundane_hostiles). */
    public static final TagKey<EntityType<?>> CAVE_ONLY_AFTER_DRAGON = tag("cave_only_after_dragon");

    /** Same height threshold the datapack's main:surface_spawn predicate uses. */
    private static final int SURFACE_Y = 63;

    /**
     * Spawns a player did on purpose (/summon, spawn egg, dispensed spawn egg) are
     * always allowed, so you can still test things. Compared by name so a renamed
     * enum constant can never cause a compile error.
     */
    private static final Set<String> DELIBERATE_SPAWN_REASONS = Set.of("COMMAND", "SPAWN_ITEM_USE", "DISPENSER");

    /**
     * Fallback for worlds where the dragon was killed before this mod was installed:
     * if any online player has one of these advancements, the flag gets set.
     * (main:end/kill_dragon is the Matcha Flavoured datapack's own version.)
     */
    private static final List<Identifier> DRAGON_ADVANCEMENTS = List.of(
            Identifier.parse("minecraft:end/kill_dragon"),
            Identifier.parse("main:end/kill_dragon")
    );

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, SurvivalTweaks.id(path));
    }

    public static void register() {
        ServerEntityEvents.ALLOW_LOAD.register(SpawnRules::allowLoad);

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level) {
                markDragonDefeated(level.getServer(), true);
            }
        });

        // Every 5 seconds, until the flag is set, check the advancement fallback.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 == 0 && !isDragonDefeated(server)) {
                checkAdvancementFallback(server);
            }
        });
    }

    private static boolean allowLoad(Entity entity, ServerLevel level, EntitySpawnReason reason, boolean loadedFromDisk) {
        // Bosses are never touched by any spawn rule (Wither may be built anywhere,
        // also on the surface after the dragon is dead).
        if (entity.getType() == EntityTypes.WITHER || entity.getType() == EntityTypes.ENDER_DRAGON) {
            return true;
        }

        Holder<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());

        // Villagers & co: blocked everywhere, for every reason - including ones saved
        // in chunks from before the mod was installed, and ones placed by worldgen
        // (e.g. the villager + zombie villager in igloo basements).
        if (type.is(NEVER_SPAWN)) {
            return deny(entity);
        }

        // The surface rules only judge NEW mobs at the moment they appear, exactly like
        // the datapack's one-time SpawnChecked tag. A creeper that wandered up to the
        // surface and got saved there is left alone when its chunk reloads.
        if (loadedFromDisk) {
            return true;
        }
        if (reason != null && DELIBERATE_SPAWN_REASONS.contains(reason.name())) {
            return true;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return true;
        }

        BlockPos pos = entity.blockPosition();
        boolean canSeeSky = level.canSeeSky(pos);

        if (canSeeSky && type.is(CAVE_ONLY)) {
            return deny(entity);
        }

        if ((canSeeSky || pos.getY() >= SURFACE_Y)
                && type.is(CAVE_ONLY_AFTER_DRAGON)
                && isDragonDefeated(level.getServer())) {
            return deny(entity);
        }

        return true;
    }

    /**
     * Detaches riders/vehicles before refusing the entity, so e.g. a blocked spider
     * jockey doesn't leave its skeleton riding a spider that doesn't exist.
     */
    private static boolean deny(Entity entity) {
        if (entity.isVehicle()) {
            entity.ejectPassengers();
        }
        if (entity.isPassenger()) {
            entity.stopRiding();
        }
        return false;
    }

    public static boolean isDragonDefeated(MinecraftServer server) {
        return WorldState.isDragonDefeated(server);
    }

    private static void markDragonDefeated(MinecraftServer server, boolean announce) {
        if (isDragonDefeated(server)) {
            return;
        }
        WorldState.setDragonDefeated(server);
        if (announce) {
            // Same message the datapack uses.
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("Evil has been banished from the surface.").withStyle(ChatFormatting.GRAY),
                    false
            );
        }
    }

    private static void checkAdvancementFallback(MinecraftServer server) {
        for (Identifier id : DRAGON_ADVANCEMENTS) {
            AdvancementHolder holder = server.getAdvancements().get(id);
            if (holder == null) {
                continue;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
                    markDragonDefeated(server, false);
                    return;
                }
            }
        }
    }
}
