# Survival Tweaks

A harder, more rewarding survival for **Minecraft 26.3** (Fabric).
Monsters lurk in caves, villagers and illagers are gone, Beta-style hunger and
healing return, and loot gets better the deeper you go.

[![Build](https://github.com/ppcharcoal/survivaltweaks/actions/workflows/build.yml/badge.svg)](https://github.com/ppcharcoal/survivaltweaks/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/ppcharcoal/survivaltweaks)](https://github.com/ppcharcoal/survivaltweaks/releases/latest)

## Highlights

- **Monsters belong underground.** Creepers and spiders spawn only where they
  can't see the sky. Defeat the Ender Dragon and the surface is safe for good:
  the common hostile mobs stop spawning in daylight-exposed spots and above sea
  level.
- **No villagers, illagers, witches or phantoms.** Villages, pillager outposts,
  woodland mansions and swamp huts don't generate. There are no patrols, raids,
  wandering traders or insomnia.
- **Beta-style hunger.** There's no hunger bar and no starving, and you can
  sprint forever. You don't regenerate on your own: food heals you instead,
  based on its saturation. Your armor bar takes the hunger bar's place.
- **Beds heal, not skip.** Sleeping no longer skips the night. Lying in bed
  heals you slowly.
- **Totem of Undying, upgraded.** It works from anywhere in your inventory and
  saves you from falling into the void. It has a new recipe built around a
  nether star.
- **Gear and items.**
  - Netherite gear and elytra are unbreakable.
  - The anvil repairs and renames items for free.
  - Potions stack to 16.
  - Potions and tipped arrows affect every mob, bosses included.
- **Better, themed loot.** Structures get extra loot on top of vanilla, tiered
  by progression. Enchanted books match the place you find them, like fire
  enchantments in the Nether or Mending in end cities.
- **Partial keep inventory.** When you die, you keep your armor, off-hand item
  and hotbar.
- **Smaller touches.**
  - Rotten flesh smokes into leather and can be composted.
  - Meat doesn't stack.
  - Wild cats live in fitting biomes.
  - Advancements that would be impossible have been adjusted or removed.

The complete list of every change is in **[FEATURES.md](FEATURES.md)**.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.19.5 or newer** for
   Minecraft **26.3**.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3.
3. Download `survivaltweaks-<version>.jar` from the
   [latest release](https://github.com/ppcharcoal/survivaltweaks/releases/latest).
4. Put both jars in your `.minecraft/mods` folder.

Requires **Java 25**, which the official launcher already provides.

**Multiplayer:** install the mod on the server **and** on every client. Item
changes such as stack sizes and the HUD need it on both sides.

### Existing worlds

The mod works on existing worlds:
- Villagers, illagers and witches already in the world are removed when their
  chunks load.
- Structures that were already generated stay; new chunks won't get them.
- If the dragon was already defeated in that world, the mod notices as soon
  as a player who has the *Free the End* advancement is online.

Back up your world before adding or removing any gameplay mod.

## Compatibility

- Built for Minecraft 26.3 only. Other versions are untested.
- Most features use Fabric API events and data packs, so they should play
  nicely with other mods.
- A few features use small optional mixins: anvil costs, potion effects on all
  mobs, and keep inventory. If another mod conflicts with one of them, only
  that feature falls back to vanilla. The game still starts.
- Mods that change hunger, the HUD, mob spawning or structure loot may overlap
  with this one.

## Building from source

```sh
git clone https://github.com/ppcharcoal/survivaltweaks
cd survivaltweaks
./gradlew build        # Windows: gradlew.bat build
```

You need a Java 25 JDK, such as [Temurin](https://adoptium.net/). The jar is
written to `build/libs/`. The first build downloads Minecraft and Fabric, so it
takes a few minutes.

## For contributors

- **[MAINTAINING.md](MAINTAINING.md)** explains how every feature works, where
  its code and data live, and how to update the mod for a new Minecraft
  version.
- Before updating, run
  `python tools/verify_against_minecraft.py <path to minecraft client jar>`.
  It checks every data file and feature against that version's game files.
- Common things to tweak:
  - Affected mobs: `src/main/resources/data/survivaltweaks/tags/entity_type/`
  - Spawn logic: `SpawnRules.java`
  - Bed heal rate: `BedRework.java`
  - Potion stack size: `ItemTweaks.java`
- **Releasing:** every push is built by GitHub Actions, and the jar appears
  under the run's *Artifacts*. To publish a release, bump `version=` in
  `gradle.properties` and push to `main`. The Release workflow then creates the
  `v<version>` tag and a GitHub Release with the jar attached.

Bug reports and suggestions are welcome in
[Issues](https://github.com/ppcharcoal/survivaltweaks/issues).

## License

[MIT](LICENSE) © 2026 ppcharcoal
