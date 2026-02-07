@echo off
setlocal enabledelayedexpansion

REM === CONFIGURATION ===
if not defined VEGA_JAR_PATH (
    if exist "lib\Vega-GUI-1.2.4.jar" (
        set "VEGA_JAR_PATH=lib\Vega-GUI-1.2.4.jar"
    ) else (
        echo Error: VEGA_JAR_PATH environment variable is not set and lib\Vega-GUI-1.2.4.jar not found.
        exit /b 1
    )
)

echo Using Vega JAR: %VEGA_JAR_PATH%

REM === CLASSPATH ===
REM Include the quarkus-app lib folder and the external JAR
set "CP=target\quarkus-app\quarkus-run.jar;target\quarkus-app\lib\main\*;target\quarkus-app\lib\boot\*;target\quarkus-app\app\*;target\quarkus-app\quarkus\*;%VEGA_JAR_PATH%"

REM === LAUNCH ===
java -Djava.util.logging.manager=org.jboss.logmanager.LogManager -cp "%CP%" io.quarkus.bootstrap.runner.QuarkusEntryPoint %*

endlocal
