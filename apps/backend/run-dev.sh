#!/usr/bin/env bash
set -euo pipefail

# Force Java 17 for Maven/Spring Boot regardless of user shell config
if [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Using JAVA_HOME=$JAVA_HOME"
mvn -v | head -n 3

exec mvn spring-boot:run "$@"

