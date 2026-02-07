# Quarkus VEGA CLI

A suite of command-line tools for running [VEGA QSAR](https://www.vegahub.eu/) models, built with Quarkus.

## Overview

This repository contains two complementary tools for working with VEGA (Virtual models for property Evaluation of chemicals within a Global Architecture):

1. **[vega-wrapper-app](vega-wrapper-app/README.md)** - A Quarkus CLI wrapper for VEGA-GUI that enables command-line execution of VEGA models
2. **[vega-parallel-cli](vega-parallel-cli/README.md)** - A parallel execution wrapper that runs multiple VEGA models concurrently

## Projects

### vega-wrapper-app

A Quarkus command-line application that wraps the functionality of `VEGA-GUI.jar`, enabling:
- Command-line execution of VEGA QSAR models
- Batch processing of SMILES files
- Scriptable model predictions
- Cross-platform support (Windows, Linux, macOS)

**[Read the full documentation →](vega-wrapper-app/README.md)**

### vega-parallel-cli

A parallel execution wrapper for the VEGA CLI that enables running multiple VEGA models concurrently:
- Configurable worker pool for parallel execution
- Real-time progress tracking
- Automatic VEGA JAR detection
- Shared output directory for all models

**[Read the full documentation →](vega-parallel-cli/README.md)**

## Quick Start

### 1. Build the Projects

```bash
# Build vega-wrapper-app
cd vega-wrapper-app
./mvnw package -DskipTests

# Build vega-parallel-cli
cd ../vega-parallel-cli
mvn package
```

### 2. Download VEGA-GUI

Download VEGA-GUI version 1.2.4:
- **Direct download**: https://www.vegahub.eu/vegahub-dwn/VEGA-GUI-1.2.4.zip
- **Download page**: https://www.vegahub.eu/download/vega-qsar-download/

Extract and locate `Vega-GUI-1.2.4.jar`.

### 3. Run Single Model (vega-wrapper-app)

```bash
cd vega-wrapper-app
./vega.sh vega --list-models -o models.txt
./vega.sh vega -i input.txt -m MELTING_POINT -o output
```

### 4. Run Multiple Models in Parallel (vega-parallel-cli)

```bash
cd vega-parallel-cli
java -jar target/vega-parallel-cli-*-runner.jar vega \
  -i input.txt \
  -m models.txt \
  -o output \
  --workers 4
```

## Architecture

```
quarkus-vega-cli/
├── vega-wrapper-app/       # Base CLI wrapper for VEGA-GUI
│   ├── vega.sh             # Linux/Mac launcher script
│   ├── vega.bat            # Windows launcher script
│   └── target/
│       └── *-runner.jar    # Uber-jar with Quarkus dependencies
│
└── vega-parallel-cli/      # Parallel execution wrapper
    └── target/
        └── *-runner.jar    # Self-contained uber-jar
```

## Requirements

- **JDK 17+** (runtime)
- **Maven 3.8+** (build only)
- **VEGA-GUI 1.2.4** (runtime, not bundled)

## License

This project is dual-licensed:
- MIT License
- Apache License 2.0
- GPL-3.0 (when distributed with VEGA-GUI)

See [LICENSE](LICENSE) for details.

## Related Links

- [VEGA QSAR Official Website](https://www.vegahub.eu/)
- [VEGA-GUI Download](https://www.vegahub.eu/download/vega-qsar-download/)
