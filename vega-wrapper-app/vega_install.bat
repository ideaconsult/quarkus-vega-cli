@echo off
setlocal

REM === CONFIGURATION ===
set "JAR_PATH=lib\Vega-GUI-1.2.4.jar"
set "GROUP_ID=insilico.vega"
set "ARTIFACT_ID=Vega-GUI"
set "VERSION=1.2.4"

REM === EXECUTE ===
echo Installing %JAR_PATH% into local Maven repository...
mvn install:install-file ^
  -Dfile=%JAR_PATH% ^
  -DgroupId=%GROUP_ID% ^
  -DartifactId=%ARTIFACT_ID% ^
  -Dversion=%VERSION% ^
  -Dpackaging=jar

echo.
echo ✅ Done. You can now declare it in pom.xml like:
echo.
echo ^<dependency^>
echo     ^<groupId^>%GROUP_ID%^</groupId^>
echo     ^<artifactId^>%ARTIFACT_ID%^</artifactId^>
echo     ^<version^>%VERSION%^</version^>
echo ^</dependency^>

endlocal
pause
