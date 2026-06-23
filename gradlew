#!/bin/sh
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_JAR="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar"
exec java \
  -classpath "$WRAPPER_JAR" \
  org.gradle.wrapper.GradleWrapperMain "$@"
