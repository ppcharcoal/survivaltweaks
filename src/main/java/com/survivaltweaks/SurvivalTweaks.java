package com.survivaltweaks;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * Survival Tweaks.
 *
 * Most features are built on official Fabric API events (checked against the
 * fabric-api 0.161.0+26.3 source) or on plain data files. The few mixins
 * (anvil costs, potion effects on all mobs, keep inventory - see the mixin
 * package) are in a non-required mixin config, so if one fails to apply after
 * a Minecraft update only that feature falls back to vanilla.
 *
 * Data-only features (see src/main/resources/data):
 *  - totem recipe:                 data/survivaltweaks/recipe/totem_of_undying.json
 *  - enchanting/fletching table:   data/minecraft/recipe/*.json (made uncraftable)
 *  - villages/outposts/mansions/huts: data/minecraft/worldgen/structure_set/*.json
 *  - patrols/traders/raids/phantoms off: data/survivaltweaks/function/gamerules/*.mcfunction
 *  - removed advancements:         data/minecraft/advancement/**
 *  - rotten flesh -> leather:      data/survivaltweaks/recipe/leather_from_smoking.json
 *  - phantom membrane End City loot: data/survivaltweaks/loot_table/chests/end_city_phantom_membranes.json
 *
 * Client-only (HUD): client/BetaHud.java (entrypoint "client" in fabric.mod.json)
 *
 * Full maintenance guide: MAINTAINING.md in the project folder.
 */
public class SurvivalTweaks implements ModInitializer {
    public static final String MOD_ID = "survivaltweaks";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        WorldState.register();
        SpawnRules.register();
        TotemTweaks.register();
        BedRework.register();
        ItemTweaks.register();
        LootTweaks.register();
        BetaHunger.register();
        UnbreakingRules.register();
        PotionTweaks.register();
        MobDrops.register();
        KeepInventory.register();
        CatSpawns.register();
    }
}
