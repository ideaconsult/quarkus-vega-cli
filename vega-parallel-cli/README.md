# VEGA Parallel CLI

A parallel execution wrapper for the VEGA CLI that enables running multiple VEGA models concurrently using a configurable worker pool.

## Overview

This tool wraps the existing `vega-wrapper-app` JAR and executes multiple models in parallel, similar to GNU `parallel` but specifically designed for VEGA model execution. It uses Java's `ExecutorService` and `ProcessBuilder` for cross-platform subprocess management.

## Features

- **Parallel Execution**: Run multiple models concurrently with configurable worker pool
- **Progress Tracking**: Real-time progress display showing completed/failed/running models
- **Pass-through Options**: Supports all original VEGA CLI options
- **Cross-platform**: Works on Windows, Linux, and macOS
- **Auto-detection**: Automatically finds VEGA JAR in common locations
- **Per-model Output**: Creates separate output directories for each model

## Building

```bash
mvn clean package
```

This creates a runner JAR in `target/vega-parallel-cli-1.0.0-SNAPSHOT-runner.jar`

## Usage

### Basic Usage

```bash
java -jar vega-parallel-cli-1.0.0-SNAPSHOT-runner.jar \
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
- `--vega-jar <path>` - Path to VEGA JAR file (default: auto-detect)

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
java -jar vega-parallel-cli-1.0.0-SNAPSHOT-runner.jar \
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
java -jar vega-parallel-cli-1.0.0-SNAPSHOT-runner.jar \
  -i test.txt \
  --smilesfield="SMILES" \
  --idfield="SMILES" \
  -m MELTING_POINT \
  -o output \
  -f
```

#### Specify Custom VEGA JAR Path

```bash
java -jar vega-parallel-cli-1.0.0-SNAPSHOT-runner.jar \
  -i test.txt \
  -m models.txt \
  -o output \
  --vega-jar /path/to/vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar \
  --workers 8
```

## Model File Format

The model file should be tab-delimited with model keys in the first column:

```
MELTING_POINT	Description of model
BCF	Another model
MUTAGENICITY	Yet another model
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

### VEGA JAR Not Found

If auto-detection fails, specify the JAR path explicitly:

```bash
--vega-jar ../vega-wrapper-app/target/vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar
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
