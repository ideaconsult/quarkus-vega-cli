# vega-wrapper-app Project

This project is a Quarkus command-line application that wraps the functionality of `VEGA-GUI.jar`.

## Requirements

- JDK 17+
- Maven 3.8+
- **VEGA-GUI JAR**: An external `Vega-GUI-<version>.jar` file is required to run this application. It is NOT bundled in the distribution.

## Building the application

The application can be packaged using:
```shell script
./mvnw package -DskipTests
```
It produces:
*   `target/vega-wrapper-app-*-runner.jar`: The application runner JAR.
*   `target/lib/`: A folder containing the application dependencies.

This structure allows you to separate the application code from its dependencies.

## Running the application

Since the `VEGA-GUI` dependency is externalized, you must use the provided launch scripts to run the application.

### Windows

1.  Ensure you have `Vega-GUI-1.2.4.jar` available.
2.  Use `vega.bat` to run the application.
    *   By default, it looks for `lib\Vega-GUI-1.2.4.jar`.
    *   You can specify a custom path using the `VEGA_JAR_PATH` environment variable.

**Example:**
```cmd
rem Default location (lib/Vega-GUI-1.2.4.jar)
vega.bat vega --list-models

rem Custom location
set VEGA_JAR_PATH=C:\path\to\Vega-GUI.jar
vega.bat vega --list-models
```

### Linux / Mac

1.  Ensure you have `Vega-GUI-1.2.4.jar` available.
2.  Use `vega.sh` to run the application.
    *   By default, it looks for `lib/Vega-GUI-1.2.4.jar`.
    *   You can specify a custom path using the `VEGA_JAR_PATH` environment variable.

**Example:**
```bash
# Default location (lib/Vega-GUI-1.2.4.jar)
./vega.sh vega --list-models

# Custom location
export VEGA_JAR_PATH=/path/to/Vega-GUI.jar
./vega.sh vega --list-models
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

If you want to run in dev mode, you might need to install the `Vega-GUI` jar to your local maven repository or ensure it is available on the classpath.

## Native Image

Creating a native executable might require additional configuration due to the dynamic loading of the external JAR. The current setup focuses on JVM mode with external JAR.
