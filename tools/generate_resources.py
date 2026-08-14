#!/usr/bin/env python3
"""
Generates all resource files (blockstates, models, lang, recipes, loot tables,
tags) and the textures for the Leather Blocks mod.

Adding a new compression tier:
  1. Extend TIERS here
  2. Extend TIER_NAMES in ModRegistry.java with the same name
  3. python3 tools/generate_resources.py
"""
import json
import os
import random

MODID = "leatherblocks"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src", "main", "resources")

# (registry_name, en_us, de_de)
TIERS = [
    ("leather_block",                      "Leather Block",                      "Lederblock"),
    ("compressed_leather_block",           "Compressed Leather Block",           "Komprimierter Lederblock"),
    ("double_compressed_leather_block",    "Double Compressed Leather Block",    "Doppelt komprimierter Lederblock"),
    ("triple_compressed_leather_block",    "Triple Compressed Leather Block",    "Dreifach komprimierter Lederblock"),
    ("quadruple_compressed_leather_block", "Quadruple Compressed Leather Block", "Vierfach komprimierter Lederblock"),
    ("quintuple_compressed_leather_block", "Quintuple Compressed Leather Block", "Fuenffach komprimierter Lederblock"),
    ("sextuple_compressed_leather_block",  "Sextuple Compressed Leather Block",  "Sechsfach komprimierter Lederblock"),
    ("septuple_compressed_leather_block",  "Septuple Compressed Leather Block",  "Siebenfach komprimierter Lederblock"),
    ("octuple_compressed_leather_block",   "Octuple Compressed Leather Block",   "Achtfach komprimierter Lederblock"),
    ("nonuple_compressed_leather_block",   "Nonuple Compressed Leather Block",   "Neunfach komprimierter Lederblock"),
]


def write_json(relpath, data):
    path = os.path.join(RES, relpath)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


VAULT = "leather_vault"


def gen_vault():
    write_json(f"assets/{MODID}/blockstates/{VAULT}.json",
               {"variants": {"": {"model": f"{MODID}:block/{VAULT}"}}})
    write_json(f"assets/{MODID}/models/block/{VAULT}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": f"{MODID}:block/{VAULT}_top",
            "side": f"{MODID}:block/{VAULT}_side",
            "bottom": f"{MODID}:block/leather_block",
        }
    })
    write_json(f"assets/{MODID}/models/item/{VAULT}.json", {"parent": f"{MODID}:block/{VAULT}"})

    # 6 leather as 2x3 - clearly distinct from the leather block's 3x3.
    write_json(f"data/{MODID}/recipe/{VAULT}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["LLL", "LLL"],
        "key": {"L": {"item": "minecraft:leather"}},
        "result": {"id": f"{MODID}:{VAULT}", "count": 1}
    })

    write_json(f"data/{MODID}/loot_table/blocks/{VAULT}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MODID}:{VAULT}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]
        }]
    })


def gen_models_and_blockstates():
    for name, _, _ in TIERS:
        write_json(f"assets/{MODID}/blockstates/{name}.json", {
            "variants": {"": {"model": f"{MODID}:block/{name}"}}
        })
        write_json(f"assets/{MODID}/models/block/{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"{MODID}:block/{name}"}
        })
        write_json(f"assets/{MODID}/models/item/{name}.json", {
            "parent": f"{MODID}:block/{name}"
        })


def gen_lang():
    en, de = {}, {}
    for name, en_name, de_name in TIERS:
        en[f"block.{MODID}.{name}"] = en_name
        de[f"block.{MODID}.{name}"] = de_name
    # Tooltip: %s is the already-grouped number.
    en[f"tooltip.{MODID}.worth"] = "= %s Leather"
    de[f"tooltip.{MODID}.worth"] = "= %s Leder"
    # Thousands separator per language.
    en[f"tooltip.{MODID}.group_separator"] = ","
    de[f"tooltip.{MODID}.group_separator"] = "."

    en[f"block.{MODID}.{VAULT}"] = "Leather Depot"
    de[f"block.{MODID}.{VAULT}"] = "Lederdepot"
    en[f"tooltip.{MODID}.vault"] = "Feed with hoppers. One per player. Contents are consumed."
    de[f"tooltip.{MODID}.vault"] = "Per Hopper befuellbar. Eines pro Spieler. Inhalt wird verbraucht."

    msgs = {
        "vault_exists": ("You already have a depot at %s %s %s (%s).",
                         "Du hast schon ein Lederdepot bei %s %s %s (%s)."),
        "vault_placed": ("Depot registered. Fill it with leather or leather blocks.",
                         "Depot registriert. Fuell es mit Leder oder Lederbloecken."),
        "vault_info":   ("Depot of %s: %s leather", "Depot von %s: %s Leder"),
        "own":          ("You have stored %s leather (rank %s).",
                         "Du hast %s Leder eingelagert (Platz %s)."),
        "none":         ("You have not stored any leather yet.",
                         "Du hast noch kein Leder eingelagert."),
        "empty":        ("Nobody has stored leather yet.", "Noch hat niemand Leder eingelagert."),
        "top_header":   ("Leather leaderboard", "Leder-Bestenliste"),
        "top_line":     ("%s. %s - %s leather", "%s. %s - %s Leder"),
    }
    for key, (en_text, de_text) in msgs.items():
        en[f"message.{MODID}.{key}"] = en_text
        de[f"message.{MODID}.{key}"] = de_text
    write_json(f"assets/{MODID}/lang/en_us.json", en)
    write_json(f"assets/{MODID}/lang/de_de.json", de)


def gen_recipes():
    # Tier 0 comes from vanilla leather, every other tier from 9x the tier below.
    for idx, (name, _, _) in enumerate(TIERS):
        source = "minecraft:leather" if idx == 0 else f"{MODID}:{TIERS[idx - 1][0]}"

        write_json(f"data/{MODID}/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": f"{MODID}_compression",
            "pattern": ["###", "###", "###"],
            "key": {"#": {"item": source}},
            "result": {"id": f"{MODID}:{name}", "count": 1}
        })

        write_json(f"data/{MODID}/recipe/{name}_uncraft.json", {
            "type": "minecraft:crafting_shapeless",
            "category": "building",
            "group": f"{MODID}_decompression",
            "ingredients": [{"item": f"{MODID}:{name}"}],
            "result": {"id": source, "count": 9}
        })


def gen_loot_tables():
    for name, _, _ in TIERS:
        write_json(f"data/{MODID}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]
            }]
        })


def gen_tags():
    names = [f"{MODID}:{n}" for n, _, _ in TIERS]
    # Like wool: fastest with a hoe, but mineable without a tool.
    write_json("data/minecraft/tags/block/mineable/hoe.json",
               {"replace": False, "values": names + [f"{MODID}:{VAULT}"]})
    # So mods like Mekanism/AE2 recognize the blocks as storage blocks.
    write_json("data/c/tags/block/storage_blocks.json",
               {"replace": False, "values": names})
    write_json("data/c/tags/item/storage_blocks.json",
               {"replace": False, "values": names})


# --------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------
def gen_textures():
    from PIL import Image

    out_dir = os.path.join(RES, "assets", MODID, "textures", "block")
    os.makedirs(out_dir, exist_ok=True)

    for tier, (name, _, _) in enumerate(TIERS):
        rng = random.Random(1337)          # same grain on every tier
        img = Image.new("RGBA", (16, 16))
        px = img.load()

        # base tone gets a bit darker/more saturated per tier
        base = (
            168 - tier * 8,
            112 - tier * 6,
            66 - tier * 3,
        )

        for y in range(16):
            for x in range(16):
                n = rng.randint(-14, 14)
                # slight leather grain
                if (x * 3 + y * 5) % 7 == 0:
                    n -= 8
                px[x, y] = (
                    max(0, min(255, base[0] + n)),
                    max(0, min(255, base[1] + n)),
                    max(0, min(255, base[2] + n)),
                    255,
                )

        def shade(x, y, f):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * f), int(g * f), int(b * f), 255)

        # frame: light top/left, dark bottom/right -> seam look
        for i in range(16):
            shade(i, 0, 1.18)
            shade(0, i, 1.18)
            shade(i, 15, 0.72)
            shade(15, i, 0.72)

        # stitching one pixel inside the frame
        for i in range(2, 14, 3):
            shade(i, 2, 0.62)
            shade(i, 13, 0.62)
            shade(2, i, 0.62)
            shade(13, i, 0.62)

        # rivets indicate the compression tier (0 = none)
        studs = [(7, 7),
                 (4, 4), (10, 4), (4, 10), (10, 10),
                 (7, 4), (4, 7), (10, 7), (7, 10)]
        for s in range(min(tier, len(studs))):
            sx, sy = studs[s]
            px[sx, sy] = (232, 214, 178, 255)
            px[sx + 1, sy] = (150, 132, 100, 255)
            px[sx, sy + 1] = (150, 132, 100, 255)
            px[sx + 1, sy + 1] = (110, 94, 70, 255)

        img.save(os.path.join(out_dir, f"{name}.png"))


def gen_vault_textures():
    from PIL import Image

    out_dir = os.path.join(RES, "assets", MODID, "textures", "block")
    os.makedirs(out_dir, exist_ok=True)

    def leather_base(seed, base):
        rng = random.Random(seed)
        img = Image.new("RGBA", (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                n = rng.randint(-14, 14)
                if (x * 3 + y * 5) % 7 == 0:
                    n -= 8
                px[x, y] = tuple(max(0, min(255, c + n)) for c in base) + (255,)
        return img, px

    def shade(px, x, y, f):
        r, g, b, a = px[x, y]
        px[x, y] = (int(min(255, r * f)), int(min(255, g * f)), int(min(255, b * f)), 255)

    # Lid: dark drop-in slot with a light border
    img, px = leather_base(4711, (150, 100, 60))
    for i in range(16):
        shade(px, i, 0, 1.18)
        shade(px, 0, i, 1.18)
        shade(px, i, 15, 0.72)
        shade(px, 15, i, 0.72)
    for y in range(5, 11):
        for x in range(5, 11):
            px[x, y] = (28, 20, 14, 255)
    for i in range(4, 12):
        shade(px, i, 4, 0.55)
        shade(px, i, 11, 1.25)
        shade(px, 4, i, 0.55)
        shade(px, 11, i, 1.25)
    img.save(os.path.join(out_dir, f"{VAULT}_top.png"))

    # Side: two horizontal straps with a buckle
    img, px = leather_base(4712, (150, 100, 60))
    for i in range(16):
        shade(px, i, 0, 1.18)
        shade(px, 0, i, 1.18)
        shade(px, i, 15, 0.72)
        shade(px, 15, i, 0.72)
    for band_y in (4, 11):
        for x in range(1, 15):
            shade(px, x, band_y, 0.58)
            shade(px, x, band_y + 1, 0.68)
        px[7, band_y] = (206, 186, 148, 255)
        px[8, band_y] = (146, 130, 100, 255)
        px[7, band_y + 1] = (146, 130, 100, 255)
        px[8, band_y + 1] = (104, 92, 70, 255)
    img.save(os.path.join(out_dir, f"{VAULT}_side.png"))


if __name__ == "__main__":
    gen_models_and_blockstates()
    gen_lang()
    gen_recipes()
    gen_loot_tables()
    gen_tags()
    gen_textures()
    gen_vault()
    gen_vault_textures()
    print(f"Generated resources for {len(TIERS)} blocks.")
