#!/usr/bin/env python3
"""Convert a Lint XML report into the JSON format Phase 3 and the evaluation read.

One object per finding, in the format CLAUDE.md section 8 fixes:

    {"taxonomyId": "P-01", "issueId": "ComposeMissingContentDescription",
     "severity": "Error", "file": "path/Screen.kt", "line": 42, "column": 9,
     "message": "...", "detection": "STATIC"}

This format is a contract. Phase 3 reads it, so fields are never renamed or removed here
without changing Phase 3 too (CLAUDE.md section 13).

Only this project's findings are converted. A finding is recognised by the `[P-01]` prefix
that every rule's message carries, so built-in Lint checks in the same report are skipped.

`detection` is not in the Lint report, so it is read from `Taxonomy.kt`, which CLAUDE.md
section 5.1 makes the single source of truth for rule metadata. The taxonomy ID of each entry
is derived from the detector class it names (`P01MissingContentDescriptionDetector` is
`P-01`), because the entry refers to the detector's constant rather than spelling the ID out.

Several reports can be converted at once, which is what the evaluation corpus needs:

    py scripts/lint_xml_to_json.py --out findings.json \\
        ../corpus/app-a/app/build/reports/lint-results-debug.xml \\
        ../corpus/app-b/app/build/reports/lint-results-debug.xml

Exit code: 0 on success, 1 when a report names a taxonomy ID the taxonomy does not have,
2 when a report or the taxonomy file is missing.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_REPORT = ROOT / "sample-app" / "build" / "reports" / "lint-results-debug.xml"
TAXONOMY = ROOT / "lint-rules" / "src" / "main" / "kotlin" / "org" / "svu" / "sedra" / "a11ylint" / "taxonomy" / "Taxonomy.kt"

#: The `[P-01]` prefix every rule's message starts with (CLAUDE.md section 5.2).
TAXONOMY_PREFIX = re.compile(r"^\[([PORU]-\d{2})\]\s*")

#: One `TaxonomyEntry(...)` block: the detector class it names, and its detection type.
ENTRY = re.compile(
    r"TaxonomyEntry\s*\(\s*id\s*=\s*([A-Za-z0-9_]+)\.TAXONOMY_ID.*?detection\s*=\s*DetectionType\.(\w+)",
    re.DOTALL,
)

#: `P01MissingContentDescriptionDetector` -> `P-01`.
DETECTOR_ID = re.compile(r"^([PORU])(\d{2})")


def detection_by_taxonomy_id(taxonomy: Path) -> dict[str, str]:
    """Maps `P-01` to `STATIC` or `STATIC_LLM`, read from Taxonomy.kt."""
    source = taxonomy.read_text(encoding="utf-8")
    table: dict[str, str] = {}
    for detector, detection in ENTRY.findall(source):
        match = DETECTOR_ID.match(detector)
        if match:
            table[f"{match.group(1)}-{match.group(2)}"] = detection
    return table


def relative_file(path: str, root: Path) -> str:
    """The path as written in the report, made relative to [root] when it is inside it.

    Paths are normalised to forward slashes so a report produced on Windows and one produced
    on Linux give the same JSON for the same finding.
    """
    try:
        return Path(path).resolve().relative_to(root).as_posix()
    except ValueError:
        return Path(path).as_posix()


def findings(report: Path, detection: dict[str, str], root: Path) -> tuple[list[dict], set[str]]:
    """Converts one report. Returns the findings and any taxonomy IDs it did not recognise."""
    results: list[dict] = []
    unknown: set[str] = set()
    for issue in ET.parse(report).getroot().findall("issue"):
        message = issue.get("message", "")
        match = TAXONOMY_PREFIX.match(message)
        if not match:
            continue
        taxonomy_id = match.group(1)
        if taxonomy_id not in detection:
            unknown.add(taxonomy_id)
        # The first location is the reported one; later ones are secondary context.
        location = issue.find("location")
        if location is None:
            continue
        results.append(
            {
                "taxonomyId": taxonomy_id,
                "issueId": issue.get("id", ""),
                "severity": issue.get("severity", ""),
                "file": relative_file(location.get("file", ""), root),
                "line": int(location.get("line", 0)),
                "column": int(location.get("column", 0)),
                "message": message,
                "detection": detection.get(taxonomy_id, ""),
            }
        )
    return results, unknown


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("reports", nargs="*", type=Path, default=[DEFAULT_REPORT],
                        help="Lint XML reports (default: the sample app's)")
    parser.add_argument("--out", type=Path, help="write here instead of standard output")
    parser.add_argument("--root", type=Path, default=ROOT,
                        help="paths are reported relative to this directory (default: the repository)")
    parser.add_argument("--taxonomy", type=Path, default=TAXONOMY, help="Taxonomy.kt")
    parser.add_argument("--compact", action="store_true", help="one line, instead of indented")
    args = parser.parse_args()

    if not args.taxonomy.is_file():
        print(f"Taxonomy not found: {args.taxonomy}", file=sys.stderr)
        return 2
    detection = detection_by_taxonomy_id(args.taxonomy)
    if not detection:
        print(f"No taxonomy entries found in {args.taxonomy}", file=sys.stderr)
        return 2

    root = args.root.resolve()
    results: list[dict] = []
    unknown: set[str] = set()
    for report in args.reports:
        if not report.is_file():
            print(f"Report not found: {report}", file=sys.stderr)
            print("Run ./gradlew :sample-app:lintDebug first.", file=sys.stderr)
            return 2
        found, missing = findings(report, detection, root)
        results.extend(found)
        unknown |= missing

    # Stable order, so two runs of the same code give byte-identical output and a diff of two
    # corpus runs shows only what really changed.
    results.sort(key=lambda r: (r["file"], r["line"], r["column"], r["taxonomyId"]))

    text = json.dumps(results, indent=2 if not args.compact else None, ensure_ascii=False)
    if args.out:
        args.out.write_text(text + "\n", encoding="utf-8")
        print(f"{len(results)} findings written to {args.out}", file=sys.stderr)
    else:
        print(text)

    if unknown:
        print(f"Taxonomy IDs not found in {args.taxonomy.name}: {', '.join(sorted(unknown))}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
