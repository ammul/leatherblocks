#!/usr/bin/env python3
"""Checks all resources: JSON syntax, file layout and cross-references."""
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
        errors.append(f"MISSING: {rel}")
        return None
    with open(path, encoding="utf-8") as fh:
        try:
            data = json.load(fh)
        except json.JSONDecodeError as exc:
            errors.append(f"BROKEN JSON: {rel} -> {exc}")
            return None
    checked += 1
    return data


def exists(rel):
    return os.path.isfile(os.path.join(RES, rel))


# 1) Read registry names from the Java code, not from the generator --------
VAULT = "leather_vault"
java = open(os.path.join(ROOT, "src/main/java/de/cedric/leatherblocks/LeatherTiers.java"),
            encoding="utf-8").read()
block = re.search(r"TIER_NAMES\s*=\s*\{(.*?)\}", java, re.S).group(1)
tiers = re.findall(r'"([a-z0-9_]+)"', block)
print(f"Registered tiers per LeatherTiers.java: {len(tiers)}")

# 2) Every tier needs the complete file set ---------------------------------
for idx, name in enumerate(tiers):
    bs = load(f"assets/{MODID}/blockstates/{name}.json")
    if bs:
        model = bs["variants"][""]["model"]
        if model != f"{MODID}:block/{name}":
            errors.append(f"{name}: blockstate points to {model}")
        if not exists(f"assets/{MODID}/models/block/{name}.json"):
            errors.append(f"{name}: block model {model} does not exist")

    bm = load(f"assets/{MODID}/models/block/{name}.json")
    if bm:
        tex = bm["textures"]["all"]
        texfile = f"assets/{MODID}/textures/{tex.split(':')[1]}.png"
        if not exists(texfile):
            errors.append(f"{name}: texture {tex} missing ({texfile})")

    im = load(f"assets/{MODID}/models/item/{name}.json")
    if im and im["parent"] != f"{MODID}:block/{name}":
        errors.append(f"{name}: item model points to {im['parent']}")

    lt = load(f"data/{MODID}/loot_table/blocks/{name}.json")
    if lt:
        drop = lt["pools"][0]["entries"][0]["name"]
        if drop != f"{MODID}:{name}":
            errors.append(f"{name}: loot table drops {drop}")

    # 3) Recipe chain: tier n <- 9x tier n-1, and back again
    src = "minecraft:leather" if idx == 0 else f"{MODID}:{tiers[idx-1]}"
    r = load(f"data/{MODID}/recipe/{name}.json")
    if r:
        if r["pattern"] != ["###", "###", "###"]:
            errors.append(f"{name}: craft pattern is not 3x3")
        if r["key"]["#"]["item"] != src:
            errors.append(f"{name}: crafts from {r['key']['#']['item']}, expected {src}")
        if r["result"]["id"] != f"{MODID}:{name}":
            errors.append(f"{name}: recipe result is wrong")
        if "item" in r["result"]:
            errors.append(f"{name}: result uses 'item' instead of 'id' (1.21 format)")

    u = load(f"data/{MODID}/recipe/{name}_uncraft.json")
    if u:
        if u["ingredients"][0]["item"] != f"{MODID}:{name}":
            errors.append(f"{name}: uncraft input is wrong")
        if u["result"]["id"] != src or u["result"]["count"] != 9:
            errors.append(f"{name}: uncraft doesn't give 9x {src}")

# 4) Lang files complete -----------------------------------------------------
for lang in ("en_us", "de_de"):
    data = load(f"assets/{MODID}/lang/{lang}.json")
    if data:
        for name in tiers:
            key = f"block.{MODID}.{name}"
            if key not in data:
                errors.append(f"{lang}.json: {key} missing")
        for key in (f"tooltip.{MODID}.worth", f"tooltip.{MODID}.group_separator",
                    f"block.{MODID}.{VAULT}"):
            if key not in data:
                errors.append(f"{lang}.json: {key} missing")
        for key in data:
            if not key.startswith((f"block.{MODID}.", f"tooltip.{MODID}.", f"message.{MODID}.")):
                errors.append(f"{lang}.json: unexpected key {key}")
        if data.get(f"tooltip.{MODID}.worth", "").count("%s") != 1:
            errors.append(f"{lang}.json: tooltip.worth needs exactly one %s")

# 4b) Both languages must have matching keys, otherwise one falls back to the raw key
_en = load(f"assets/{MODID}/lang/en_us.json") or {}
_de = load(f"assets/{MODID}/lang/de_de.json") or {}
for missing in sorted(set(_en) - set(_de)):
    errors.append(f"de_de.json missing key from en_us: {missing}")
for missing in sorted(set(_de) - set(_en)):
    errors.append(f"en_us.json missing key from de_de: {missing}")
for key in sorted(set(_en) & set(_de)):
    if _en[key].count("%s") != _de[key].count("%s"):
        errors.append(f"{key}: different number of %s in en_us/de_de")

# 4c) The vault: its own file set, but NOT part of the compression chain
vbs = load(f"assets/{MODID}/blockstates/{VAULT}.json")
vm = load(f"assets/{MODID}/models/block/{VAULT}.json")
if vm:
    for slot, tex in vm["textures"].items():
        if not exists(f"assets/{MODID}/textures/{tex.split(':')[1]}.png"):
            errors.append(f"{VAULT}: texture {tex} ({slot}) missing")
load(f"assets/{MODID}/models/item/{VAULT}.json")
vlt = load(f"data/{MODID}/loot_table/blocks/{VAULT}.json")
if vlt and vlt["pools"][0]["entries"][0]["name"] != f"{MODID}:{VAULT}":
    errors.append(f"{VAULT}: loot table drops the wrong item")
vr = load(f"data/{MODID}/recipe/{VAULT}.json")
if vr:
    leather_used = sum(row.count("L") for row in vr["pattern"])
    if leather_used != 6:
        errors.append(f"{VAULT}: recipe uses {leather_used} leather, expected 6")
    if vr["key"]["L"]["item"] != "minecraft:leather":
        errors.append(f"{VAULT}: recipe doesn't use vanilla leather")
if exists(f"data/{MODID}/recipe/{VAULT}_uncraft.json"):
    errors.append(f"{VAULT}: must not have an uncraft recipe")

# 5) Tags --------------------------------------------------------------------
for tag, expected in (
        ("data/minecraft/tags/block/mineable/hoe.json", tiers + [VAULT]),
        # The vault is not a storage block - otherwise AE2 autocrafting would reach into it
        ("data/c/tags/block/storage_blocks.json", tiers),
        ("data/c/tags/item/storage_blocks.json", tiers)):
    data = load(tag)
    if data and sorted(data["values"]) != sorted(f"{MODID}:{n}" for n in expected):
        errors.append(f"{tag}: values don't match the blocks")

# 6) Orphaned files (e.g. after removing a tier) -----------------------------
for folder, suffix in ((f"assets/{MODID}/blockstates", ".json"),
                       (f"assets/{MODID}/models/block", ".json"),
                       (f"assets/{MODID}/textures/block", ".png")):
    d = os.path.join(RES, folder)
    for f in os.listdir(d):
        stem = f[: -len(suffix)]
        if stem not in tiers and stem not in (VAULT, f"{VAULT}_top", f"{VAULT}_side"):
            errors.append(f"Orphaned file: {folder}/{f}")

# 9) Java classes that must exist
for cls in ("LeatherTiers", "LeatherScores", "LeatherDepotData", "LeatherVaultBlock",
            "LeatherVaultBlockEntity", "ModCommands", "ModRegistry",
            "CompressedLeatherBlockItem", "LeatherBlocks"):
    if not os.path.isfile(os.path.join(ROOT, f"src/main/java/de/cedric/leatherblocks/{cls}.java")):
        errors.append(f"Java class missing: {cls}.java")
# The pure core must not gain Minecraft imports, or it stops being testable
for pure in ("LeatherTiers", "LeatherScores"):
    src = open(os.path.join(ROOT, f"src/main/java/de/cedric/leatherblocks/{pure}.java"),
               encoding="utf-8").read()
    for bad in ("import net.minecraft", "import net.neoforged"):
        if bad in src:
            errors.append(f"{pure}.java: {bad} - the core must stay dependency-free")

# 7) Texture format -----------------------------------------------------------
try:
    from PIL import Image
    for name in tiers:
        tex = os.path.join(RES, f"assets/{MODID}/textures/block/{name}.png")
        if not os.path.isfile(tex):
            errors.append(f"Texture missing: {name}.png")
            continue
        img = Image.open(tex)
        if img.size != (16, 16):
            errors.append(f"{name}.png is {img.size}, expected 16x16")
        if img.mode != "RGBA":
            errors.append(f"{name}.png is {img.mode}, expected RGBA")
except ImportError:
    print("(Pillow missing - skipping texture check)")

# 8) mods.toml / pack.mcmeta --------------------------------------------------
toml = open(os.path.join(RES, "META-INF/neoforge.mods.toml"), encoding="utf-8").read()
if f'modId = "{MODID}"' not in toml:
    errors.append("neoforge.mods.toml: modId doesn't match the namespace")
if not exists("pack.mcmeta"):
    errors.append("pack.mcmeta missing")
load("pack.mcmeta")

print(f"{checked} JSON files parsed, {len(tiers)} tiers checked.")
if errors:
    print(f"\n{len(errors)} PROBLEMS:")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print("Everything consistent.")
