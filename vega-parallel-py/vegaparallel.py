#!/usr/bin/env python3
import argparse
import subprocess
import sys
from pathlib import Path
from concurrent.futures import ProcessPoolExecutor, as_completed


def load_models(model_key):
    p = Path(model_key)
    if p.exists():
        models = []
        for line in p.read_text().splitlines():
            if not line.strip():
                continue
            key = line.split("\t")[0].strip()
            if key:
                models.append(key)
        return models
    else:
        return [model_key]


def run_model(args, model):
    """
    Runs a VEGA model as a subprocess. Returns (model, exit_code).
    """
    cmd = [
        "java", "-jar", args.wrapper_jar,
        "vega",
        "-i", args.input,
        "-o", args.output,
        "-m", model,
    ]

    if args.fast:        cmd.append("-f")
    if args.jsonl:       cmd.append("-j")
    if args.listmodels:  cmd.append("-l")
    if args.idfield:     cmd.append(f"--idfield={args.idfield}")
    if args.smilesfield: cmd.append(f"--smilesfield={args.smilesfield}")
    if args.maxrows:     cmd.extend(["-x", str(args.maxrows)])
    if args.reinit:      cmd.extend(["-z", str(args.reinit)])
    if args.smiles:      cmd.extend(["-s", args.smiles])

    print(f"[{model}] START:", " ".join(cmd), flush=True)

    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True
    )

    for line in proc.stdout:
        print(f"[{model}] {line}", end="", flush=True)

    proc.wait()
    print(f"[{model}] EXIT {proc.returncode}", flush=True)
    return model, proc.returncode


def main():
    parser = argparse.ArgumentParser(description="Parallel VEGA wrapper (Python).")
    parser.add_argument("--workers", type=int, default=1, help="Parallel workers")
    parser.add_argument("--wrapper-jar", required=True, help="vega-wrapper-app jar")
    parser.add_argument("-i", "--input", required=True, help="Input file")
    parser.add_argument("-o", "--output", required=True, help="Output directory")
    parser.add_argument("-m", "--model", required=True, help="Model key or file of keys")

    # optional flags matching VEGA wrapper
    parser.add_argument("--idfield")
    parser.add_argument("--smilesfield")
    parser.add_argument("-f", "--fast", action="store_true")
    parser.add_argument("-j", "--jsonl", action="store_true")
    parser.add_argument("-l", "--listmodels", action="store_true")
    parser.add_argument("-x", "--maxrows", type=int)
    parser.add_argument("-z", "--reinit", type=int)
    parser.add_argument("-s", "--smiles")

    args = parser.parse_args()

    models = load_models(args.model)
    print(f"Loaded {len(models)} models.")

    futures = []
    with ProcessPoolExecutor(max_workers=args.workers) as pool:
        for m in models:
            futures.append(pool.submit(run_model, args, m))

        for fut in as_completed(futures):
            model, code = fut.result()
            if code != 0:
                print(f"[{model}] FAILED with exit code {code}", file=sys.stderr)

    print("All models processed.")


if __name__ == "__main__":
    main()
