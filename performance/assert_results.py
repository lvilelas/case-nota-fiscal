#!/usr/bin/env python3
import csv
import math
import os
import sys


def percentile(values, percentile_value):
    ordered = sorted(values)
    index = max(0, math.ceil((percentile_value / 100) * len(ordered)) - 1)
    return ordered[index]


result_file = sys.argv[1] if len(sys.argv) > 1 else "performance/results.jtl"
max_p95_ms = int(os.getenv("MAX_P95_MS", "1000"))
max_error_rate = float(os.getenv("MAX_ERROR_RATE", "0"))

with open(result_file, newline="", encoding="utf-8") as stream:
    rows = list(csv.DictReader(stream))

if not rows:
    raise SystemExit("Nenhuma amostra encontrada no resultado do JMeter")

elapsed = [int(row["elapsed"]) for row in rows]
errors = sum(row["success"].lower() != "true" for row in rows)
p95_ms = percentile(elapsed, 95)
error_rate = errors * 100 / len(rows)

print(f"amostras={len(rows)} p95_ms={p95_ms} error_rate={error_rate:.2f}%")

if p95_ms > max_p95_ms:
    raise SystemExit(f"p95 de {p95_ms} ms excedeu o limite de {max_p95_ms} ms")
if error_rate > max_error_rate:
    raise SystemExit(f"taxa de erro de {error_rate:.2f}% excedeu o limite de {max_error_rate:.2f}%")
