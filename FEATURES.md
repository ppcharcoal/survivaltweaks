# Survival Tweaks — Feature List

> A harder, more rewarding survival: monsters lurk in caves, villagers and
> illagers are gone, Beta-style hunger and healing return, and loot gets better
> the deeper you go.

Fabric mod for Minecraft 26.3 · requires Fabric API · install on **both client
and server** (singleplayer: just the client).

---

## 1. Monsters & the surface

**Cave-only monsters**
- Creepers and spiders only spawn where they can't see the sky — in caves,
  under cover, never on the open surface.
- Zombies, skeletons and the other undead still roam the surface at night.

**After the Ender Dragon dies**, the surface becomes safe:
- Zombies, husks, drowned, skeletons, strays, bogged, parched, creepers,
  spiders, slimes and endermen no longer spawn where they can see the sky or
  anywhere at y 63 and above (Overworld).
- Chat announces: *"Evil has been banished from the surface."*
- The world remembers this permanently (survives restarts and mod updates).

**Exceptions**
- Mobs you spawn on purpose (`/summon`, spawn eggs, dispensers) are always
  allowed.
- Bosses (Wither, Ender Dragon) are never affected by spawn rules — the Wither
  can be built anywhere, any time.
- Rules only apply to newly appearing mobs; existing mobs are left alone.

## 2. No villagers, illagers or witches

**Mobs that never spawn** (also removed from existing chunks):
- Villager, zombie villager, wandering trader, trader llama
- Pillager, vindicator, evoker, vex, illusioner, ravager
- Witch
- Phantom

**Structures that no longer generate**
- Villages, pillager outposts, woodland mansions, swamp huts

**No maps to removed structures**
- The woodland mansion explorer map is removed from campsite secret chests
  (new in 26.3); no loot anywhere leads to a removed structure.

**Mechanics turned off**
- Pillager patrols, wandering trader visits, raids
- Insomnia — phantoms never come, however long you stay awake

**Wild cats**
- With villages and swamp huts gone, cats now spawn naturally where cats and
  wildcats live in real life: plains, meadows, forests, birch and dark forests,
  cherry groves, savannas, swamps and taiga. Never in deserts, snow, jungles
  (ocelots live there), mountains or oceans.
- Breeds match the region:

| Biome | Breeds |
|---|---|
| Savanna | Siamese, Persian |
| Taiga & windswept forest | Ragdoll, White |
| Swamp & dark forest | Black, All Black |
| Cherry grove | Calico, White |
| Everywhere else | Tabby, British Shorthair, Red, Jellie, Calico, White, Black (All Black at full moon) |

- "A Complete Catalogue" is obtainable again.

## 3. Beta-style hunger & healing

**Hunger**
- No hunger bar — you never get hungry and never starve.
- Unlimited sprinting.
- No natural health regeneration.

**Food heals you**
- Eating gives Regeneration II for as long as it takes to heal the food's
  saturation in HP — e.g. steak heals about 6.5 hearts, bread 3 hearts,
  golden carrot 7 hearts.
- Eating while regenerating adds to the remaining time (up to 5 minutes).
- Works for every food, including cake and stews.

**HUD**
- The armor bar sits where the hunger bar used to be (right side), filling
  right-to-left like in Beta, with correctly mirrored half icons.
- Air bubbles appear above it underwater.
- While riding a horse or other mount, armor moves back to the left so the
  mount's hearts have room.

**Beds**
- Sleeping no longer skips the night.
- Lying in bed heals half a heart every 5 seconds.
- Beds still set your spawn point; vanilla sleeping rules otherwise apply.

## 4. Items & equipment

**Totem of Undying**
- Works from anywhere in your inventory, not just your hands.
- Saves you from the void: teleports you to your respawn point, with slow
  falling.
- Same effects as vanilla; `/kill` still kills; a totem in your hand still
  grants "Postmortal".
- New recipe: emerald / gold block / emerald, gold block / nether star /
  gold block, emerald / gold block / emerald.

**Partial keep inventory**
- When you die you keep everything you're wearing (armor), your off-hand item
  and your whole hotbar. Only your main inventory and XP drop.
- Items with Curse of Vanishing still disappear, as in vanilla.
- Kept items never drop, not even for a moment, so they can't be duplicated in
  multiplayer.
- Safe even if you quit to the title screen before respawning.

**Unbreakable gear**
- All netherite tools, weapons and armor are unbreakable.
- Elytra are unbreakable.
- Unbreaking can never be applied to netherite items or elytra — not with books
  in the anvil (not even in creative mode), not at an enchanting table, never in
  loot.
- Gear that already has it loses it, e.g. diamond gear upgraded to netherite.

**Stacking**
- Potions, splash potions and lingering potions stack to 16.
- Raw and cooked beef, porkchop, chicken, mutton and rabbit no longer stack.
  Fish and rotten flesh still stack.

## 5. Crafting, smelting & composting

- **Uncraftable:** enchanting table, fletching table.
- **Smoker:** rotten flesh → leather.
- **Composter:** rotten flesh can be composted (same chance as a melon slice).
- **Eating rotten flesh** gives Slowness (30 s) and Nausea (15 s) instead of
  Hunger, 80% of the time.
- New recipes unlock in the recipe book automatically: the totem when you pick
  up a nether star, leather when you pick up rotten flesh.

## 6. Anvil

- Repairing (with materials or a second item) and renaming cost **no levels** —
  only the materials.
- Repairs and renames no longer raise the item's hidden "prior work penalty",
  so they never get more expensive.
- Combining enchantments works exactly like vanilla.

## 7. Potions & tipped arrows

**Every mob can be affected** — vanilla immunities are removed:
- Undead can be poisoned (and regenerated).
- Spiders can be poisoned; wither skeletons can get Wither.
- The Wither and the Ender Dragon accept potion effects.

**Tipped arrows**
- Work on all of them, including the Ender Dragon (arrows hit its body parts,
  which vanilla never let effects through).

Instant Damage still heals undead and Instant Health still hurts them, as in
vanilla.

## 8. Loot

All structure loot is **added on top of vanilla** — nothing vanilla is taken
away.

### Material tiers (better further into progression)

| Tier | Structures | Bonus rolls | Can contain |
|---|---|---|---|
| 1 | Mineshaft, dungeon, shipwreck (supply/map), ocean ruins, ruined portal, igloo | 1–2 | iron, gold, emeralds, golden apple, diamond |
| 2 | Desert temple, jungle temple, buried treasure, shipwreck treasure, trial chamber chests | 2–3 | more of the above, enchanted golden apple |
| 3 | Nether fortress, bastion (normal chests), stronghold | 2–4 | larger stacks, more diamonds |
| 4 | Bastion treasure, ancient city | 3–4 | diamonds, emeralds, enchanted golden apples, netherite scrap |
| 5 | End city | 3–5 | the most diamonds and emeralds, best enchanted golden apple and netherite scrap odds |

### Themed enchanted books (matching the structure or biome)

| Theme | Where | Book chance per chest | Enchantments |
|---|---|---|---|
| Underground | Mineshaft, dungeon | 46% | Efficiency I–III, Fortune I–II, Unbreaking I–II, Feather Falling I, rare Silk Touch |
| Ocean | Shipwrecks, ocean ruins, buried treasure | 47% | Respiration, Aqua Affinity, Depth Strider, Luck of the Sea, Lure, Impaling, Loyalty, Riptide, Frost Walker, rare Channeling |
| Desert | Desert temple | 50% | Blast Protection II–III, Protection II, Smite II–III, Sharpness II, Bane of Arthropods III |
| Jungle | Jungle temple | 50% | Projectile Protection II–III, Power II–III, Punch, Quick Charge II, Piercing III, Multishot, rare Infinity |
| Cold | Igloo | 50% | Frost Walker I–II, Feather Falling II, Protection II |
| Ruined portal | Ruined portal | 41% | Fire Protection I–II, Flame, Fire Aspect I |
| Trial | Trial chamber chests | 41% | Density II–III, Breach I–II, Projectile Protection III, rare Wind Burst |
| Stronghold | Stronghold | 55% | Sharpness III–IV, Protection III–IV, Power III–IV, Unbreaking III, Feather Falling IV, rare Infinity |
| Nether | Fortress, bastion | 56% | Fire Aspect I–II, Flame, Fire Protection III–IV |
| Nether treasure | Bastion treasure room | 71% | Fire Aspect II, Flame, Fire Protection IV, Soul Speed III |
| Ancient city | Ancient city | 71% | Swift Sneak II–III, Protection IV, Sharpness V, Power V, rare Mending |
| End | End city | 50% | Looting II–III, Mending |

### Special loot & drops
- **Phantom membranes:** found in end city chests (75% chance, 1–4) — the
  replacement source now that phantoms are gone.
- **Wither skeleton skulls:** drop 70% of the time (player kills).
- **Drowned tridents:** a drowned holding a trident drops it 70% of the time
  (player kills).

## 9. Advancements

**Removed** (impossible without villagers, illagers, witches, phantoms, allays
or enchanting tables):
- What a Deal!, Star Trader, Zombie Doctor, Surge Protector, Very Very
  Frightening
- Voluntary Exile, Hero of the Village, Who's the Pillager Now?
- Two Birds, One Arrow, Enchanter
- You've Got a Friend in Me, Birthday Song

**Adjusted** (still there, impossible parts removed):
- **Monsters Hunted:** no villager, illager, witch or phantom kills needed.
- **How Did We Get Here?:** Hero of the Village and Raid Omen not needed.
- **Smithing with Style:** the Vex trim (woodland mansions only) not needed.
- **Hired Help:** moved under the Adventure tab, since its parent was removed.

Everything else stays obtainable — including the mace and ominous trials
(ominous bottles still come from trial chamber vaults).

## 10. Technical

- Built on Fabric API events and data files. The few small mixins (anvil cost,
  potion effects, keep inventory) are optional: if a future Minecraft update breaks one, only
  that feature switches back to vanilla; the game still starts.
- Items kept on death are saved with the player until respawn.
- The dragon-killed state is saved in plain text at
  `<world>/data/survivaltweaks/world_state.properties`, readable by any future
  version of the mod.
- Ships with `MAINTAINING.md` (how everything works and how to update it) and
  `tools/verify_against_minecraft.py`, which checks all of the mod against any
  Minecraft version's jar before updating.
