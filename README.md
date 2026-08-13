# Leather Blocks

Winzige NeoForge-Mod für **Minecraft 1.21.1 / NeoForge 21.1.x** (ATM10).
Leder lässt sich zu Blöcken pressen, die Blöcke lassen sich weiter komprimieren.

## Inhalt

| Stufe | Block | entspricht Leder |
|---|---|---|
| 0 | Lederblock | 9 |
| 1 | Komprimierter Lederblock | 81 |
| 2 | Doppelt komprimiert | 729 |
| 3 | Dreifach komprimiert | 6.561 |
| 4 | Vierfach komprimiert | 59.049 |
| 5 | Fünffach komprimiert | 531.441 |
| 6 | Sechsfach komprimiert | 4.782.969 |
| 7 | Siebenfach komprimiert | 43.046.721 |
| 8 | Achtfach komprimiert | 387.420.489 |
| 9 | Neunfach komprimiert | 3.486.784.401 |

Jede Stufe: 3×3 der Stufe darunter → 1 Block, shapeless zurück → 9 Stück.
Textur zeigt die Stufe über die Anzahl der Nieten (0–9, würfelartig angeordnet).
Jeder Block zeigt seine Leder-Äquivalenz im Tooltip an (`= 3.486.784.401 Leder`).
Das Tausendertrennzeichen kommt aus der Sprachdatei (`.` für de_de, `,` für en_us),
gerechnet wird mit `long` — Stufe 9 passt nicht mehr in ein `int`.
Die Blöcke speichern keinen Zähler, der Wert ist rein informativ.

Eigenschaften bewusst minimal: Wolle-Sound, braune Kartenfarbe, ohne Werkzeug
abbaubar (mit Hacke am schnellsten), Härte 0.8 + 0.2 pro Stufe (Stufe 9 = 2.6), **nicht brennbar**
(damit dir kein Lavaunfall 59k Leder frisst — `.ignitedByLava()` in
`ModRegistry.properties()` einkommentieren, falls doch gewünscht).

Getaggt als `c:storage_blocks`, damit AE2/Mekanism & Co. sie als Lagerblöcke sehen.

## Lederdepot & Bestenliste

Aus 6 Leder (2×3) craftbar. **Ein Depot pro Spieler** — wer ein zweites setzt,
bekommt es sofort samt Hinweis auf die Koordinaten des ersten zurück.

Befüllbar per Hopper, Rohr oder von Hand über jedes System, das `IItemHandler`
nutzt. Angenommen werden Vanilla-Leder und alle Kompressionsstufen; der
Leder-Gegenwert wird dem Besitzer gutgeschrieben.

**Eingeworfenes wird verbraucht.** Das ist Absicht: gäbe das Depot Items wieder
heraus, könnte man dieselben 64 Blöcke im Kreis pumpen und den Score beliebig
hochtreiben. So kostet jeder Punkt echtes Leder. Wer das nicht will, muss
`extractItem` in `LeatherVaultBlockEntity.DepotHandler` implementieren **und**
beim Herausgeben den Punktestand wieder abziehen.

Der Punktestand liegt in `SavedData` an der Overworld, nicht im Block — Depot
abbauen und woanders neu setzen verliert nichts, gibt nur den Platz wieder frei.

```
/leder        # eigener Stand und Platzierung
/leder top    # Top 10
```

Kein Vanilla-Scoreboard: dessen Scores sind `int` und laufen bei einem einzigen
neunfach komprimierten Block über.

Rechtsklick auf das Depot zeigt Besitzer und Stand im Chat.

## Bauen

Java 21 nötig.

```bash
./gradlew build          # -> build/libs/leatherblocks-1.0.0.jar
./gradlew runClient      # zum Testen
```

Die Plugin- und NeoForge-Versionen stehen in `gradle.properties`. Falls
ModDevGradle nicht auflöst: aktuelle Version aus dem offiziellen MDK für 1.21.1
übernehmen (https://github.com/neoforged/MDK/tree/1.21.1), oder das MDK ziehen
und einfach `src/` + `gradle.properties`-Werte hineinkopieren.

## Installieren

Jar in den `mods/`-Ordner — **auf Server und allen Clients**. Die Mod bringt
Blöcke und Texturen mit, ein Client ohne sie fliegt beim Join raus.

## Stufe hinzufügen/entfernen

1. `TIER_NAMES` in `src/main/java/de/cedric/leatherblocks/ModRegistry.java` anpassen
2. `TIERS` in `tools/generate_resources.py` identisch anpassen
3. `python3 tools/generate_resources.py && python3 tools/validate.py`

`bash tools/check.sh` fährt alles: Ressourcen erzeugen, validieren,
`tools/CoreTest.java` gegen die Kernlogik (Werte, Überlauf, Sortierung der
Bestenliste), plus Syntaxprüfung der Minecraft-Klassen. Braucht nur ein JDK.

`LeatherTiers` und `LeatherScores` sind bewusst frei von Minecraft-Importen,
damit die Logik ohne Spielumgebung testbar bleibt — der Validator besteht darauf.

`tools/validate.py` liest die Namen aus dem Java-Code und prüft dagegen alle
JSONs, Rezeptketten, Texturen, Lang-Keys und Tags — inklusive verwaister Dateien.
