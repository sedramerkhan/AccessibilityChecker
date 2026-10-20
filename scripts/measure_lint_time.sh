#!/usr/bin/env bash
# Measure what the custom rules add to a Lint run (Plan section 6.5).
#
# Runs `:sample-app:lintDebug --rerun-tasks` several times with the rules and several times
# without them, and reports the added time. Without the rules means the `lintChecks` dependency
# is not applied at all (`-Pa11ylint.off=true`), not merely that the issues are disabled, so the
# figure covers loading the jar and running the detectors.
#
# The rules jar is built once before timing and left out of every timed run
# (`-x :lint-rules:jar`, which also drops its compile tasks). Otherwise `--rerun-tasks` would
# recompile all the detectors from Kotlin in every "with" run, a cost a project that uses the
# published library never pays. Until 2026-10-19 that compile was included (see DECISIONS).
#
# The run order alternates rather than doing all the "with" runs first, and within each pair the
# side that goes first alternates too (with/without, then without/with). A machine that warms up
# or throttles during the measurement then affects both sides equally instead of biasing one.
#
# Each pair starts from a fresh Gradle daemon (`--stop`) followed by an untimed warm-up of both
# sides. Without this the daemon carried state from run to run: on 2026-10-19 the first four pairs
# were stable and then both sides slowed down for good from the fifth, which points at the
# daemon's 2 GB heap filling up (or the laptop throttling), not at the rules. A fresh daemon per
# pair makes every pair independent of the ones before it.
#
# Every repetition is printed, not only the mean. Wall clock on a developer machine is not
# reliable in a single sample: a suspend or a background task lands entirely in whichever run is
# in flight, and on this project one such stall was observed adding hours to a single unit test
# (see docs/PROGRESS.md, 2026-10-13). Reading the spread is how that is spotted, so the script
# reports min, median and max, and warns when the spread is wide enough to doubt the mean.
#
# Only POSIX tools and awk are used, no bc: bc is not present in Git Bash on Windows, which is
# where this project is developed.
#
# Usage: scripts/measure_lint_time.sh [repetitions]   (default 5)

set -u

cd "$(dirname "$0")/.." || exit 2

REPS="${1:-5}"
GRADLE="./gradlew"
TASK=":sample-app:lintDebug"

if ! [ "$REPS" -ge 2 ] 2>/dev/null; then
    echo "Repetitions must be a number, 2 or more (got: $REPS)" >&2
    exit 2
fi

# Milliseconds of wall clock for one full Lint run. --rerun-tasks forces the work to happen
# again; without it Gradle would report an up-to-date task and time almost nothing.
run_lint() {
    local start end
    start=$(date +%s%3N)
    if ! "$GRADLE" "$TASK" --rerun-tasks -x :lint-rules:jar -q "$@" > /dev/null 2>&1; then
        echo "A Lint run failed. Run the task by hand to see why." >&2
        return 1
    fi
    end=$(date +%s%3N)
    echo $((end - start))
}

# mean, median, trimmed mean, min and max of the numbers on stdin, in milliseconds.
#
# The trimmed mean drops the fastest and the slowest run before averaging, which is the usual
# way to report a benchmark on a machine that is not dedicated to it: one suspend, one antivirus
# sweep or one background build lands in a single run and would otherwise move the mean more
# than the thing being measured. It needs at least 5 runs to mean anything, so below that it is
# the plain mean.
summarise() {
    sort -n | awk '
        { v[n++] = $1; total += $1 }
        END {
            if (n == 0) { print "0 0 0 0 0"; exit }
            median = (n % 2) ? v[(n - 1) / 2] : (v[n / 2 - 1] + v[n / 2]) / 2
            trimmed = total / n
            if (n >= 5) {
                inner = 0
                for (i = 1; i < n - 1; i++) inner += v[i]
                trimmed = inner / (n - 2)
            }
            printf "%.1f %.1f %.1f %d %d", total / n, median, trimmed, v[0], v[n - 1]
        }'
}

echo "Measuring $TASK, $REPS repetitions each way."
echo "Building the rules jar once; the timed runs reuse it instead of recompiling the rules."
if ! "$GRADLE" :lint-rules:jar -q > /dev/null 2>&1; then
    echo "Building :lint-rules:jar failed. Run it by hand to see why." >&2
    exit 1
fi
echo "Each pair starts from a fresh Gradle daemon and an untimed warm-up of both sides."

with=()
without=()
for i in $(seq 1 "$REPS"); do
    "$GRADLE" --stop -q > /dev/null 2>&1
    run_lint > /dev/null || exit 1
    run_lint -Pa11ylint.off=true > /dev/null || exit 1
    if [ $((i % 2)) -eq 1 ]; then
        a=$(run_lint) || exit 1
        b=$(run_lint -Pa11ylint.off=true) || exit 1
    else
        b=$(run_lint -Pa11ylint.off=true) || exit 1
        a=$(run_lint) || exit 1
    fi
    with+=("$a")
    without+=("$b")
    printf "  run %d of %d: with %6d ms   without %6d ms   added %+6d ms\n" \
        "$i" "$REPS" "$a" "$b" "$((a - b))"
done

read -r w_mean w_median w_trim w_min w_max <<< "$(printf '%s\n' "${with[@]}" | summarise)"
read -r o_mean o_median o_trim o_min o_max <<< "$(printf '%s\n' "${without[@]}" | summarise)"

echo
awk -v wm="$w_mean" -v wd="$w_median" -v wt="$w_trim" -v wlo="$w_min" -v whi="$w_max" \
    -v om="$o_mean" -v od="$o_median" -v ot="$o_trim" -v olo="$o_min" -v ohi="$o_max" '
BEGIN {
    printf "%-12s %9s %9s %9s %9s %9s\n", "", "trimmed", "median", "mean", "min", "max"
    printf "%-12s %8.2fs %8.2fs %8.2fs %8.2fs %8.2fs\n", "with rules", wt/1000, wd/1000, wm/1000, wlo/1000, whi/1000
    printf "%-12s %8.2fs %8.2fs %8.2fs %8.2fs %8.2fs\n", "without",    ot/1000, od/1000, om/1000, olo/1000, ohi/1000
    printf "\nAdded by the rules: %.2f s trimmed, %.2f s median, %.2f s mean", (wt-ot)/1000, (wd-od)/1000, (wm-om)/1000
    if (ot > 0) printf " (%.1f%% of a trimmed run without them)", 100*(wt-ot)/ot
    printf ".\nQuote the trimmed figure, with the number of repetitions.\n"
}'

# A spread wider than the effect being measured means the mean is not worth quoting on its own.
awk -v wlo="$w_min" -v whi="$w_max" -v wm="$w_mean" -v om="$o_mean" '
BEGIN {
    spread = whi - wlo
    added = wm - om
    if (added < 0) added = -added
    if (spread > added) {
        printf "\nWarning: the runs with the rules vary by %.2f s between fastest and slowest,\n", spread/1000
        printf "which is wider than the %.2f s difference being measured. Quote the median, say how\n", added/1000
        print "many repetitions were used, and re-run on an otherwise idle machine before putting"
        print "the number in the thesis."
    }
}'
