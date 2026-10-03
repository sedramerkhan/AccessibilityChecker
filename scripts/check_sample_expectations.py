#!/usr/bin/env python3
"""Compare the sample app's Lint results with the // EXPECT markers in its sources.

A bad line in the sample app carries a marker comment at the end of the line:

    Icon(Icons.Filled.Delete, contentDescription = null)  // EXPECT: ComposeMissingContentDescription

Several issue IDs can be listed, separated by commas. The script reads the Lint XML report
(run `./gradlew :sample-app:lintDebug` first) and checks both directions:

* missing:    a marker whose issue was not reported on that line (a false negative), and
* unexpected: one of our issues reported on a line without a matching marker (a false
  positive, for example on a "good" screen).

Only the project's own issue IDs are compared. They are read from the lint-rules sources
(every `Issue.create(id = "Compose...")`), so built-in Lint checks are ignored.

Exit code: 0 when everything matches, 1 on any mismatch, 2 when the report is missing.
"""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_REPORT = ROOT / "sample-app" / "build" / "reports" / "lint-results-debug.xml"
DEFAULT_SOURCES = ROOT / "sample-app" / "src"
RULE_SOURCES = ROOT / "lint-rules" / "src" / "main"

MARKER = re.compile(r"//\s*EXPECT:\s*([A-Za-z0-9_,\s]+?)\s*$")
ISSUE_ID = re.compile(r"""\bid\s*=\s*"(Compose\w+)\"""")


def known_issue_ids(rule_sources: Path) -> set[str]:
    """Issue IDs declared in the lint-rules module."""
    ids: set[str] = set()
    for path in rule_sources.rglob("*.kt"):
        ids.update(ISSUE_ID.findall(path.read_text(encoding="utf-8")))
    return ids


def expected_findings(sources: Path) -> set[tuple[str, int, str]]:
    """(relative file, line, issue id) for every // EXPECT marker under the sample sources."""
    found: set[tuple[str, int, str]] = set()
    for path in sorted(sources.resolve().rglob("*.kt")):
        relative = path.relative_to(ROOT).as_posix()
        for number, text in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            match = MARKER.search(text)
            if not match:
                continue
            for issue in match.group(1).split(","):
                if issue.strip():
                    found.add((relative, number, issue.strip()))
    return found


def reported_findings(report: Path, ids: set[str]) -> set[tuple[str, int, str]]:
    """(relative file, line, issue id) for every reported issue whose id is in ids."""
    found: set[tuple[str, int, str]] = set()
    sample_dir = ROOT / "sample-app"
    for issue in ET.parse(report).getroot().iter("issue"):
        issue_id = issue.get("id", "")
        if issue_id not in ids:
            continue
        location = issue.find("location")
        if location is None or location.get("line") is None:
            continue
        file_path = Path(location.get("file", ""))
        if not file_path.is_absolute():
            file_path = sample_dir / file_path
        try:
            relative = file_path.resolve().relative_to(ROOT).as_posix()
        except ValueError:
            relative = file_path.as_posix()
        found.add((relative, int(location.get("line")), issue_id))
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT, help="Lint XML report")
    parser.add_argument("--sources", type=Path, default=DEFAULT_SOURCES, help="sample app sources")
    args = parser.parse_args()

    if not args.report.is_file():
        print(f"Lint report not found: {args.report}")
        print("Run ./gradlew :sample-app:lintDebug first.")
        return 2

    ids = known_issue_ids(RULE_SOURCES)
    expected = expected_findings(args.sources)
    unknown = sorted({issue for _, _, issue in expected} - ids)
    reported = reported_findings(args.report, ids)

    missing = sorted(expected - reported)
    unexpected = sorted(reported - expected)

    for issue in unknown:
        print(f"UNKNOWN ID   {issue} is used in an EXPECT marker but is not declared in lint-rules")
    for file, line, issue in missing:
        print(f"MISSING      {file}:{line}  {issue}")
    for file, line, issue in unexpected:
        print(f"UNEXPECTED   {file}:{line}  {issue}")

    print(
        f"{len(ids)} known issue ids, {len(expected)} expected, {len(reported)} reported, "
        f"{len(missing)} missing, {len(unexpected)} unexpected"
    )
    return 1 if missing or unexpected or unknown else 0


if __name__ == "__main__":
    sys.exit(main())
