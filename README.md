# Leather Blocks

Tiny NeoForge mod for **Minecraft 1.21.1 / NeoForge 21.1.x** (ATM10).
Leather can be pressed into blocks, and the blocks can be further compressed.

## Contents

| Tier | Block | equals leather |
|---|---|---|
| 0 | Leather Block | 9 |
| 1 | Compressed Leather Block | 81 |
| 2 | Double Compressed | 729 |
| 3 | Triple Compressed | 6,561 |
| 4 | Quadruple Compressed | 59,049 |
| 5 | Quintuple Compressed | 531,441 |
| 6 | Sextuple Compressed | 4,782,969 |
| 7 | Septuple Compressed | 43,046,721 |
| 8 | Octuple Compressed | 387,420,489 |
| 9 | Nonuple Compressed | 3,486,784,401 |

Each tier: 3×3 of the tier below → 1 block, shapeless back → 9 pieces.
The texture shows the tier via the number of rivets (0–9, arranged cube-like).
Each block shows its leather equivalence in the tooltip (`= 3,486,784,401 Leather`).
The thousands separator comes from the language file (`.` for de_de, `,` for en_us),
computed with `long` — tier 9 no longer fits in an `int`.
The blocks don't store a counter, the value is purely informational.

Properties are deliberately minimal: wool sound, brown map color, mineable
without a tool (fastest with an axe), hardness 0.8 + 0.2 per tier (tier 9 = 2.6), **not flammable**
(so a lava accident doesn't eat 59k leather from you — uncomment `.ignitedByLava()` in
`ModRegistry.properties()` if you want that anyway).

Tagged as `c:storage_blocks`, so AE2/Mekanism & co. recognize them as storage blocks.

## Leather Vault & Leaderboard

Craftable from 6 leather (2×3). **One vault per player** — whoever places a
second one gets it back immediately, along with a hint about the first one's coordinates.

Can be filled via hopper, pipe, or by hand through any system that uses
`IItemHandler`. Accepts vanilla leather and all compression tiers; the
leather equivalent is credited to the owner.

**Items dropped in are consumed.** This is intentional: if the vault gave
items back out, you could pump the same 64 blocks in a loop and inflate the
score indefinitely. This way every point costs real leather. If you don't
want that, you'll need to implement `extractItem` in
`LeatherVaultBlockEntity.DepotHandler` **and** deduct the score again on withdrawal.

The score lives in `SavedData` attached to the Overworld, not in the block —
mining the vault and placing it elsewhere loses nothing, it just frees up the space.

```
/leder        # your own score and placement
/leder top    # top 10
```

No vanilla scoreboard: those scores are `int` and overflow with a single
nonuple-compressed block.

Right-clicking the vault shows the owner and score in chat.

## Building

Requires Java 21.

```bash
./gradlew build          # -> build/libs/leatherblocks-1.0.0.jar
./gradlew runClient      # for testing
```

The plugin and NeoForge versions are in `gradle.properties`. If
ModDevGradle fails to resolve: take the current version from the official MDK for 1.21.1
(https://github.com/neoforged/MDK/tree/1.21.1), or pull the MDK
and just copy in `src/` + the `gradle.properties` values.

## Installing

Put the jar in the `mods/` folder — **on the server and all clients**. The mod
ships its own blocks and textures, a client without it gets kicked on join.

## Adding/removing a tier

1. Adjust `TIER_NAMES` in `src/main/java/de/cedric/leatherblocks/ModRegistry.java`
2. Adjust `TIERS` in `tools/generate_resources.py` identically
3. `python3 tools/generate_resources.py && python3 tools/validate.py`

`bash tools/check.sh` runs everything: generates resources, validates,
runs `tools/CoreTest.java` against the core logic (values, overflow, leaderboard
sorting), plus a syntax check of the Minecraft classes. Only needs a JDK.

`LeatherTiers` and `LeatherScores` are deliberately free of Minecraft
imports, so the logic stays testable without a game environment — the validator insists on it.

`tools/validate.py` reads the names from the Java code and checks them against all
JSONs, recipe chains, textures, lang keys, and tags — including orphaned files.
