package com.survivaltweaks;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All extra loot, in ONE place.
 *
 * Every entry below says: "when vanilla loot table X is rolled, ALSO roll these of
 * our own tables". Our tables are normal data files in
 *   data/survivaltweaks/loot_table/...
 * so chances, items and enchantments are changed there, without Java. Vanilla loot
 * is never replaced (Fabric LootTableEvents.MODIFY only adds pools), so Mojang's
 * future loot changes still come through.
 *
 * Two layers per structure:
 *  1. a material TIER (gets better further into progression):
 *     tier1_common     mineshaft, dungeon, shipwreck supply/map, ocean ruins, ruined portal, igloo
 *     tier2_uncommon   desert & jungle temple, buried treasure, shipwreck treasure, trial chamber chests
 *     tier3_rare       nether fortress, bastion (normal chests), stronghold
 *     tier4_epic       bastion treasure, ancient city
 *     tier5_legendary  end city
 *  2. a THEMED enchanted book table (books/...) matching the structure/biome:
 *     underground (efficiency, fortune...), ocean (respiration, riptide...), desert
 *     (blast protection, smite...), jungle (projectile protection, bow/crossbow...),
 *     cold (frost walker...), ruined_portal / nether / nether_treasure (fire aspect,
 *     flame, fire protection), trial (density, breach, wind burst), stronghold
 *     (classic high-level), ancient_city (swift sneak, rare mending), end (looting,
 *     mending).
 *
 * Mobs: wither skeleton skull 70% (player kill, like vanilla).
 * (Drowned trident 70% is an equipment drop chance, see MobDrops.java.)
 *
 * If a vanilla table is ever renamed, its entry simply stops matching (no crash);
 * tools/verify_against_minecraft.py reports missing names.
 */
public final class LootTweaks {
    private LootTweaks() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SurvivalTweaks.MOD_ID);

    private static final String T1 = "tiers/tier1_common";
    private static final String T2 = "tiers/tier2_uncommon";
    private static final String T3 = "tiers/tier3_rare";
    private static final String T4 = "tiers/tier4_epic";
    private static final String T5 = "tiers/tier5_legendary";
    private static final String MEMBRANES = "chests/end_city_phantom_membranes";

    /** vanilla loot table -> our tables added to it */
    private static final Map<ResourceKey<LootTable>, List<ResourceKey<LootTable>>> BONUS = new HashMap<>();

    static {
        // Tier 1
        add("minecraft:chests/abandoned_mineshaft", T1, "books/underground");
        add("minecraft:chests/simple_dungeon", T1, "books/underground");
        add("minecraft:chests/shipwreck_supply", T1, "books/ocean");
        add("minecraft:chests/shipwreck_map", T1, "books/ocean");
        add("minecraft:chests/underwater_ruin_small", T1, "books/ocean");
        add("minecraft:chests/underwater_ruin_big", T1, "books/ocean");
        add("minecraft:chests/ruined_portal", T1, "books/ruined_portal");
        add("minecraft:chests/igloo_chest", T1, "books/cold");
        // Tier 2
        add("minecraft:chests/desert_pyramid", T2, "books/desert");
        add("minecraft:chests/jungle_temple", T2, "books/jungle");
        add("minecraft:chests/buried_treasure", T2, "books/ocean");
        add("minecraft:chests/shipwreck_treasure", T2, "books/ocean");
        add("minecraft:chests/trial_chambers/supply", T2, "books/trial");
        add("minecraft:chests/trial_chambers/corridor", T2, "books/trial");
        add("minecraft:chests/trial_chambers/entrance", T2, "books/trial");
        add("minecraft:chests/trial_chambers/intersection", T2, "books/trial");
        add("minecraft:chests/trial_chambers/intersection_barrel", T2, "books/trial");
        // Tier 3
        add("minecraft:chests/nether_bridge", T3, "books/nether");
        add("minecraft:chests/bastion_other", T3, "books/nether");
        add("minecraft:chests/bastion_bridge", T3, "books/nether");
        add("minecraft:chests/bastion_hoglin_stable", T3, "books/nether");
        add("minecraft:chests/stronghold_corridor", T3, "books/stronghold");
        add("minecraft:chests/stronghold_crossing", T3, "books/stronghold");
        add("minecraft:chests/stronghold_library", T3, "books/stronghold");
        // Tier 4
        add("minecraft:chests/bastion_treasure", T4, "books/nether_treasure");
        add("minecraft:chests/ancient_city", T4, "books/ancient_city");
        add("minecraft:chests/ancient_city_ice_box", T4, "books/ancient_city");
        // Tier 5
        add("minecraft:chests/end_city_treasure", T5, "books/end", MEMBRANES);
        // Mobs (EntityTypes gives the correct table id even if Mojang renames it)
        EntityTypes.WITHER_SKELETON.getDefaultLootTable()
                .ifPresent(key -> BONUS.put(key, List.of(own("mobs/wither_skeleton_skull"))));
    }

    private static void add(String vanillaTable, String... ownTables) {
        List<ResourceKey<LootTable>> list = new java.util.ArrayList<>();
        for (String own : ownTables) {
            list.add(own(own));
        }
        BONUS.put(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(vanillaTable)), List.copyOf(list));
    }

    private static ResourceKey<LootTable> own(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, SurvivalTweaks.id(path));
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, provider) -> {
            List<ResourceKey<LootTable>> extras = BONUS.get(key);
            if (extras == null) {
                return;
            }
            for (ResourceKey<LootTable> extra : extras) {
                try {
                    tableBuilder.withPool(LootPool.lootPool()
                            .add(NestedLootTable.lootTableReference(provider.getOrThrow(extra))));
                } catch (RuntimeException e) {
                    // Never break world loading over bonus loot.
                    LOGGER.warn("[Survival Tweaks] Could not add {} to {}; skipped.", extra, key, e);
                }
            }
        });
    }
}
