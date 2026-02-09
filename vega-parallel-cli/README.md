# VEGA Parallel CLI

A parallel execution wrapper for the VEGA CLI that enables running multiple VEGA models concurrently using a configurable worker pool.

## Overview

This tool wraps the existing `vega-wrapper-app` JAR and executes multiple models in parallel, similar to GNU `parallel` but specifically designed for VEGA model execution. It uses Java's `ExecutorService` and `ProcessBuilder` for cross-platform subprocess management.

## Features

- **Parallel Execution**: Run multiple models concurrently with configurable worker pool
- **Progress Tracking**: Real-time progress display showing completed/failed/running models
- **Pass-through Options**: Supports all original VEGA CLI options
- **Cross-platform**: Works on Windows, Linux, and macOS
- **Auto-detection**: Automatically finds both VEGA wrapper JAR and Vega-GUI JAR in common locations
- **Shared Output**: All models write to the same output directory

## Building

```bash

mvn clean package
```

This creates a self-contained **uber-jar** (full jar with all dependencies) in `target/vega-parallel-cli-1.0.1-SNAPSHOT-runner.jar`. You can run this file on any machine with Java installed.

## Usage

### Basic Usage

```bash
java -jar vega-parallel-cli-1.0.1-SNAPSHOT-runner.jar vega \
  -i input.txt \
  --smilesfield="SMILES" \
  --idfield="ID" \
  -m models.txt \
  -o output_dir \
  --workers 4
```

### Options

#### Parallel Execution Options

- `--workers, -w <N>` - Number of parallel workers (default: number of CPU cores)
- `--vega-jar <path>` - Path to VEGA wrapper JAR file (default: auto-detect)
- `--vega-gui-jar <path>` - Path to Vega-GUI JAR file (default: auto-detect from `VEGA_JAR_PATH` env var or same directory as wrapper JAR)
- `--timeout <minutes>` - Timeout for single model execution in minutes (default: -1, no timeout)

#### VEGA Subcommand

The subcommand to execute can be passed as a positional argument:

```bash
java -jar vega-parallel-cli.jar [subcommand] [options] ...
```

- `[subcommand]` - Subcommand to execute (default: `vega`)

#### VEGA Options (Pass-through)

All original VEGA CLI options are supported:

- `-i, --input <file>` - Path to input SMILES or .txt file (required)
- `-m, --model <key|file>` - Model key or file with model keys (required)
- `-o, --output <dir>` - Output directory for all models (required)
- `-s, --smiles <smiles>` - Input SMILES
- `--smilesfield <name>` - Name of the column with SMILES (default: SMILES)
- `--idfield <name>` - Name of the column with molecule ID (default: ID)
- `-f, --fastmode` - Enable fast mode
- `-j, --jsonl` - Write JSONL format instead of TXT
- `-x, --maxrows <N>` - Max rows to process from file
- `-z, --reinicialize-model <N>` - Reinitialize model every N rows

### Examples

#### Execute Multiple Models with 4 Workers

```bash
java -jar vega-parallel-cli-1.0.1-SNAPSHOT-runner.jar vega \
  -i test.txt \
  --smilesfield="SMILES" \
  --idfield="SMILES" \
  -m models_selected.txt \
  -o output \
  --workers 4 \
  -f
```

#### Execute Single Model (No Parallelization)

```bash
java -jar vega-parallel-cli-1.0.1-SNAPSHOT-runner.jar vega \
  -i test.txt \
  --smilesfield="SMILES" \
  --idfield="SMILES" \
  -m MELTING_POINT \
  -o output \
  -f
```

#### Specify Custom JAR Paths

```bash
java -jar vega-parallel-cli-1.0.1-SNAPSHOT-runner.jar vega \
  -i test.txt \
  -m models.txt \
  -o output \
  --vega-jar /path/to/vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar \
  --vega-gui-jar /path/to/Vega-GUI-1.2.4.jar \
  --workers 8
```

#### Execute with Timeout

Set a timeout of 60 minutes for each model execution:

```bash
java -jar vega-parallel-cli-1.0.1-SNAPSHOT-runner.jar vega \
  -i test.txt \
  -m models.txt \
  -o output \
  --timeout 60
```

## Model File Format

The model file should be tab-delimited with model keys in the first column. **The first non-empty line is automatically skipped as a header.**

```
Key	name	enabled
MELTING_POINT	Description of model	TRUE
BCF	Another model	TRUE
MUTAGENICITY	Yet another model	TRUE
```

Only the first column is used; additional columns are ignored.

## Output Structure

All models write their output files to the same directory:

```
output_dir/
├── resultw_MELTING_POINT.txt
├── resultw_BCF.txt
└── resultw_MUTAGENICITY.txt
```

## Performance Considerations

- **Worker Count**: Set `--workers` based on your CPU cores and memory. More workers = faster execution but higher memory usage.
- **I/O Bound**: If models are I/O bound, you can use more workers than CPU cores.
- **Memory**: Each worker runs a separate JVM process, so monitor memory usage.

## Comparison with Original VEGA CLI

| Feature | Original VEGA CLI | VEGA Parallel CLI |
|---------|------------------|-------------------|
| Single model execution | ✓ | ✓ |
| Multiple models (sequential) | ✓ | ✓ |
| Multiple models (parallel) | ✗ | ✓ |
| Progress tracking | Limited | ✓ |
| Shared output directory | ✓ | ✓ |

## Troubleshooting

### VEGA JARs Not Found

If auto-detection fails, specify both JAR paths explicitly:

```bash
--vega-jar ../vega-wrapper-app/target/vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar \
--vega-gui-jar ../vega-wrapper-app/Vega-GUI-1.2.4.jar
```

Or set the `VEGA_JAR_PATH` environment variable to point to the Vega-GUI JAR:

```bash
export VEGA_JAR_PATH=/path/to/Vega-GUI-1.2.4.jar
```

### Out of Memory

Reduce the number of workers:

```bash
--workers 2
```

Or increase JVM memory:

```bash
java -Xmx4g -jar vega-parallel-cli-1.0.0-SNAPSHOT-runner.jar ...
```

## License

Same as the parent project (see LICENSE file in repository root).
