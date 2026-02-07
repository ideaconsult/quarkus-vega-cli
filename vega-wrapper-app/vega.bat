@echo off
setlocal

set "VEGA_JAR=Vega-GUI-1.2.4.jar"
if defined VEGA_JAR_PATH set "VEGA_JAR=%VEGA_JAR_PATH%"

java -Djava.util.logging.manager=org.jboss.logmanager.LogManager -cp "vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar;%VEGA_JAR%" io.quarkus.runner.GeneratedMain %*
endlocal
