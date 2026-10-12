# Survival Tweaks (Fabric, Minecraft 26.3)

## Build
1. Install a Java 25 JDK (e.g. Temurin 25) and make sure `java -version` shows 25.
2. Open a terminal in this folder.
3. Run `gradlew.bat build` (Windows) or `./gradlew build` (macOS/Linux).
   The first build downloads Minecraft + Fabric and takes a few minutes.
4. The mod jar is `build/libs/survivaltweaks-1.9.0.jar`.
5. Put it in `.minecraft/mods` together with Fabric API. Install it on the client too.

If the build fails, send the error text back.

## Releases (GitHub)
Every push is built automatically (Actions tab → Build → the jar is under
"Artifacts"). To publish a new release, change `version=` in
`gradle.properties` and push to `main`: the Release workflow builds the mod,
creates the tag `v<version>` and a GitHub Release with the jar attached.
Pushes that don't change the version don't create a release.

## Features
A harder, more rewarding survival: monsters lurk in caves, villagers and
illagers are gone, Beta-style hunger and healing return, and loot gets better
the deeper you go.

The complete, grouped list of every change is in **FEATURES.md**.
See MAINTAINING.md for how everything works and how to update the mod.
Before updating to a new Minecraft version, run
`python tools/verify_against_minecraft.py <minecraft client jar>` (MAINTAINING.md section 0).

## Where to change things
- Which mobs are affected: `src/main/resources/data/survivaltweaks/tags/entity_type/`
- Spawn logic: `SpawnRules.java`
- Heal rate: `BedRework.java` (TICKS_PER_HEAL, HEAL_AMOUNT)
- Potion stack size: `ItemTweaks.java` (POTION_STACK_SIZE)

## Quick test checklist
- Game launches with no errors in the log.
- New world: no villages; no creepers/spiders on the surface at night.
- `/kill @e[type=minecraft:ender_dragon]` won't count; kill the dragon normally
  (or have the advancement) - chat says "Evil has been banished from the surface."
- Totem in inventory (not hand), jump into the void -> you get teleported home.
- Sleep at night: health goes up, night doesn't skip.
