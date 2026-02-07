#!/bin/bash

# === CONFIGURATION ===
if [ -z "$VEGA_JAR_PATH" ]; then
    if [ -f "lib/Vega-GUI-1.2.4.jar" ]; then
        VEGA_JAR_PATH="lib/Vega-GUI-1.2.4.jar"
    else
        echo "Error: VEGA_JAR_PATH environment variable is not set and lib/Vega-GUI-1.2.4.jar not found."
        exit 1
    fi
fi

echo "Using Vega JAR: $VEGA_JAR_PATH"

# === CLASSPATH ===
# Include the quarkus-app lib folder and the external JAR
# Note: Separator is : on Unix/Linux
CP="target/quarkus-app/quarkus-run.jar:target/quarkus-app/lib/main/*:target/quarkus-app/lib/boot/*:target/quarkus-app/app/*:target/quarkus-app/quarkus/*:$VEGA_JAR_PATH"

# === LAUNCH ===
java -Djava.util.logging.manager=org.jboss.logmanager.LogManager -cp "$CP" io.quarkus.bootstrap.runner.QuarkusEntryPoint "$@"
