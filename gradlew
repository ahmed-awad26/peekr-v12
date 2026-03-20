#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
  echo "Missing gradle/wrapper/gradle-wrapper.jar"
  echo "Run: gradle wrapper --gradle-version 8.6"
  exit 1
fi

JAVACMD=${JAVA_HOME:+$JAVA_HOME/bin/}java
if [ ! -x "$JAVACMD" ]; then
  JAVACMD=java
fi

exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
