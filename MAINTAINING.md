# Survival Tweaks – Maintenance & Update Guide

This file explains how every feature works, where it lives, and what to check when
updating to a new Minecraft version (26.4, 27.x, ...). Hand this file to whoever
(or whatever AI) does the next update, together with the project folder.

---

## 0. The compatibility checker (run this first on every update)

`tools/verify_against_minecraft.py` checks everything this mod relies on in
vanilla against a Minecraft client jar: mixin target methods, Java members,
advancement ids/parents, structure-set format, loot-table format (key by key),
recipe/loot/entity/gamerule/sprite ids. Needs only Python 3.

    python tools/verify_against_minecraft.py <path>/minecraft-<version>-client.jar
    python tools/verify_against_minecraft.py <jar> --fix   # re-copies vanilla-copy files

Jar location (Prism): `PrismLauncher/libraries/com/mojang/minecraft/<version>/`.
Fix every FAIL before building; read every WARN. On 26.3 it reports
124 OK / 0 WARN / 0 FAIL. Since 26.1 the jar is unobfuscated, so the tool (or any
class viewer) shows Mojang's real names - use it instead of guessing.

## 1. Design rules (keep following these!)

1. **Avoid mixins.** Everything uses official Fabric API events or plain data files,
   except where Fabric has no event: the anvil (3.13) and potion effects on all
   mobs/bosses (3.15). Those mixins are tiny, call plain Java classes, use no
   private fields, and their config `survivaltweaks.mixins.json` is
   `"required": false` - if an update breaks one, the game still starts and only
   that feature falls back to vanilla (the log shows a mixin error naming the
   class). Any new mixin must follow the same pattern. Search the Fabric API source
   for an event first (e.g. version tag `0.161.0+26.3` on GitHub).
2. **Verify, don't guess.** Before using a Minecraft/Fabric class or method, check it
   exists for that version: run the checker (section 0), read the vanilla jar, or
   read the Fabric API source and its test mods. Copy data-file formats from the
   jar's own `data/minecraft/...` files - never from memory or older datapacks.
   Version 1.0.0 had one failed build because `CriteriaTriggers` had moved in 26.3.
3. **Prefer data files** (JSON / mcfunction) for anything Minecraft supports as data.
   A broken recipe, tag, function or loot file usually only disables that one thing
   and logs an error. **EXCEPTION: advancements.** Since 26.x they load like
   registries - ONE advancement JSON that fails to parse stops every world from
   opening ("problems with a datapack"; creating a world hangs). The log shows
   `Errors in registry minecraft:advancement` and the file name. Always test that a
   world opens after touching any advancement file. (Versions 1.3.0 and 1.3.1 had exactly this
   bug - see 3.3.)
4. **One thing per file** where a rename could break it (see gamerules below).
5. **Never rename saved IDs** (section 4). Existing worlds depend on them.

---

## 2. Updating to a new Minecraft version – checklist

1. Go to https://fabricmc.net/develop/ and pick the new Minecraft version. Copy the
   new values into `gradle.properties`:
   - `minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`
   Update `gradle/wrapper/gradle-wrapper.properties` if the page/template says the
   Gradle version changed. Check the required Java version too.
2. In `src/main/resources/fabric.mod.json`, change `"minecraft": "~26.3"` to the new
   version (and `java` / `fabricloader` if required).
3. Bump `version` in `gradle.properties` (e.g. 1.2.0).
4. Run the checker (section 0) against the new version's jar and fix FAILs.
5. Run `gradlew.bat build`. Fix compile errors one by one (usually a class moved or
   was renamed; the error names the exact line).
6. Launch the game, then open `.minecraft/logs/latest.log` and search for
   `survivaltweaks` and for `ERROR`. Data files that failed to load show up there.
7. Go through the test checklist (section 6).
8. Read the Minecraft changelog for anything that touches: mob spawning, villagers/
   illagers, totems, beds/sleeping, gamerules, structures, advancements, item
   components. New mobs or structures in these areas may need adding (section 5).

---

## 3. Features – how each one works and what can break

### 3.1 Cave-only spawning + dragon progression — `SpawnRules.java`
- Uses Fabric `ServerEntityEvents.ALLOW_LOAD`, which runs right before ANY entity is
  added to a world. Returning `false` means the entity never exists.
- Before the dragon: entities in tag `survivaltweaks:cave_only` (creepers, spiders)
  are refused in the Overworld if they can see the sky.
- After the dragon: entities in tag `survivaltweaks:cave_only_after_dragon` are
  refused in the Overworld if they can see the sky OR are at y >= 63 (`SURFACE_Y`).
- Bosses (Wither, Ender Dragon) are never touched by any spawn rule: checked
  first in `allowLoad`, so the Wither can be built anywhere, any time.
- Only NEW mobs are checked. Mobs loaded from disk are left alone. Spawns from
  `/summon`, spawn eggs and dispensers are always allowed (`DELIBERATE_SPAWN_REASONS`,
  compared by name so a renamed enum can't break compiling).
- Dragon detection: `ServerLivingEntityEvents.AFTER_DEATH` on `EnderDragon`, plus a
  fallback every 5 seconds that checks whether an online player has the advancement
  `minecraft:end/kill_dragon` or `main:end/kill_dragon` (the Matcha Flavoured one).
- Lists of mobs: `data/survivaltweaks/tags/entity_type/*.json`. Entries that might
  not exist in every version use `{"id": ..., "required": false}` so the tag still
  loads if Mojang removes/renames that mob.
- Can break if: `ALLOW_LOAD` changes signature (compile error, easy fix), or a mob
  is renamed (tag error in log).

### 3.2 No villagers / illagers / witches / phantoms — `SpawnRules.java` + data
- Tag `survivaltweaks:never_spawn` is refused everywhere, for every reason,
  including entities saved in old chunks and ones placed by world generation.
- Structures: `data/minecraft/worldgen/structure_set/` overrides `villages`,
  `pillager_outposts`, `woodland_mansions`, `swamp_huts` with `"frequency": 0.0`.
  If Mojang changes the structure_set format, copy the NEW vanilla file and only set
  `frequency` to `0.0`. If they add a new villager-type structure, add its set here.
- Explorer maps to removed structures: the 26.3 campsite secret chest can contain a
  `woodland_mansion_map`. `data/minecraft/loot_table/chests/abandoned_camp_secret_chest.json`
  is GENERATED by the checker (`--fix`): an exact vanilla copy minus every entry
  that leads to a removed structure (`REMOVED_STRUCTURE_REFS`). The checker also
  scans ALL vanilla loot tables each update and FAILs if a new one appears - then
  add it to `MODIFIED_LOOT_TABLES` and run `--fix`. (Cartographer villager trades
  also sell such maps, but villagers never spawn.)
- Gamerules: `data/survivaltweaks/function/gamerules/*.mcfunction`, one rule per file
  (`spawn_patrols`, `spawn_wandering_traders`, `raids`, `spawn_phantoms`), all run on
  world load via `data/minecraft/tags/function/load.json` (entries are
  `required: false`). Minecraft rejects a whole function file if one line is
  invalid, which is why each rule has its own file. If a rule is renamed, the log
  shows which file failed; fix the name in that file.
- Insomnia/phantoms: phantoms are in `never_spawn` AND `spawn_phantoms` is off, so
  the insomnia mechanic has no effect. Phantom membranes come from End City chests
  instead (3.9).

### 3.3 Removed advancements — `data/minecraft/advancement/**`
- Each file overrides a vanilla advancement with only a Fabric load condition that
  is always false (`fabric:not` of `fabric:true`). Fabric then skips the file, so
  the advancement doesn't exist. Each file has a `__comment` explaining why.
- REMOVED (single goal that can't happen): What a Deal!, Star Trader, Zombie
  Doctor, Surge Protector, Very Very Frightening, Voluntary Exile, Hero of the
  Village, Who's the Pillager Now?, Two Birds One Arrow, Enchanter, You've Got a
  Friend in Me, Birthday Song.
- MODIFIED (kept, impossible parts dropped) - GENERATED from the vanilla jar by
  the checker, never hand-edited:
  - Monsters Hunted: no villager/illager/witch/phantom kills needed.
  - How Did We Get Here?: Hero of the Village and Raid Omen removed from the
    effect list (Bad Omen stays: ominous bottles drop from trial chamber vaults).
  - Smithing with Style: Vex trim (woodland mansions only) not needed.
  The rule: every criterion/effect mentioning an "impossible" id is dropped -
  impossible = everything in tag `never_spawn` + `IMPOSSIBLE_EXTRA` in the
  checker. Change those lists, then run the checker with `--fix`.
- The checker also scans EVERY other vanilla advancement and FAILs if one needs
  something impossible - this is how new unobtainable advancements get noticed
  after an update (or after removing more things).
- To restore one: delete its file. To remove another: copy any of these files to the
  matching path/name of the vanilla advancement.
- On updates: check new vanilla advancements that need villagers, illagers,
  witches, phantoms, allays, enchanting tables, mansions or outposts.
- Do NOT remove recipes this way — that crashed the game in an earlier version
  (other files reference recipes). Recipes are disabled by replacement instead (3.6).

### 3.4 Totem of Undying — `TotemTweaks.java` + recipe
- Fabric `ServerLivingEntityEvents.ALLOW_DEATH` (fires before vanilla's totem check).
- Totem in hand + normal death: the mod does nothing; vanilla saves you (this keeps
  the Postmortal advancement working without touching advancement code).
- Totem anywhere else in the inventory, or ANY void death: the mod uses the totem,
  applies vanilla effects, and for the void teleports you to your respawn point.
- `/kill` still kills you (damage that bypasses invulnerability), like vanilla.
- Recipe: `data/survivaltweaks/recipe/totem_of_undying.json`; recipe-book unlock when
  you pick up a nether star: `data/survivaltweaks/advancement/recipes/`.
- Likely break points on update: `findRespawnPositionAndUseSpawnBlock`,
  `TeleportTransition`, `MobEffects` names (compile errors).

### 3.5 Beds — `BedRework.java`
- Fabric `EntitySleepEvents.ALLOW_RESETTING_TIME` returns false, so the night is
  never skipped. A server tick loop heals sleeping players 1 HP (half a heart)
  every 100 ticks (`TICKS_PER_HEAL`, `HEAL_AMOUNT`).

### 3.6 Uncraftable enchanting table & fletching table — data
- `data/minecraft/recipe/enchanting_table.json` and `fletching_table.json`
  replace the
  vanilla recipes with "barrier -> barrier" (impossible in survival). The recipe IDs
  must keep existing, because vanilla advancements reference them.

### 3.7 Items — `ItemTweaks.java`
- Fabric `DefaultItemComponentEvents.MODIFY` changes item defaults (applies to old
  items in existing worlds too). The mod must be on the client as well.
- Potions/splash/lingering: `max_stack_size` = 16 (`POTION_STACK_SIZE`).
- Every `minecraft:netherite_*` item that has durability + elytra: `unbreakable`.
  New netherite gear in future versions is picked up automatically.
- Rotten flesh: `consumable` component replaced with vanilla's default food +
  Slowness I (30 s) and Nausea (15 s) at 80% chance, instead of Hunger
  (`ROTTEN_FLESH_*` constants). Built like vanilla's `Consumables.ROTTEN_FLESH`.
- Rotten flesh: gets the `compostable` component copied from `COMPOST_CHANCE_LIKE`
  (melon slice). If the log says that item has no compostable component, pick
  another vanilla compostable item.

### 3.8 Rotten flesh -> leather in the smoker — data
- `data/survivaltweaks/recipe/leather_from_smoking.json` (type `minecraft:smoking`,
  100 ticks), unlocked in the recipe book on picking up rotten flesh.

### 3.9 Bonus loot, themed books, mob drops — `LootTweaks.java` + data
- `LootTweaks.java` has ONE map: vanilla loot table -> our tables added to it
  (Fabric `LootTableEvents.MODIFY`, which only ADDS pools; vanilla loot is kept,
  so Mojang's future changes still come through). Each addition is wrapped in
  try/catch: a problem logs a `[Survival Tweaks]` warning, never breaks loading.
- Each structure gets TWO additions:
  - `loot_table/tiers/tier1_common ... tier5_legendary` - materials (iron, gold,
    emeralds, diamonds, golden apples, enchanted golden apples, netherite scrap).
    Tier per structure is listed at the top of `LootTweaks.java`; end cities = 5.
  - `loot_table/books/<theme>.json` - enchanted books matching the structure or
    biome, using `set_enchantments` with explicit levels (higher in later
    structures): underground, ocean, desert, jungle, cold, ruined_portal, trial,
    stronghold, nether, nether_treasure (fire aspect / flame / fire protection),
    ancient_city (swift sneak, rare mending), end (looting, mending).
    The checker verifies every enchantment exists and its level <= vanilla max.
- `loot_table/chests/end_city_phantom_membranes` - membranes (75%, 1-4).
- `loot_table/mobs/wither_skeleton_skull` - 70% skull, player kill only (pool
  condition `minecraft:killed_by_player`, same as vanilla's skull pool). The
  vanilla 2.5% roll still happens too. Table id from
  `EntityTypes.WITHER_SKELETON.getDefaultLootTable()`.
- Eyes of ender: vanilla again (craftable, no extra drops) since 1.7.0.
- Chances are made with weights (item weight vs `minecraft:empty` weight), not
  loot conditions - simpler and nothing to get wrong.
- 26.3 LOOT FORMAT: entries use `"modifier": {"type": "minecraft:set_count", ...}`
  (one object, or a list for several); pools have only `rolls` + `entries`.
  The OLD format (`"functions": [{"function": ...}]`, `bonus_rolls`) is SILENTLY
  IGNORED - no error, the item just drops without count/enchantment. Versions
  before 1.6.0 had this bug (membranes always 1, books unenchanted). The checker
  compares every key in our loot files with vanilla's and FAILs on unknown ones.
- LOOT TABLES LOAD LIKE REGISTRIES: one broken JSON can stop worlds from opening
  (like advancements, rule 3). Test a world opens after editing any of them.
- Vanilla table names (e.g. `minecraft:chests/nether_bridge`) can be renamed by
  Mojang; a renamed table just stops getting bonus loot (no crash). Check the
  names against the new version's loot tables when updating.

### 3.10 Beta hunger — `BetaHunger.java` (server)
- The hidden vanilla food level is pinned at 17 with 0 saturation, every tick.
  Vanilla rules at 17 give: infinite sprint (needs > 6), no starvation (needs 0),
  no natural regeneration (needs >= 18), can always eat (needs < 20).
- Eating is detected by saturation appearing (it is reset to 0 each tick), which
  works for every food source incl. cake and stews. Heal = saturation x
  `HP_PER_SATURATION` HP (min. 1), given as Regeneration II (`REGEN_AMPLIFIER`);
  eating again adds to the remaining time (capped at `MAX_REGEN_TICKS`).
- Only uses `FoodData` get/set food level + saturation and `MobEffects.REGENERATION`.
- If Mojang changes the sprint/starvation thresholds, re-check that
  `FROZEN_FOOD_LEVEL` (17) is still above the sprint limit and below the natural
  regeneration limit.

### 3.11 Beta HUD — `client/BetaHud.java` (client entrypoint)
- Fabric `HudElementRegistry` + `HudStatusBarHeightRegistry` (rendering API),
  modelled on Fabric's `HudStatusBarHeightsTest` testmod - when updating, compare
  with that file in the new Fabric API version first.
- The FOOD_BAR element draws the armor bar at the food position (right side,
  right-to-left); its height is 10 only with armor. The ARMOR_BAR (left) is empty
  with height 0. While riding a living mount, armor falls back to the vanilla
  left position (the mount's hearts use the right side). Air bubbles stack above.
- 26.x names: drawing class `GuiGraphicsExtractor` (was `GuiGraphics`), elements
  implement `extractRenderState`. Sprites used: `hud/armor_full`, `hud/armor_empty`
  (checker verifies both). Vanilla `hud/armor_half` is NOT used - it is filled on
  the left; a half icon here is the empty icon + the full icon clipped with
  `enableScissor` to its right part (`HALF_SPLIT`).
- The server-side hunger works without the client mod, but then players see a
  normal hunger bar stuck at 8.5 drumsticks.

### 3.12 Unstackable meat — `ItemTweaks.java`
- `UNSTACKABLE_MEAT` set (raw + cooked beef, porkchop, chicken, mutton, rabbit)
  gets `max_stack_size` 1. Fish and rotten flesh deliberately excluded.

### 3.13 Anvil: free repair & rename — `AnvilRules.java` + `mixin/AnvilMenuMixin.java`
- After vanilla's `AnvilMenu.createResult()` runs, `AnvilRules` compares the
  result's enchantments (and stored enchantments, for books) with the left input.
  Identical = repair and/or rename -> level cost set to 0 (`setData(0, 0)`; the
  anvil's only data slot is its cost) and the result keeps the left item's
  original `repair_cost`, so repairs/renames never raise the prior-work penalty.
  Anything that adds/upgrades an enchantment = vanilla cost and penalty.
- Vanilla refuses results that cost 0 levels, so the mixin also hooks
  `mayPickup(Player, boolean)` to allow free results.
- Slot numbers come from vanilla's own constants `AnvilMenu.INPUT_SLOT` /
  `RESULT_SLOT`. All hooked names verified against the 26.3 jar (checker).
- Known limit: vanilla still shows "Too Expensive!" when an item's existing
  penalty is already huge (from many enchant combinations), because vanilla
  empties the result before this mod sees it.
- Anvils still get damaged with use (vanilla 12% chance) - that's not a level cost.

### 3.14 No Unbreaking on unbreakable gear — `UnbreakingRules.java`
- Fabric `EnchantmentEvents.ALLOW_ENCHANTING` returns FALSE for Unbreaking on any
  item that has the `unbreakable` component. One rule covers the enchanting
  table, anvil books AND randomly enchanted loot (Fabric routes loot enchanting
  through it) - so netherite gear in bastions never rolls Unbreaking. Cost: one
  tiny check whenever an enchantment is considered (negligible).
- The anvil ALSO refuses it directly (`AnvilRules`): a result that is unbreakable
  and would GAIN Unbreaking is cleared. Needed because vanilla's anvil skips the
  enchant check for creative players (`hasInfiniteMaterials`).
- Every 20 ticks each online player's inventory is checked; unbreakable items
  lose Unbreaking (`EnchantmentHelper.updateEnchantments` with level 0 = remove,
  verified in the jar). This cleans diamond gear upgraded to netherite.
- Mending is NOT blocked; add `Enchantments.MENDING` to the same checks if wanted.

### 3.15 Potion effects & tipped arrows work on all mobs and bosses — `mixin/*Effect*`, `PotionTweaks.java`
- `LivingEntityEffectImmunityMixin`:
  1. `@WrapOperation` on the `canBeAffected` call inside
     `LivingEntity.addEffect(MobEffectInstance, Entity)` - all normal effects on
     every non-player mob, whatever its class says.
  2. `@ModifyReturnValue` on `LivingEntity.canBeAffected` - tag-based immunities
     (e.g. undead vs poison/regeneration) everywhere they're asked.
  The call inside `forceAddEffect` is deliberately NOT wrapped: Fabric API injects
  at that exact call.
- `EffectImmunityOverridesMixin`: classes with their OWN `canBeAffected` (26.3:
  WitherBoss, Spider, WitherSkeleton, Parched, AbstractNautilus) always answer yes.
  The checker lists any new/removed such classes.
- `BossEffectsMixin`: EnderDragon and WitherBoss override `addEffect` to refuse
  everything; replaced by `forceAddEffect` (vanilla's store-the-effect method).
- `PotionTweaks` + `ArrowEffectsAccessor`: arrows hit the dragon's body PARTS (not
  living), so vanilla never applies their effects to it. All part damage reaches
  the dragon via Fabric `AFTER_DAMAGE` with the arrow as direct source; we then
  call the arrow's own vanilla `doPostHurtEffects` on the dragon (vanilla 1/8
  duration). If the accessor ever fails to apply, this is skipped safely.
- Unchanged: undead are still healed by Instant Damage / hurt by Instant Health.
- All targets/members verified against the 26.3 jar (checker section "Mixin targets").

### 3.16 Drowned trident 70% — `MobDrops.java`
- A drowned's trident is an EQUIPMENT drop chance stored on the mob, not loot.
  Fabric `ServerEntityEvents.ENTITY_LOAD`: any drowned holding a trident gets
  main-hand drop chance 0.70 (`Mob.setDropChance`), never lowering a higher one.
  Vanilla rules still apply (player kill; dropped trident has random damage).

### 3.17 Partial keep inventory — `KeepInventory.java` + `mixin/PlayerDropEquipmentMixin.java`
- On death you keep worn armor (head/chest/legs/feet), the off-hand item and the
  hotbar. Main inventory and XP drop as usual. Curse of Vanishing items are not
  kept (vanilla destroys them).
- The mixin hooks the start of vanilla `Player.dropEquipment(ServerLevel)` (the
  method that drops a dead player's inventory) and takes the kept items out.
- They are stored in the player attachment `survivaltweaks:kept_items`
  (persistent + `copyOnDeath`), so they survive quitting before respawning, and
  Fabric `ServerPlayerEvents.AFTER_RESPAWN` puts them back in the same slots
  (inventory full -> added anywhere -> dropped at feet as a last resort).
- No duplication: in 26.3 the ONLY place a dead player's items drop is
  `Inventory.dropAll()` inside `Player.dropEquipment` (player loot table empty,
  no other drop overrides). The hook runs at the very start of that method, so
  kept items never exist as dropped items. After respawn the stored copy is
  removed from both the old and new player entity. Only acts on a player that
  `isDeadOrDying()`. The checker re-verifies these facts on every update.
- If the mixin fails to apply after an update, deaths drop everything (vanilla).
- Works with the `keepInventory` gamerule too (then everything is kept anyway).

### 3.18 Wild cats — `CatSpawns.java` + data
- Vanilla cats only spawn in villages and swamp huts (both removed), so cats spawn
  naturally via Fabric `BiomeModifications.addSpawn` (CREATURE, weight 4, groups
  1-2) in biome tag `survivaltweaks:cats_spawn_in` (temperate grass/woodland,
  savanna, swamp, taiga). Vanilla's own cat placement rule (grass, light) applies;
  untamed cats despawn far from players like vanilla strays.
- Breeds by biome: `data/minecraft/cat_variant/*.json` are GENERATED by the checker
  (`CAT_VARIANT_RULES`, `--fix`): savanna = Siamese/Persian; taiga = Ragdoll/White;
  swamp & dark forest = Black/All Black; cherry grove = Calico/White; everywhere
  else = Tabby, British Shorthair, Red, Jellie, Calico, White, Black (+ All Black
  at full moon, vanilla rule). Groups are biome tags in
  `data/survivaltweaks/tags/worldgen/biome/cat_variant/`.
- Makes "A Complete Catalogue" obtainable again (all 11 breeds exist somewhere).
- Cat variants load like registries: never hand-edit; change the rules, run --fix,
  test a world opens. The checker warns about new vanilla breeds.

---

## 4. Saved data – NEVER rename these

| What | Where | Used for |
|---|---|---|
| `<world>/data/survivaltweaks/world_state.properties`, key `dragon_defeated` | `WorldState.java` | Main dragon flag. Plain text, version independent. |
| Fabric attachment `survivaltweaks:dragon_defeated` (Boolean) | `WorldState.java` | Backup copy inside the world save (how 1.0.0 stored it). |
| Player attachment `survivaltweaks:kept_items` (slot -> item map, keys like `hotbar_3`, `equipment_HEAD`) | `KeepInventory.java` | Items kept on death until respawn. |
| Mod id `survivaltweaks` | everywhere | Namespace of all the above, the tags and the recipes. |

On world load the file and the attachment are combined (either one saying `true`
counts), and a 1.0.0 world is migrated into the file automatically. If a future
Fabric version ever drops attachments, the file alone is enough — the attachment
code can then be deleted. You can set `dragon_defeated=true` by hand (with the world
closed) to switch a world into the post-dragon phase.

---

## 4b. Expected log lines (harmless - don't chase these)

| Log line | Why |
|---|---|
| `Ignored advancement '...' in progress file ... it doesn't exist anymore?` | Old worlds remembering progress on a removed advancement. Appears once per old player file. |
| `Requested post effect does not exist: minecraft:end_of_frame` | Vanilla/launcher message, unrelated to this mod. |
| `Player ... standing on air - force-sending blocks below` | Vanilla message after teleporting. |

If you see a mixin error mentioning `AnvilMenuMixin` or `survivaltweaks.mixins.json`,
the anvil feature is switched off (vanilla anvil) - see 3.13. Everything else
still works.

Lines that DO mean a problem: anything containing `survivaltweaks` with ERROR or
WARN (the mod logs its own fallbacks with a `[Survival Tweaks]` prefix),
`Errors in registry`, `Failed to load function`, `Couldn't load tag`, or
`Failed to parse` mentioning a survivaltweaks file.

---

## 5. Quick reference: where to change things

| Want to... | Edit |
|---|---|
| Change which mobs are cave-only | `data/survivaltweaks/tags/entity_type/cave_only*.json` |
| Block another mob completely | `data/survivaltweaks/tags/entity_type/never_spawn.json` |
| Change surface height | `SURFACE_Y` in `SpawnRules.java` |
| Change heal speed | `BedRework.java` |
| Change potion stack size | `ItemTweaks.java` |
| Change food healing | `BetaHunger.java` (HP_PER_SATURATION, REGEN_AMPLIFIER) |
| Change which meats don't stack | `UNSTACKABLE_MEAT` in `ItemTweaks.java` |
| Remove/restore an advancement | `data/minecraft/advancement/...` |
| Change recipes | `data/survivaltweaks/recipe/` |
| Change membrane loot | `data/survivaltweaks/loot_table/chests/end_city_phantom_membranes.json` |
| Change bonus materials / themed books | `data/survivaltweaks/loot_table/tiers/`, `.../books/` |
| Change where cats spawn / which breed | `tags/worldgen/biome/cats_spawn_in.json`, `CAT_VARIANT_RULES` in the checker + `--fix` |
| Change skull / trident chance | `loot_table/mobs/wither_skeleton_skull.json`, `MobDrops.java` |
| Remove loot that leads to a removed structure | `MODIFIED_LOOT_TABLES` / `REMOVED_STRUCTURE_REFS` in the checker, run `--fix` |
| Make another advancement adjust itself | `MODIFIED_ADVANCEMENTS` / `IMPOSSIBLE_EXTRA` in the checker, run `--fix` |
| Change which structure gets which tier | the map in `LootTweaks.java` |

---

## 6. Test checklist after every update

- [ ] `gradlew.bat build` succeeds; game launches; no `survivaltweaks` errors in log.
- [ ] New world: no villages, outposts, mansions or swamp huts (`/locate` won't find them).
- [ ] No creepers/spiders on the surface at night; they do appear in caves.
- [ ] No villagers, illagers, witches, phantoms. Stay awake 3+ nights: no phantoms.
- [ ] `/gamerule spawn_patrols`, `raids`, `spawn_wandering_traders`, `spawn_phantoms` all `false`.
- [ ] Removed advancements don't show in the advancement screen.
- [ ] Kill the dragon: chat says "Evil has been banished from the surface.";
      `<world>/data/survivaltweaks/world_state.properties` contains `dragon_defeated=true`;
      after restarting, no zombies/skeletons spawn on the surface.
- [ ] Totem in inventory (not hand) saves you; totem saves you from the void and
      teleports you home; totem in hand still gives Postmortal.
- [ ] Sleeping heals and the night doesn't skip.
- [ ] Netherite gear and elytra show "Unbreakable"; potions stack to 16.
- [ ] Enchanting/fletching tables can't be crafted; totem recipe works.
- [ ] Rotten flesh smokes into leather and goes into a composter; eating it
      usually gives Slowness + Nausea (never Hunger).
- [ ] No hunger bar; armor bar shows on the right above the hotbar (left while riding
      a horse); air bubbles appear above it underwater. With an odd armor value
      (e.g. leather helmet only = 1) the half icon is filled on its RIGHT side.
- [ ] Sprinting never stops; health doesn't regenerate on its own; no starvation.
- [ ] Eating gives Regeneration II roughly as long as the food's saturation
      (steak about 16 s); eating twice extends it.
- [ ] Raw/cooked meat doesn't stack; fish and rotten flesh still do.
- [ ] Worlds open and new worlds generate (no "Errors in registry" in the log).
      No `Couldn't load advancements` line; "Hired Help" exists in the Adventure tab.
- [ ] Anvil: renaming and repairing (with materials or a second damaged item) show
      no level cost and work with 0 levels; doing it repeatedly never becomes more
      expensive. Applying an enchanted book still costs levels as in vanilla.
- [ ] Netherite gear/elytra: Unbreaking is never offered by the enchanting table and
      an Unbreaking book can't be applied in the anvil. Diamond gear with
      Unbreaking upgraded to netherite loses Unbreaking within a second.
- [ ] Splash poison on a zombie/spider damages it; Slowness/Weakness splash or
      tipped arrows affect the Wither; splash Slowness affects the Ender Dragon
      (check with /data get entity @e[type=ender_dragon,limit=1] active_effects
      or watch for effect particles); a Slowness tipped arrow on the dragon too.
- [ ] Eye of ender is craftable again (vanilla).
- [ ] `/loot give @s loot minecraft:chests/nether_bridge` (repeat): fire aspect /
      flame / fire protection books; `.../end_city_treasure`: looting / mending
      books + best materials. No `[Survival Tweaks] Could not add` in the log.
- [ ] Wither skeletons drop skulls ~70% of the time (player kill); drowned with a
      trident drop it ~70% of the time.
- [ ] A Wither can be built on the surface after the dragon is dead.
- [ ] Dragon test: `/summon` has no boss bar (vanilla - the bar belongs to the End
      fight). Hit it with a Slowness tipped arrow and run
      `/data get entity @e[type=ender_dragon,limit=1] active_effects`.
- [ ] Bonus loot books are ENCHANTED and stacks have varied counts (proves the
      26.3 loot format is read, see 3.9).
- [ ] Die with items everywhere: armor, off-hand and hotbar come back on
      respawn; main inventory drops. Also try quitting to the title screen
      before clicking Respawn, then rejoin and respawn - items still come back.
- [ ] Wild cats appear in plains/forests/savanna/taiga/swamp (fresh chunks):
      Siamese/Persian in savanna, Ragdoll/White in taiga, black cats in swamps.
      `/summon minecraft:cat` in a savanna gives Siamese or Persian. Log line
      "Applied N biome modifications" is no longer 0.
- [ ] End City chests sometimes contain phantom membranes (or test:
      `/loot give @s loot minecraft:chests/end_city_treasure` a few times).
