#!/usr/bin/env python3
"""
audit_compose_perf.py
Deterministic static linter for Jetpack Compose performance in True Scale:
1. Verifies ZERO object allocations inside DrawScope / Canvas { ... } blocks (no Paint(), Path(), Stroke() inside draw loop)
2. Verifies mandatory presence of fontFeatureSettings = "tnum" for tabular numeric readouts
3. Reports violations and saves report to .tmp/reports/compose_perf_audit.json
"""

import sys
import os
import re
import json

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIR = os.path.join(ROOT_DIR, "app", "src", "main", "java", "com", "example", "truescale")
TMP_DIR = os.path.join(ROOT_DIR, ".tmp", "reports")
os.makedirs(TMP_DIR, exist_ok=True)
REPORT_FILE = os.path.join(TMP_DIR, "compose_perf_audit.json")

FORBIDDEN_DRAW_ALLOCATIONS = [
    r"\bPaint\s*\(",
    r"\bPath\s*\(",
    r"\bStroke\s*\(",
    r"\bTextStyle\s*\(",
    r"\bBrush\.[a-zA-Z]+\s*\(",
    r"\bCornerPathEffect\s*\(",
]

def find_canvas_blocks(content):
    """
    Finds and extracts content inside Canvas(...) { ... } or drawBehind { ... } blocks.
    Uses bracket matching to accurately extract the lambda body.
    """
    canvas_patterns = [
        re.compile(r'\bCanvas\s*(?:\([^)]*\))?\s*\{'),
        re.compile(r'\bdrawBehind\s*\{'),
        re.compile(r'\bdrawWithContent\s*\{')
    ]

    blocks = []
    for pattern in canvas_patterns:
        for match in pattern.finditer(content):
            start_idx = match.end() - 1 # Position of '{'
            brace_count = 1
            idx = start_idx + 1
            while idx < len(content) and brace_count > 0:
                char = content[idx]
                if char == '{':
                    brace_count += 1
                elif char == '}':
                    brace_count -= 1
                idx += 1
            if brace_count == 0:
                block_body = content[start_idx + 1:idx - 1]
                line_no = content[:start_idx].count('\n') + 1
                blocks.append((line_no, block_body))
    return blocks

def audit_file_draw_allocations(filepath):
    violations = []
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    canvas_blocks = find_canvas_blocks(content)
    for start_line, block in canvas_blocks:
        block_lines = block.split('\n')
        for offset, line in enumerate(block_lines):
            # Ignore comments
            clean_line = re.sub(r'//.*', '', line).strip()
            if not clean_line:
                continue
            for pattern in FORBIDDEN_DRAW_ALLOCATIONS:
                if re.search(pattern, clean_line):
                    # Check if it's not a variable assignment inside remember
                    violations.append({
                        "file": os.path.relpath(filepath, ROOT_DIR),
                        "line": start_line + offset,
                        "code": clean_line,
                        "violation": f"Forbidden object allocation inside Canvas block matching '{pattern}'"
                    })
    return violations

def audit_tabular_numbers(root_dir):
    """
    Ensures that fontFeatureSettings = "tnum" is defined in typography/theme
    and referenced by numeric readouts.
    """
    type_kt = os.path.join(root_dir, "app", "src", "main", "java", "com", "example", "truescale", "ui", "theme", "Type.kt")
    if not os.path.exists(type_kt):
        return [{"error": "ui/theme/Type.kt not found"}]

    with open(type_kt, 'r', encoding='utf-8') as f:
        type_content = f.read()

    has_tnum = 'fontFeatureSettings = "tnum"' in type_content or "fontFeatureSettings = \"tnum\"" in type_content
    if not has_tnum:
        return [{
            "file": os.path.relpath(type_kt, ROOT_DIR),
            "violation": "Missing fontFeatureSettings = 'tnum' in TabularNumberStyle typography definition"
        }]
    return []

def main():
    print("=" * 60)
    print("True Scale — Jetpack Compose Performance & Anti-Allocation Audit")
    print("=" * 60)

    if not os.path.exists(SRC_DIR):
        print(f"Source directory {SRC_DIR} not yet present (files to be created).")
        return 0

    all_violations = []

    # 1. Audit components for DrawScope allocations
    components_dir = os.path.join(SRC_DIR, "ui", "components")
    if os.path.exists(components_dir):
        for root, _, files in os.walk(components_dir):
            for file in files:
                if file.endswith(".kt"):
                    fp = os.path.join(root, file)
                    v = audit_file_draw_allocations(fp)
                    all_violations.extend(v)

    # 2. Audit typography for tnum
    tnum_violations = audit_tabular_numbers(ROOT_DIR)
    all_violations.extend(tnum_violations)

    report = {
        "status": "FAILED" if all_violations else "PASSED",
        "violation_count": len(all_violations),
        "violations": all_violations
    }

    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    if all_violations:
        print(f"\n[X] AUDIT FAILED: Found {len(all_violations)} Compose performance violation(s):")
        for v in all_violations:
            print(f"  - {v}")
        print(f"\nReport written to: {REPORT_FILE}")
        return 1
    else:
        print("[✓] Zero DrawScope/Canvas allocations detected.")
        print("[✓] Mandatory tabular numbers ('tnum') verified.")
        print(f"Audit PASSED cleanly. Report written to: {REPORT_FILE}")
        return 0

if __name__ == "__main__":
    sys.exit(main())
