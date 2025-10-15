#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="docs"
mkdir -p "$OUT_DIR"

# Architecture (whole repo)
claude /ask --no-apply --output "${OUT_DIR}/architecture.md" < docs/prompts/01_architecture.txt

# Module (rule-engine) — adjust module name if needed
claude /ask --no-apply --output "${OUT_DIR}/rule-engine.md" < docs/prompts/02_module_rule_engine.txt

echo "Generated: docs/architecture.md, docs/rule-engine.md"

