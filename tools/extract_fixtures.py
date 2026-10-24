#!/usr/bin/env python3
"""Extract example 200 responses from the Mist Postman collection into test fixtures.

Usage: python3 tools/extract_fixtures.py mist-postman.json src/test/resources/fixtures op1 [op2 ...]

Each operation (by Postman request name / operationId) is written to <out>/<operationId>.json.
Re-run after updating mist-postman.json to refresh fixtures against the latest schema.
"""
import json
import sys
from pathlib import Path


def leaves(items):
    for item in items:
        if "item" in item:
            yield from leaves(item["item"])
        else:
            yield item


def main():
    collection, out_dir, *ops = sys.argv[1:]
    by_name = {item["name"]: item for item in leaves(json.load(open(collection))["item"])}
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    for op in ops:
        item = by_name.get(op)
        if item is None:
            sys.exit(f"unknown operation: {op}")
        body = next((r.get("body") for r in item.get("response", []) if r.get("code") == 200 and r.get("body")), None)
        if body is None:
            sys.exit(f"no 200 example for: {op}")
        (out / f"{op}.json").write_text(json.dumps(json.loads(body), indent=2) + "\n")
        print(f"wrote {op}.json")


if __name__ == "__main__":
    main()
