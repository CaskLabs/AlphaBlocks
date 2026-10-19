#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/test-deps build/unit-tests
fetch() {
    file="build/test-deps/$1"
    if [ ! -f "$file" ]; then
        curl --fail --location --retry 2 --connect-timeout 15 --max-time 60 \
            "https://repo.maven.apache.org/maven2/$2" --output "$file"
    fi
    python3 - "$file" "$3" <<'PY'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1])
if hashlib.sha256(p.read_bytes()).hexdigest() != sys.argv[2]:
    p.unlink()
    raise SystemExit('Dependency checksum failed: ' + str(p))
PY
}
fetch junit.jar junit/junit/4.12/junit-4.12.jar \
    59721f0805e223d84b90677887d9ff567dc534d7c502ca903c0c2b17f05c116a
fetch hamcrest.jar org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar \
    66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9
test_classpath="build/test-deps/junit.jar:build/test-deps/hamcrest.jar"
javac -source 1.6 -target 1.6 -Xlint:-options -cp "$test_classpath" -d build/unit-tests \
    app/src/main/java/com/casklabs/alphablocks/Game.java \
    app/src/main/java/com/casklabs/alphablocks/RepeatInput.java \
    app/src/main/java/com/casklabs/alphablocks/GameSession.java \
    app/src/main/java/com/casklabs/alphablocks/GameScene.java \
    app/src/test/java/com/casklabs/alphablocks/*.java
java -cp "build/unit-tests:$test_classpath" org.junit.runner.JUnitCore \
    com.casklabs.alphablocks.GameTest com.casklabs.alphablocks.GameSessionTest
