#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/preview docs
javac -d build/preview app/src/main/java/com/casklabs/alphablocks/Game.java \
  app/src/main/java/com/casklabs/alphablocks/RepeatInput.java \
  app/src/main/java/com/casklabs/alphablocks/GameSession.java \
  app/src/main/java/com/casklabs/alphablocks/GameScene.java scripts/Preview.java
java -Djava.awt.headless=true -cp build/preview Preview
