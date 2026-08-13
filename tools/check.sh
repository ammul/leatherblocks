#!/usr/bin/env bash
# Alles pruefen, was ohne Minecraft-Abhaengigkeiten pruefbar ist.
set -e
cd "$(dirname "$0")/.."

echo "== Ressourcen erzeugen =="
python3 tools/generate_resources.py

echo "== Ressourcen validieren =="
python3 tools/validate.py

echo "== Kern-Logik testen =="
rm -rf /tmp/leatherblocks-test && mkdir -p /tmp/leatherblocks-test
javac -d /tmp/leatherblocks-test \
  src/main/java/de/cedric/leatherblocks/LeatherTiers.java \
  src/main/java/de/cedric/leatherblocks/LeatherScores.java \
  tools/CoreTest.java
java -cp /tmp/leatherblocks-test de.cedric.leatherblocks.CoreTest

echo "== Syntaxpruefung der Minecraft-Klassen =="
javac -Xmaxerrs 2000 -d /tmp/leatherblocks-test src/main/java/de/cedric/leatherblocks/*.java 2>&1 \
  | grep ": error:" \
  | grep -v "does not exist" \
  | grep -v "cannot find symbol" \
  | grep -v "does not override or implement" \
  | { grep . && echo "^ echte Syntaxfehler" && exit 1 || echo "keine Syntaxfehler (nur fehlende MC-Klassen)"; }
