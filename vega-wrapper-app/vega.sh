#!/bin/bash

VEGA_JAR="Vega-GUI-1.2.4.jar"
if [ -n "$VEGA_JAR_PATH" ]; then
    VEGA_JAR="$VEGA_JAR_PATH"
fi

java -Djava.util.logging.manager=org.jboss.logmanager.LogManager -cp "vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar:$VEGA_JAR" io.quarkus.runner.GeneratedMain "$@"
