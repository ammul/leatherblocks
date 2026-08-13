#!/usr/bin/env python3
"""Prueft alle Ressourcen: JSON-Syntax, Datei-Layout und Querverweise."""
import json
import os
import re
import sys

MODID = "leatherblocks"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
errors, checked = [], 0


def load(rel):
    global checked
    path = os.path.join(RES, rel)
    if not os.path.isfile(path):
        errors.append(f"FEHLT: {rel}")
        return None
    with open(path, encoding="utf-8") as fh:
        try:
            data = json.load(fh)
        except json.JSONDecodeError as exc:
            errors.append(f"KAPUTTES JSON: {rel} -> {exc}")
            return None
    checked += 1
    return data


def exists(rel):
    return os.path.isfile(os.path.join(RES, rel))


# 1) Registry-Namen aus dem Java-Code lesen, nicht aus dem Generator ---------
VAULT = "leather_vault"
java = open(os.path.join(ROOT, "src/main/java/de/cedric/leatherblocks/LeatherTiers.java"),
            encoding="utf-8").read()
block = re.search(r"TIER_NAMES\s*=\s*\{(.*?)\}", java, re.S).group(1)
tiers = re.findall(r'"([a-z0-9_]+)"', block)
print(f"Registrierte Stufen laut LeatherTiers.java: {len(tiers)}")

# 2) Jede Stufe braucht das komplette Datei-Set -----------------------------
for idx, name in enumerate(tiers):
    bs = load(f"assets/{MODID}/blockstates/{name}.json")
    if bs:
        model = bs["variants"][""]["model"]
        if model != f"{MODID}:block/{name}":
            errors.append(f"{name}: blockstate zeigt auf {model}")
        if not exists(f"assets/{MODID}/models/block/{name}.json"):
            errors.append(f"{name}: Blockmodell {model} existiert nicht")

    bm = load(f"assets/{MODID}/models/block/{name}.json")
    if bm:
        tex = bm["textures"]["all"]
        texfile = f"assets/{MODID}/textures/{tex.split(':')[1]}.png"
        if not exists(texfile):
            errors.append(f"{name}: Textur {tex} fehlt ({texfile})")

    im = load(f"assets/{MODID}/models/item/{name}.json")
    if im and im["parent"] != f"{MODID}:block/{name}":
        errors.append(f"{name}: Item-Modell zeigt auf {im['parent']}")

    lt = load(f"data/{MODID}/loot_table/blocks/{name}.json")
    if lt:
        drop = lt["pools"][0]["entries"][0]["name"]
        if drop != f"{MODID}:{name}":
            errors.append(f"{name}: Loot-Table dropt {drop}")

    # 3) Rezept-Kette: Stufe n <- 9x Stufe n-1, und wieder zurueck
    src = "minecraft:leather" if idx == 0 else f"{MODID}:{tiers[idx-1]}"
    r = load(f"data/{MODID}/recipe/{name}.json")
    if r:
        if r["pattern"] != ["###", "###", "###"]:
            errors.append(f"{name}: Craft-Pattern ist nicht 3x3")
        if r["key"]["#"]["item"] != src:
            errors.append(f"{name}: craftet aus {r['key']['#']['item']}, erwartet {src}")
        if r["result"]["id"] != f"{MODID}:{name}":
            errors.append(f"{name}: Rezept-Ergebnis falsch")
        if "item" in r["result"]:
            errors.append(f"{name}: result nutzt 'item' statt 'id' (1.21-Format)")

    u = load(f"data/{MODID}/recipe/{name}_uncraft.json")
    if u:
        if u["ingredients"][0]["item"] != f"{MODID}:{name}":
            errors.append(f"{name}: Uncraft-Input falsch")
        if u["result"]["id"] != src or u["result"]["count"] != 9:
            errors.append(f"{name}: Uncraft gibt nicht 9x {src}")

# 4) Lang-Dateien vollstaendig ---------------------------------------------
for lang in ("en_us", "de_de"):
    data = load(f"assets/{MODID}/lang/{lang}.json")
    if data:
        for name in tiers:
            key = f"block.{MODID}.{name}"
            if key not in data:
                errors.append(f"{lang}.json: {key} fehlt")
        for key in (f"tooltip.{MODID}.worth", f"tooltip.{MODID}.group_separator",
                    f"block.{MODID}.{VAULT}"):
            if key not in data:
                errors.append(f"{lang}.json: {key} fehlt")
        for key in data:
            if not key.startswith((f"block.{MODID}.", f"tooltip.{MODID}.", f"message.{MODID}.")):
                errors.append(f"{lang}.json: unerwarteter Key {key}")
        if data.get(f"tooltip.{MODID}.worth", "").count("%s") != 1:
            errors.append(f"{lang}.json: tooltip.worth braucht genau ein %s")

# 4b) Beide Sprachen muessen deckungsgleich sein, sonst faellt eine auf den Key zurueck
_en = load(f"assets/{MODID}/lang/en_us.json") or {}
_de = load(f"assets/{MODID}/lang/de_de.json") or {}
for missing in sorted(set(_en) - set(_de)):
    errors.append(f"de_de.json fehlt Key aus en_us: {missing}")
for missing in sorted(set(_de) - set(_en)):
    errors.append(f"en_us.json fehlt Key aus de_de: {missing}")
for key in sorted(set(_en) & set(_de)):
    if _en[key].count("%s") != _de[key].count("%s"):
        errors.append(f"{key}: unterschiedlich viele %s in en_us/de_de")

# 4c) Das Depot: eigenes Datei-Set, aber NICHT in der Kompressionskette
vbs = load(f"assets/{MODID}/blockstates/{VAULT}.json")
vm = load(f"assets/{MODID}/models/block/{VAULT}.json")
if vm:
    for slot, tex in vm["textures"].items():
        if not exists(f"assets/{MODID}/textures/{tex.split(':')[1]}.png"):
            errors.append(f"{VAULT}: Textur {tex} ({slot}) fehlt")
load(f"assets/{MODID}/models/item/{VAULT}.json")
vlt = load(f"data/{MODID}/loot_table/blocks/{VAULT}.json")
if vlt and vlt["pools"][0]["entries"][0]["name"] != f"{MODID}:{VAULT}":
    errors.append(f"{VAULT}: Loot-Table dropt das falsche Item")
vr = load(f"data/{MODID}/recipe/{VAULT}.json")
if vr:
    leather_used = sum(row.count("L") for row in vr["pattern"])
    if leather_used != 6:
        errors.append(f"{VAULT}: Rezept nutzt {leather_used} Leder, erwartet 6")
    if vr["key"]["L"]["item"] != "minecraft:leather":
        errors.append(f"{VAULT}: Rezept nutzt nicht Vanilla-Leder")
if exists(f"data/{MODID}/recipe/{VAULT}_uncraft.json"):
    errors.append(f"{VAULT}: darf kein Uncraft-Rezept haben")

# 5) Tags ------------------------------------------------------------------
for tag, expected in (
        ("data/minecraft/tags/block/mineable/hoe.json", tiers + [VAULT]),
        # Das Depot ist kein Lagerblock - sonst greift AE2-Autocrafting darauf zu
        ("data/c/tags/block/storage_blocks.json", tiers),
        ("data/c/tags/item/storage_blocks.json", tiers)):
    data = load(tag)
    if data and sorted(data["values"]) != sorted(f"{MODID}:{n}" for n in expected):
        errors.append(f"{tag}: Werte stimmen nicht mit den Bloecken ueberein")

# 6) Verwaiste Dateien (z.B. nach dem Entfernen einer Stufe) ---------------
for folder, suffix in ((f"assets/{MODID}/blockstates", ".json"),
                       (f"assets/{MODID}/models/block", ".json"),
                       (f"assets/{MODID}/textures/block", ".png")):
    d = os.path.join(RES, folder)
    for f in os.listdir(d):
        stem = f[: -len(suffix)]
        if stem not in tiers and stem not in (VAULT, f"{VAULT}_top", f"{VAULT}_side"):
            errors.append(f"Verwaiste Datei: {folder}/{f}")

# 9) Java-Klassen, die es geben muss
for cls in ("LeatherTiers", "LeatherScores", "LeatherDepotData", "LeatherVaultBlock",
            "LeatherVaultBlockEntity", "ModCommands", "ModRegistry",
            "CompressedLeatherBlockItem", "LeatherBlocks"):
    if not os.path.isfile(os.path.join(ROOT, f"src/main/java/de/cedric/leatherblocks/{cls}.java")):
        errors.append(f"Java-Klasse fehlt: {cls}.java")
# Der reine Kern darf keine Minecraft-Importe bekommen, sonst ist er nicht mehr testbar
for pure in ("LeatherTiers", "LeatherScores"):
    src = open(os.path.join(ROOT, f"src/main/java/de/cedric/leatherblocks/{pure}.java"),
               encoding="utf-8").read()
    for bad in ("import net.minecraft", "import net.neoforged"):
        if bad in src:
            errors.append(f"{pure}.java: {bad} - der Kern muss abhaengigkeitsfrei bleiben")

# 7) Texturformat ----------------------------------------------------------
try:
    from PIL import Image
    for name in tiers:
        tex = os.path.join(RES, f"assets/{MODID}/textures/block/{name}.png")
        if not os.path.isfile(tex):
            errors.append(f"Textur fehlt: {name}.png")
            continue
        img = Image.open(tex)
        if img.size != (16, 16):
            errors.append(f"{name}.png ist {img.size}, erwartet 16x16")
        if img.mode != "RGBA":
            errors.append(f"{name}.png ist {img.mode}, erwartet RGBA")
except ImportError:
    print("(Pillow fehlt - Texturpruefung uebersprungen)")

# 8) mods.toml / pack.mcmeta ----------------------------------------------
toml = open(os.path.join(RES, "META-INF/neoforge.mods.toml"), encoding="utf-8").read()
if f'modId = "{MODID}"' not in toml:
    errors.append("neoforge.mods.toml: modId passt nicht zum Namespace")
if not exists("pack.mcmeta"):
    errors.append("pack.mcmeta fehlt")
load("pack.mcmeta")

print(f"{checked} JSON-Dateien geparst, {len(tiers)} Stufen geprueft.")
if errors:
    print(f"\n{len(errors)} PROBLEME:")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print("Alles konsistent.")
