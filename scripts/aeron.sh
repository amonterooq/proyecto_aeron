#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
task="${1:-build}"
case "$task" in build|run|test) ;; *) echo 'Usage: bash scripts/aeron.sh [build|run|test]' >&2; exit 2 ;; esac
java_cmd=java
javac_cmd=javac
if [[ -n "${JAVA_HOME:-}" ]]; then
  java_cmd="$JAVA_HOME/bin/java"
  javac_cmd="$JAVA_HOME/bin/javac"
fi
mkdir -p build/classes
find src -name '*.java' -print > build/sources.txt
"$javac_cmd" -encoding UTF-8 --release 21 -d build/classes @build/sources.txt
if [[ "$task" == run ]]; then
  "$java_cmd" -Dfile.encoding=UTF-8 -cp build/classes aeronpcd.driver.AirportDriver
elif [[ "$task" == test ]]; then
  "$java_cmd" -Dfile.encoding=UTF-8 -Djava.awt.headless=true -cp build/classes aeronpcd.test.TestRunner
fi
