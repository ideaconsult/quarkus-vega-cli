# vega-wrapper-app Project

This project is a Quarkus command-line application that wraps the functionality of `VEGA-GUI.jar`.

## Requirements

### Runtime Requirements
- JDK 17+
- **VEGA-GUI JAR**: Version 1.2.4 is required to run this application. It is NOT bundled in the distribution.

### Build Requirements (for developers)
- Maven 3.8+

### Obtaining VEGA-GUI 1.2.4

Download `Vega-GUI-1.2.4.jar` from the official VEGA QSAR website:
- **Download page**: https://www.vegahub.eu/download/vega-qsar-download/
- Look for version 1.2.4 specifically

> [!NOTE]
> This wrapper has been tested with VEGA-GUI version 1.2.4. It may work with later versions but compatibility has not been verified.

## Licensing

This wrapper application code is dual-licensed and compatible with:
- MIT License
- Apache License 2.0
- GPL-3.0 (when distributed with VEGA-GUI)

**Important**: When you distribute the uber-jar together with VEGA-GUI 1.2.4, the combined distribution must comply with VEGA-GUI's GPL-3.0 license terms. The VEGA-GUI library is licensed under GPL-3.0.

## Building the application

The application can be packaged using:
```shell script
./mvnw package -DskipTests
```
It produces a single uber-jar:
*   `target/vega-wrapper-app-*-runner.jar`: The application uber-JAR containing all Quarkus dependencies (~20MB).

The `Vega-GUI` dependency is excluded and must be provided separately at runtime.

## Running the application

Since the `VEGA-GUI` dependency is externalized, you must use the provided launch scripts to run the application.

### Windows

1.  Ensure you have `Vega-GUI-1.2.4.jar` in the same directory as the script.
2.  Use `vega.bat` to run the application.
    *   By default, it looks for `Vega-GUI-1.2.4.jar` in the current directory.
    *   You can specify a custom path using the `VEGA_JAR_PATH` environment variable.

**Example:**
```cmd
rem Default location (same directory)
vega.bat vega --list-models -o models.txt

rem Custom location
set VEGA_JAR_PATH=C:\path\to\Vega-GUI.jar
vega.bat vega --list-models -o models.txt
```

### Linux / Mac

1.  Ensure you have `Vega-GUI-1.2.4.jar` in the same directory as the script.
2.  Use `vega.sh` to run the application.
    *   By default, it looks for `Vega-GUI-1.2.4.jar` in the current directory.
    *   You can specify a custom path using the `VEGA_JAR_PATH` environment variable.

**Example:**
```bash
# Default location (same directory)
./vega.sh vega --list-models -o models.txt

# Custom location
export VEGA_JAR_PATH=/path/to/Vega-GUI.jar
./vega.sh vega --list-models -o models.txt
```

## Configuration

The launch scripts (`vega.bat` and `vega.sh`) are designed for **deployment**.
They expect the following directory structure:

```text
.
├── vega.bat (or vega.sh)
├── vega-wrapper-app-*-runner.jar  (uber-jar with all Quarkus dependencies)
└── Vega-GUI-1.2.4.jar  (external VEGA JAR)
```

**To deploy:**
1. Copy `target/vega-wrapper-app-*-runner.jar` to your deployment directory
2. Place `Vega-GUI-1.2.4.jar` in the same directory
3. Copy `vega.bat` (or `vega.sh`) to your deployment directory

The scripts use `-cp` to include both JARs on the classpath.

You can override the Vega-GUI location by setting the `VEGA_JAR_PATH` environment variable.

## Development

To run in development mode, use `mvn quarkus:dev`. The `Vega-GUI` jar must be installed in your local Maven repository (with `provided` scope) for compilation.

## Native Image

Creating a native executable might require additional configuration due to the dynamic loading of the external JAR. The current setup focuses on JVM mode with external JAR.
