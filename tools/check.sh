#!/usr/bin/env bash
# Checks everything that can be checked without Minecraft dependencies.
set -e
cd "$(dirname "$0")/.."

echo "== Generating resources =="
python3 tools/generate_resources.py

echo "== Validating resources =="
python3 tools/validate.py

echo "== Testing core logic =="
rm -rf /tmp/leatherblocks-test && mkdir -p /tmp/leatherblocks-test
javac -d /tmp/leatherblocks-test \
  src/main/java/de/cedric/leatherblocks/LeatherTiers.java \
  src/main/java/de/cedric/leatherblocks/LeatherScores.java \
  tools/CoreTest.java
java -cp /tmp/leatherblocks-test de.cedric.leatherblocks.CoreTest

echo "== Syntax-checking the Minecraft classes =="
javac -Xmaxerrs 2000 -d /tmp/leatherblocks-test src/main/java/de/cedric/leatherblocks/*.java 2>&1 \
  | grep ": error:" \
  | grep -v "does not exist" \
  | grep -v "cannot find symbol" \
  | grep -v "does not override or implement" \
  | { grep . && echo "^ real syntax errors" && exit 1 || echo "no syntax errors (only missing MC classes)"; }
