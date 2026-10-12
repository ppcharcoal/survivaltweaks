#!/usr/bin/env python3
"""
Survival Tweaks - compatibility checker.

Checks everything this mod relies on in vanilla Minecraft against a client jar,
BEFORE you build/test an update. Needs only Python 3 (no extra packages).

Usage (from the project folder):
    python tools/verify_against_minecraft.py path/to/minecraft-<version>-client.jar

Where to find the jar (Prism Launcher):
    PrismLauncher/libraries/com/mojang/minecraft/<version>/minecraft-<version>-client.jar

Output: OK / WARN / FAIL lines and a summary. FAIL = something the mod uses no
longer exists or changed shape - fix it before release (MAINTAINING.md explains
each feature). WARN = worth a look. With --fix, files that are copies of vanilla
(structure sets, Hired Help advancement) are regenerated from the jar.

Works on unobfuscated jars (Minecraft 26.1 and later).
"""
import json
import os
import re
import struct
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
MOD = os.path.join(os.path.dirname(HERE), "src", "main")
MOD_DATA = os.path.join(MOD, "resources", "data")

results = {"OK": 0, "WARN": 0, "FAIL": 0}


def report(level, msg):
    results[level] += 1
    print(f"[{level}] {msg}")


# ---------------------------------------------------------------- class reader
def parse_class(data):
    """Minimal Java class-file reader: methods, fields, called methods, strings."""
    p = 8
    n = struct.unpack(">H", data[p:p + 2])[0]
    p += 2
    cp = [None] * n
    i = 1
    while i < n:
        t = data[p]
        p += 1
        if t == 1:
            ln = struct.unpack(">H", data[p:p + 2])[0]
            cp[i] = ("utf", data[p + 2:p + 2 + ln].decode("utf8", "replace"))
            p += 2 + ln
        elif t in (3, 4):
            p += 4
        elif t in (5, 6):
            p += 8
            i += 1
        elif t == 7:
            cp[i] = ("class", struct.unpack(">H", data[p:p + 2])[0])
            p += 2
        elif t == 8:
            cp[i] = ("str", struct.unpack(">H", data[p:p + 2])[0])
            p += 2
        elif t in (9, 10, 11):
            a, b = struct.unpack(">HH", data[p:p + 4])
            cp[i] = ("ref", a, b)
            p += 4
        elif t == 12:
            a, b = struct.unpack(">HH", data[p:p + 4])
            cp[i] = ("nat", a, b)
            p += 4
        elif t == 15:
            p += 3
        elif t == 16:
            p += 2
        elif t in (17, 18):
            p += 4
        elif t in (19, 20):
            p += 2
        else:
            raise ValueError(f"bad constant tag {t}")
        i += 1

    def utf(k):
        return cp[k][1]

    def ref(k):
        e = cp[k]
        nat = cp[e[2]]
        return utf(cp[e[1]][1]), utf(nat[1]), utf(nat[2])

    p += 2
    this_idx, super_idx = struct.unpack(">HH", data[p:p + 4])
    p += 4
    ic = struct.unpack(">H", data[p:p + 2])[0]
    p += 2 + 2 * ic

    def members(p):
        out = []
        c = struct.unpack(">H", data[p:p + 2])[0]
        p += 2
        for _ in range(c):
            acc, ni, di, ac = struct.unpack(">HHHH", data[p:p + 8])
            p += 8
            code = None
            for _ in range(ac):
                an, al = struct.unpack(">HI", data[p:p + 6])
                p += 6
                if utf(an) == "Code":
                    clen = struct.unpack(">I", data[p + 4:p + 8])[0]
                    code = data[p + 8:p + 8 + clen]
                p += al
            out.append((acc, utf(ni), utf(di), code))
        return p, out

    p, fields = members(p)
    p, methods = members(p)
    calls = {}
    for acc, name, desc, code in methods:
        found = []
        if code:
            for j in range(len(code) - 2):
                if code[j] in (0xB6, 0xB7, 0xB8, 0xB9):
                    k = struct.unpack(">H", code[j + 1:j + 3])[0]
                    if 0 < k < len(cp) and cp[k] and cp[k][0] == "ref":
                        try:
                            found.append(ref(k))
                        except Exception:
                            pass
        calls[(name, desc)] = found
    return {
        "name": utf(cp[this_idx][1]),
        "super": utf(cp[super_idx][1]) if super_idx else None,
        "methods": {(m[1], m[2]): m[0] for m in methods},
        "fields": {f[1]: (f[0], f[2]) for f in fields},
        "calls": calls,
        "strings": {utf(e[1]) for e in cp if e and e[0] == "str"},
    }


class Jar:
    def __init__(self, path):
        self.zip = zipfile.ZipFile(path)
        self.names = set(self.zip.namelist())
        self._cache = {}

    def has(self, name):
        return name in self.names

    def json(self, name):
        return json.loads(self.zip.read(name))

    def cls(self, dotted):
        if dotted not in self._cache:
            path = dotted.replace(".", "/") + ".class"
            self._cache[dotted] = parse_class(self.zip.read(path)) if path in self.names else None
        return self._cache[dotted]


MEI = "Lnet/minecraft/world/effect/MobEffectInstance;"
ENT = "Lnet/minecraft/world/entity/Entity;"


# ---------------------------------------------------------------- checks
def check_method(jar, cls, name, desc, why):
    c = jar.cls(cls)
    if c is None:
        report("FAIL", f"class {cls} not found ({why})")
    elif (name, desc) not in c["methods"]:
        report("FAIL", f"{cls}.{name}{desc} not found ({why})")
    else:
        report("OK", f"{cls.split('.')[-1]}.{name} ({why})")


def check_mixins(jar):
    print("\n== Mixin targets and Java members ==")
    check_method(jar, "net.minecraft.world.inventory.AnvilMenu", "createResult", "()V", "anvil mixin")
    check_method(jar, "net.minecraft.world.inventory.AnvilMenu", "mayPickup",
                 "(Lnet/minecraft/world/entity/player/Player;Z)Z", "anvil mixin")
    anvil = jar.cls("net.minecraft.world.inventory.AnvilMenu")
    if anvil:
        for f in ("INPUT_SLOT", "RESULT_SLOT"):
            report("OK" if f in anvil["fields"] else "FAIL", f"AnvilMenu.{f} constant (AnvilRules)")
        slots = sum(1 for v in anvil["calls"].values() for c in v if c[1] == "addDataSlot")
        report("OK" if slots == 1 else "FAIL", f"AnvilMenu has {slots} data slot(s); AnvilRules assumes exactly 1 (the cost)")

    le = "net.minecraft.world.entity.LivingEntity"
    add = ("addEffect", f"({MEI}{ENT})Z")
    check_method(jar, le, *add, "effect immunity mixin")
    check_method(jar, le, "canBeAffected", f"({MEI})Z", "effect immunity mixin")
    check_method(jar, le, "forceAddEffect", f"({MEI}{ENT})V", "boss effect mixin")
    c = jar.cls(le)
    if c and add in c["calls"]:
        ok = any(x[1] == "canBeAffected" for x in c["calls"][add])
        report("OK" if ok else "FAIL", "LivingEntity.addEffect still calls canBeAffected (immunity mixin hooks that call)")
    for boss in ("net.minecraft.world.entity.boss.enderdragon.EnderDragon",
                 "net.minecraft.world.entity.boss.wither.WitherBoss"):
        b = jar.cls(boss)
        if b is None:
            report("FAIL", f"{boss} not found - update BossEffectsMixin targets")
        elif add not in b["methods"]:
            report("WARN", f"{boss.split('.')[-1]} no longer overrides addEffect - BossEffectsMixin "
                           f"no longer needed for it (it may already accept effects)")
        else:
            report("OK", f"{boss.split('.')[-1]}.addEffect override (BossEffectsMixin)")
    check_method(jar, "net.minecraft.world.entity.projectile.arrow.Arrow", "doPostHurtEffects",
                 "(Lnet/minecraft/world/entity/LivingEntity;)V", "ArrowEffectsAccessor (dragon tipped arrows)")

    # MobDrops / SpawnRules / LootTweaks
    check_method(jar, "net.minecraft.world.entity.Mob", "setDropChance",
                 "(Lnet/minecraft/world/entity/EquipmentSlot;F)V", "MobDrops (drowned trident)")
    check_method(jar, "net.minecraft.world.entity.Mob", "getDropChances",
                 "()Lnet/minecraft/world/entity/DropChances;", "MobDrops (drowned trident)")
    check_method(jar, le, "getItemBySlot",
                 "(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;", "MobDrops")
    et = jar.cls("net.minecraft.world.entity.EntityTypes")
    for f in ("WITHER", "ENDER_DRAGON", "DROWNED", "WITHER_SKELETON"):
        report("OK" if et and f in et["fields"] else "FAIL", f"EntityTypes.{f} (spawn exception / drops)")

    # KeepInventory (partial keep inventory)
    check_method(jar, "net.minecraft.world.entity.player.Player", "dropEquipment",
                 "(Lnet/minecraft/server/level/ServerLevel;)V", "PlayerDropEquipmentMixin")
    inv = "net.minecraft.world.entity.player.Inventory"
    check_method(jar, inv, "getSelectionSize", "()I", "KeepInventory (hotbar size)")
    check_method(jar, inv, "add", "(Lnet/minecraft/world/item/ItemStack;)Z", "KeepInventory")
    check_method(jar, le, "setItemSlot",
                 "(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", "KeepInventory")
    check_method(jar, "net.minecraft.world.entity.Entity", "spawnAtLocation",
                 "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;",
                 "KeepInventory (full inventory fallback)")
    check_method(jar, "net.minecraft.world.item.enchantment.EnchantmentHelper", "has",
                 "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/component/DataComponentType;)Z", "KeepInventory (vanishing)")
    if jar.has("data/minecraft/enchantment/vanishing_curse.json"):
        eff = jar.json("data/minecraft/enchantment/vanishing_curse.json").get("effects", {})
        report("OK" if "minecraft:prevent_equipment_drop" in eff else "FAIL",
               "Curse of Vanishing still uses prevent_equipment_drop (KeepInventory skips those items)")

    check_method(jar, le, "isDeadOrDying", "()Z", "KeepInventory (death guard)")
    check_method(jar, "net.minecraft.world.item.enchantment.ItemEnchantments", "keySet", "()Ljava/util/Set;",
                 "AnvilRules (no Unbreaking on unbreakable gear)")
    check_method(jar, "net.minecraft.world.inventory.Slot", "set", "(Lnet/minecraft/world/item/ItemStack;)V",
                 "AnvilRules (clear result)")
    # The only place a dead player's items may drop must still be Inventory.dropAll in Player.dropEquipment
    pl = jar.cls("net.minecraft.world.entity.player.Player")
    if pl:
        overrides = [n for (n, d) in pl["methods"] if n in ("dropCustomDeathLoot", "dropFromLootTable", "dropAllDeathLoot")]
        report("OK" if not overrides else "FAIL",
               "Player drops items only via dropEquipment (no-dupe guarantee of KeepInventory)"
               + (f" - NEW overrides: {overrides}" if overrides else ""))
    if jar.has("data/minecraft/loot_table/entities/player.json"):
        report("OK" if not jar.json("data/minecraft/loot_table/entities/player.json").get("pools") else "FAIL",
               "vanilla player loot table is empty (KeepInventory)")

    # ItemTweaks: rotten flesh consumable (Slowness + Nausea)
    check_method(jar, "net.minecraft.world.item.component.Consumables", "defaultFood",
                 "()Lnet/minecraft/world/item/component/Consumable$Builder;", "ItemTweaks (rotten flesh)")
    check_method(jar, "net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect", "<init>",
                 "(Ljava/util/List;F)V", "ItemTweaks (rotten flesh effects)")
    check_method(jar, "net.minecraft.world.item.component.Consumable$Builder", "onConsume",
                 "(Lnet/minecraft/world/item/consume_effects/ConsumeEffect;)Lnet/minecraft/world/item/component/Consumable$Builder;",
                 "ItemTweaks (rotten flesh)")
    mob_effects = jar.cls("net.minecraft.world.effect.MobEffects")
    for f in ("SLOWNESS", "NAUSEA"):
        report("OK" if mob_effects and f in mob_effects["fields"] else "FAIL", f"MobEffects.{f} (rotten flesh)")

    # CatSpawns
    mc = jar.cls("net.minecraft.world.entity.MobCategory")
    report("OK" if mc and "CREATURE" in mc["fields"] else "FAIL", "MobCategory.CREATURE (CatSpawns)")
    report("OK" if et and "CAT" in et["fields"] else "FAIL", "EntityTypes.CAT (CatSpawns)")
    sp = jar.zip.read("net/minecraft/world/entity/SpawnPlacements.class")
    report("OK" if b"CAT" in sp else "WARN", "cats still have a vanilla spawn placement rule (CatSpawns)")

    # Every class with its own canBeAffected must be listed in EffectImmunityOverridesMixin
    mixin_src = open(os.path.join(MOD, "java", "com", "survivaltweaks", "mixin",
                                  "EffectImmunityOverridesMixin.java")).read()
    listed = set(re.findall(r'"(net\.minecraft\.[\w.]+)"', mixin_src))
    overriding = set()
    for n in jar.names:
        if n.startswith("net/minecraft/world/entity/") and n.endswith(".class"):
            dotted = n[:-6].replace("/", ".")
            if dotted == le:
                continue
            k = jar.cls(dotted)
            if k and ("canBeAffected", f"({MEI})Z") in k["methods"]:
                overriding.add(dotted)
    for cls in sorted(overriding - listed):
        report("WARN", f"{cls} has its own canBeAffected - add it to EffectImmunityOverridesMixin")
    for cls in sorted(listed - overriding):
        report("WARN", f"{cls} listed in EffectImmunityOverridesMixin but no longer overrides canBeAffected - remove it")
    if overriding == listed:
        report("OK", f"EffectImmunityOverridesMixin lists all {len(listed)} immunity overrides")


# Advancements kept but with impossible parts removed. They are GENERATED from the
# jar: every criterion mentioning an impossible thing is dropped (and removed from
# "requirements"), and impossible effects are dropped from effect lists.
MODIFIED_ADVANCEMENTS = [
    "adventure/kill_all_mobs",                          # Monsters Hunted
    "nether/all_effects",                               # How Did We Get Here?
    "adventure/trim_with_all_exclusive_armor_patterns", # Smithing with Style
]
# Things that can't happen with this mod (never_spawn mobs are added automatically).
IMPOSSIBLE_EXTRA = [
    "minecraft:hero_of_the_village",   # no villages/raids
    "minecraft:raid_omen",             # no raids
    "minecraft:vex_armor_trim_smithing_template",                 # woodland mansions only (item)
    "minecraft:vex_armor_trim_smithing_template_smithing_trim",   # ...and its trim recipe
]


def impossible_ids():
    tag = json.load(open(os.path.join(MOD_DATA, "survivaltweaks", "tags", "entity_type", "never_spawn.json")))
    ids = [(v["id"] if isinstance(v, dict) else v) for v in tag["values"]]
    return ids + IMPOSSIBLE_EXTRA


def mentions(obj, ids):
    text = json.dumps(obj)
    return any(re.search(re.escape(i) + r'(?![a-z_])', text) for i in ids)


def build_modified_advancement(vanilla, ids):
    adv = json.loads(json.dumps(vanilla))

    def strip_effects(o):
        if isinstance(o, dict):
            if "effects" in o and isinstance(o["effects"], dict):
                for k in [k for k in o["effects"] if k in ids]:
                    del o["effects"][k]
            for v in o.values():
                strip_effects(v)
        elif isinstance(o, list):
            for v in o:
                strip_effects(v)
    # 1) remove impossible entries from effect lists (e.g. How Did We Get Here?)
    strip_effects(adv["criteria"])
    # 2) drop whole criteria that still need something impossible
    dropped = {name for name, crit in adv["criteria"].items() if mentions(crit, ids)}
    for name in dropped:
        del adv["criteria"][name]
    adv["requirements"] = [[r for r in group if r not in dropped] for group in adv["requirements"]]
    adv["requirements"] = [g for g in adv["requirements"] if g]
    return adv


def check_modified_advancements(jar, fix):
    print("\n== Modified advancements (generated from the jar) ==")
    ids = impossible_ids()
    for rel in MODIFIED_ADVANCEMENTS:
        van = f"data/minecraft/advancement/{rel}.json"
        ours = os.path.join(MOD_DATA, "minecraft", "advancement", rel + ".json")
        if not jar.has(van):
            report("WARN", f"{rel} no longer exists in vanilla - delete our copy")
            continue
        expected = build_modified_advancement(jar.json(van), ids)
        if not expected["criteria"]:
            report("FAIL", f"{rel}: nothing left after removing impossible parts - remove it instead")
            continue
        current = json.load(open(ours)) if os.path.exists(ours) else None
        if current == expected:
            report("OK", f"{rel} matches vanilla minus impossible parts")
        elif fix:
            os.makedirs(os.path.dirname(ours), exist_ok=True)
            json.dump(expected, open(ours, "w"), indent=2)
            report("WARN", f"{rel} regenerated from vanilla (--fix)")
        else:
            report("FAIL", f"{rel} out of date - run with --fix to regenerate")


# Vanilla loot tables kept but with entries removed that lead to structures this mod
# removes (explorer maps). GENERATED from the jar with --fix.
MODIFIED_LOOT_TABLES = [
    "chests/abandoned_camp_secret_chest",   # campsite secret chest (26.3): woodland mansion map
]
# Anything mentioning one of these leads to a removed structure (mansion, villages,
# swamp hut, pillager outpost). Matched as exact ids.
REMOVED_STRUCTURE_REFS = [
    "minecraft:woodland_mansion_map",
    "#minecraft:on_woodland_mansion_maps",
    "#minecraft:on_swamp_hut_maps",
    "#minecraft:on_desert_village_maps",
    "#minecraft:on_plains_village_maps",
    "#minecraft:on_savanna_village_maps",
    "#minecraft:on_snowy_village_maps",
    "#minecraft:on_taiga_village_maps",
    "minecraft:mansion",
    "minecraft:swamp_hut",
    "minecraft:pillager_outpost",
    "#minecraft:village",
]


def build_modified_loot_table(vanilla):
    table = json.loads(json.dumps(vanilla))
    for pool in table.get("pools", []):
        pool["entries"] = [e for e in pool.get("entries", []) if not mentions(e, REMOVED_STRUCTURE_REFS)]
    table["pools"] = [p for p in table.get("pools", []) if p["entries"]]
    return table


def check_modified_loot_tables(jar, fix):
    print("\n== Loot leading to removed structures ==")
    for rel in MODIFIED_LOOT_TABLES:
        van = f"data/minecraft/loot_table/{rel}.json"
        ours = os.path.join(MOD_DATA, "minecraft", "loot_table", rel + ".json")
        if not jar.has(van):
            report("WARN", f"{rel} no longer exists in vanilla - delete our copy")
            continue
        expected = build_modified_loot_table(jar.json(van))
        if expected == jar.json(van):
            report("WARN", f"{rel} no longer contains removed-structure loot - our copy can be deleted")
        current = json.load(open(ours)) if os.path.exists(ours) else None
        if current == expected:
            report("OK", f"{rel} = vanilla minus removed-structure entries")
        elif fix:
            os.makedirs(os.path.dirname(ours), exist_ok=True)
            json.dump(expected, open(ours, "w"), indent=2)
            report("WARN", f"{rel} regenerated from vanilla (--fix)")
        else:
            report("FAIL", f"{rel} out of date - run with --fix to regenerate")
    # every OTHER vanilla loot table must not lead to a removed structure
    bad = []
    for n in sorted(jar.names):
        if n.startswith("data/minecraft/loot_table/") and n.endswith(".json"):
            rel = n[len("data/minecraft/loot_table/"):-5]
            if rel not in MODIFIED_LOOT_TABLES and mentions(jar.json(n), REMOVED_STRUCTURE_REFS):
                bad.append(rel)
    for rel in bad:
        report("FAIL", f"loot table {rel} leads to a removed structure - add it to MODIFIED_LOOT_TABLES and run --fix")
    if not bad:
        report("OK", "no other vanilla loot table leads to a removed structure")


# Cat breeds by biome (GENERATED into data/minecraft/cat_variant/ with --fix).
# variant -> (biome group tag or None, part of the default mix everywhere else?)
# A variant with a group gets a priority-1 biome condition: in those biomes ONLY
# priority-1 matches are picked. Vanilla's own non-default conditions (e.g. the
# all_black full-moon/structure rules) are kept.
CAT_VARIANT_RULES = {
    "tabby":             (None, True),
    "british_shorthair": (None, True),
    "red":               (None, True),
    "jellie":            (None, True),
    "calico":            ("#survivaltweaks:cat_variant/cherry", True),
    "white":             ("#survivaltweaks:cat_variant/cold", True),
    "black":             ("#survivaltweaks:cat_variant/dark", True),
    "all_black":         ("#survivaltweaks:cat_variant/dark", False),
    "siamese":           ("#survivaltweaks:cat_variant/warm", False),
    "persian":           ("#survivaltweaks:cat_variant/warm", False),
    "ragdoll":           ("#survivaltweaks:cat_variant/cold", False),
}
# white also belongs to the cherry grove group (Japanese white bobtails)
CAT_EXTRA_GROUPS = {"white": ["#survivaltweaks:cat_variant/cherry"]}


def build_cat_variant(vanilla, name):
    group, default = CAT_VARIANT_RULES[name]
    v = json.loads(json.dumps(vanilla))
    conds = [c for c in v.get("spawn_conditions", []) if "condition" in c]  # drop unconditioned defaults
    for g in ([group] if group else []) + CAT_EXTRA_GROUPS.get(name, []):
        conds.append({"condition": {"type": "minecraft:biome", "biomes": g}, "priority": 1})
    if default:
        conds.append({"priority": 0})
    v["spawn_conditions"] = conds
    return v


def check_cat_variants(jar, fix):
    print("\n== Cat breeds by biome (generated from the jar) ==")
    vdir = "data/minecraft/cat_variant/"
    vanilla = sorted(n[len(vdir):-5] for n in jar.names if n.startswith(vdir) and n.endswith(".json"))
    for name in vanilla:
        if name not in CAT_VARIANT_RULES:
            report("WARN", f"new vanilla cat variant '{name}' - add it to CAT_VARIANT_RULES")
    out = os.path.join(MOD_DATA, "minecraft", "cat_variant")
    for name in CAT_VARIANT_RULES:
        if name not in vanilla:
            report("FAIL", f"cat variant '{name}' no longer exists - remove it from CAT_VARIANT_RULES and its file")
            continue
        expected = build_cat_variant(jar.json(f"{vdir}{name}.json"), name)
        path = os.path.join(out, name + ".json")
        current = json.load(open(path)) if os.path.exists(path) else None
        if current == expected:
            report("OK", f"cat variant {name}")
        elif fix:
            os.makedirs(out, exist_ok=True)
            json.dump(expected, open(path, "w"), indent=2)
            report("WARN", f"cat variant {name} regenerated from vanilla (--fix)")
        else:
            report("FAIL", f"cat variant {name} out of date - run with --fix")
    # biome condition type must still exist in vanilla variants
    used = any('"minecraft:biome"' in json.dumps(jar.json(n)) for n in jar.names
               if n.startswith("data/minecraft/") and "_variant/" in n and n.endswith(".json"))
    report("OK" if used else "FAIL", "spawn condition type minecraft:biome still used by vanilla variants")
    biomes = {n[len("data/minecraft/worldgen/biome/"):-5] for n in jar.names
              if n.startswith("data/minecraft/worldgen/biome/") and n.endswith(".json")}
    tdir = os.path.join(MOD_DATA, "survivaltweaks", "tags", "worldgen", "biome")
    for root, _, files in os.walk(tdir):
        for f in files:
            for e in json.load(open(os.path.join(root, f)))["values"]:
                b = (e["id"] if isinstance(e, dict) else e).split(":")[1]
                if b not in biomes:
                    report("WARN", f"biome '{b}' in tag {f} no longer exists (skipped safely)")
    report("OK", "cat biome tags checked")


def check_advancements(jar, fix):
    print("\n== Advancements ==")
    base = os.path.join(MOD_DATA, "minecraft", "advancement")
    removed = []
    for root, _, files in os.walk(base):
        for f in files:
            rel = os.path.relpath(os.path.join(root, f), base).replace("\\", "/")[:-5]
            d = json.load(open(os.path.join(root, f)))
            if "fabric:load_conditions" in d:
                removed.append(rel)
                if not jar.has(f"data/minecraft/advancement/{rel}.json"):
                    report("WARN", f"removed advancement {rel} no longer exists in vanilla (file can be deleted)")
    report("OK", f"{len(removed)} removed advancements checked")
    # orphans
    rebuilt = os.path.join(base, "adventure", "summon_iron_golem.json")
    for n in sorted(jar.names):
        if n.startswith("data/minecraft/advancement/") and n.endswith(".json") and "/recipes/" not in n:
            rel = n[len("data/minecraft/advancement/"):-5]
            if rel in removed:
                continue
            parent = jar.json(n).get("parent", "").replace("minecraft:", "")
            if parent in removed:
                if os.path.exists(os.path.join(base, rel + ".json")):
                    report("OK", f"{rel} (child of removed {parent}) is re-parented by the mod")
                else:
                    report("FAIL", f"{rel} has removed parent {parent} - copy it from the jar and change 'parent'")
    # every vanilla advancement that is still active must not need something impossible
    ids = impossible_ids()
    for n in sorted(jar.names):
        if not (n.startswith("data/minecraft/advancement/") and n.endswith(".json")) or "/recipes/" in n:
            continue
        rel = n[len("data/minecraft/advancement/"):-5]
        if rel in removed or rel in MODIFIED_ADVANCEMENTS:
            continue
        adv = jar.json(n)
        crits = adv.get("criteria", {})
        bad = {k for k, c in crits.items() if mentions(c, ids)}
        # only a problem if EVERY alternative of some requirement group is impossible
        if bad and any(all(r in bad for r in group) for group in adv.get("requirements", [])):
            report("FAIL", f"advancement {rel} needs something impossible ({sorted(bad)[:3]}) - remove or modify it")
    report("OK", "no other advancement needs anything impossible")
    # Hired Help must equal vanilla except parent
    van = "data/minecraft/advancement/adventure/summon_iron_golem.json"
    if jar.has(van) and os.path.exists(rebuilt):
        v = jar.json(van)
        m = json.load(open(rebuilt))
        v2 = dict(v, parent=m.get("parent"))
        if v2 == m:
            report("OK", "Hired Help copy matches vanilla (except parent)")
        elif fix:
            json.dump(v2, open(rebuilt, "w"), indent=2)
            report("WARN", "Hired Help regenerated from vanilla (--fix)")
        else:
            report("FAIL", "Hired Help differs from vanilla - run with --fix to re-copy it")


def check_structure_sets(jar, fix):
    print("\n== Structure sets ==")
    base = os.path.join(MOD_DATA, "minecraft", "worldgen", "structure_set")
    for f in sorted(os.listdir(base)):
        van = f"data/minecraft/worldgen/structure_set/{f}"
        if not jar.has(van):
            report("FAIL", f"structure set {f} not in vanilla any more")
            continue
        v = jar.json(van)
        if "frequency" not in json.dumps(v) and not any(
                "frequency" in jar.json(n).get("placement", {}) for n in jar.names
                if n.startswith("data/minecraft/worldgen/structure_set/") and n.endswith(".json")):
            report("FAIL", "no vanilla structure set uses 'frequency' any more - the removal method must change")
        v["placement"]["frequency"] = 0.0
        m = json.load(open(os.path.join(base, f)))
        if v == m:
            report("OK", f"{f} = vanilla + frequency 0")
        elif fix:
            json.dump(v, open(os.path.join(base, f), "w"), indent=2)
            report("WARN", f"{f} regenerated from vanilla (--fix)")
        else:
            report("FAIL", f"{f} differs from vanilla - run with --fix")


def check_recipes(jar):
    print("\n== Recipes ==")
    base = os.path.join(MOD_DATA, "minecraft", "recipe")
    for f in sorted(os.listdir(base)):
        ok = jar.has(f"data/minecraft/recipe/{f}")
        report("OK" if ok else "FAIL", f"overridden recipe {f} exists in vanilla")
    keys = {"smoking": "cooked_beef_from_smoking.json"}
    v = jar.json("data/minecraft/recipe/" + keys["smoking"])
    ours = json.load(open(os.path.join(MOD_DATA, "survivaltweaks", "recipe", "leather_from_smoking.json")))
    missing = set(ours) - set(v) - {"category"}
    report("OK" if not missing else "WARN", f"smoking recipe fields match vanilla {sorted(missing) or ''}")


def collect_loot_shapes(obj, out, enchants=None):
    if isinstance(obj, dict):
        if obj.get("type") == "minecraft:set_enchantments" and isinstance(obj.get("enchantments"), dict):
            if enchants is not None:
                enchants.extend(obj["enchantments"].items())
            obj = {k: v for k, v in obj.items() if k != "enchantments"}
            out.add("enchantments")
        for k in obj:
            out.add(k)
        if "type" in obj:
            out.add("type=" + str(obj["type"]))
        for v in obj.values():
            collect_loot_shapes(v, out, enchants)
    elif isinstance(obj, list):
        for v in obj:
            collect_loot_shapes(v, out, enchants)


def check_loot(jar):
    print("\n== Loot tables ==")
    vanilla_shapes = set()
    for n in jar.names:
        if n.startswith("data/minecraft/loot_table/") and n.endswith(".json"):
            collect_loot_shapes(jar.json(n), vanilla_shapes)
    base = os.path.join(MOD_DATA, "survivaltweaks", "loot_table")
    for root, _, files in os.walk(base):
        for f in files:
            path = os.path.join(root, f)
            ours = set()
            enchants = []
            collect_loot_shapes(json.load(open(path)), ours, enchants)
            for eid, lvl in enchants:
                f = f"data/minecraft/enchantment/{eid.split(':')[1]}.json"
                if not jar.has(f):
                    report("FAIL", f"{os.path.relpath(path, base)}: enchantment {eid} does not exist")
                elif lvl > jar.json(f).get("max_level", 1):
                    report("FAIL", f"{os.path.relpath(path, base)}: {eid} level {lvl} > vanilla max {jar.json(f)['max_level']}")
            unknown = sorted(s for s in ours - vanilla_shapes if not s.startswith("type=minecraft:uniform"))
            rel = os.path.relpath(path, base)
            if unknown:
                report("FAIL", f"{rel}: keys/types not used by any vanilla loot table: {unknown}")
            else:
                report("OK", f"{rel}: format matches vanilla")
    src = open(os.path.join(MOD, "java", "com", "survivaltweaks", "LootTweaks.java")).read()
    for t in re.findall(r'add\("minecraft:([^"]+)"', src):
        ok = jar.has(f"data/minecraft/loot_table/{t}.json")
        report("OK" if ok else "WARN", f"vanilla loot table {t} " + ("exists" if ok else "MISSING - no bonus loot there"))


def check_misc(jar):
    print("\n== Entities, gamerules, sprites, enchantments ==")
    strings = set()
    for n in jar.names:
        if re.match(r"net/minecraft/world/(entity/EntityType|level/gamerules/)\w*\.class$", n):
            strings |= jar.cls(n[:-6].replace("/", ".")).get("strings", set())
    for f in os.listdir(os.path.join(MOD_DATA, "survivaltweaks", "tags", "entity_type")):
        for v in json.load(open(os.path.join(MOD_DATA, "survivaltweaks", "tags", "entity_type", f)))["values"]:
            eid = (v["id"] if isinstance(v, dict) else v).split(":")[1]
            if eid not in strings:
                report("WARN", f"entity '{eid}' (tag {f}) not found in vanilla")
    report("OK", "entity ids in tags checked")
    gdir = os.path.join(MOD_DATA, "survivaltweaks", "function", "gamerules")
    for f in sorted(os.listdir(gdir)):
        rule = [l for l in open(os.path.join(gdir, f)) if l.startswith("gamerule ")][0].split()[1]
        report("OK" if rule in strings else "FAIL", f"gamerule {rule} ({f})")
    for s in ("armor_full", "armor_empty"):
        ok = jar.has(f"assets/minecraft/textures/gui/sprites/hud/{s}.png")
        report("OK" if ok else "FAIL", f"HUD sprite hud/{s}")
    report("OK" if jar.has("data/minecraft/enchantment/unbreaking.json") else "FAIL", "enchantment unbreaking")


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not args:
        print(__doc__)
        sys.exit(2)
    fix = "--fix" in sys.argv
    jar = Jar(args[0])
    print("Checking", args[0], "(fix mode)" if fix else "")
    check_mixins(jar)
    check_advancements(jar, fix)
    check_modified_advancements(jar, fix)
    check_modified_loot_tables(jar, fix)
    check_cat_variants(jar, fix)
    check_structure_sets(jar, fix)
    check_recipes(jar)
    check_loot(jar)
    check_misc(jar)
    print(f"\nSummary: {results['OK']} OK, {results['WARN']} WARN, {results['FAIL']} FAIL")
    sys.exit(1 if results["FAIL"] else 0)


if __name__ == "__main__":
    main()
