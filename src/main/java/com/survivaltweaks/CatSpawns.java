package com.survivaltweaks;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

/**
 * Wild cats. Vanilla cats only come from villages and swamp huts, which this mod
 * removes, so cats now spawn naturally (like other animals) in biomes where cats
 * and wildcats live in real life: temperate grassland and woodland, savanna,
 * swamps and taiga - never deserts, snow, jungles (ocelots), mountains or oceans.
 *
 * Biome list: data tag survivaltweaks:cats_spawn_in (editable, no Java).
 * Which breed spawns where: data/minecraft/cat_variant/*.json, GENERATED from the
 * vanilla jar by tools/verify_against_minecraft.py --fix (see CAT_VARIANT_RULES).
 *
 * Vanilla's own cat spawn rule (on grass, enough light) still applies, and untamed
 * cats despawn far from players like vanilla stray cats, so they keep turning up.
 * Uses Fabric BiomeModifications.addSpawn (verified in Fabric API 0.161.0+26.3).
 */
public final class CatSpawns {
    private CatSpawns() {
    }

    public static final TagKey<Biome> CATS_SPAWN_IN =
            TagKey.create(Registries.BIOME, SurvivalTweaks.id("cats_spawn_in"));

    private static final int WEIGHT = 4;     // sheep are 12, cows 8, rabbits 4
    private static final int MIN_GROUP = 1;
    private static final int MAX_GROUP = 2;

    public static void register() {
        BiomeModifications.addSpawn(BiomeSelectors.tag(CATS_SPAWN_IN),
                MobCategory.CREATURE, EntityTypes.CAT, WEIGHT, MIN_GROUP, MAX_GROUP);
    }
}
