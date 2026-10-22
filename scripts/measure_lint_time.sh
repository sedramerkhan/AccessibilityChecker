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
# The added time is analysed per pair: each pair's "with" minus its "without". Background load
# that slows the machine for a while slows both runs of a pair, so it cancels in the difference
# while it inflates the raw times. The summary therefore leads with the median of the per-pair
# differences and a 95% confidence interval for their mean, and the warning fires when that
# interval is wide or includes zero. Until 2026-10-22 the warning compared the spread of the raw
# "with" times with the difference, which fired on runs whose pairs agreed closely (see DECISIONS).
#
# Usage: scripts/measure_lint_time.sh [repetitions]   (default 5)
#        scripts/measure_lint_time.sh --analyse "<with ms ...>" "<without ms ...>"
#            re-runs only the analysis on times from an earlier run, without running Lint

set -u

cd "$(dirname "$0")/.." || exit 2

REPS="${1:-5}"
GRADLE="./gradlew"
TASK=":sample-app:lintDebug"

if [ "$REPS" != "--analyse" ] && ! [ "$REPS" -ge 2 ] 2>/dev/null; then
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

# Prints the summary for two space-separated lists of milliseconds, "with" and "without", given in
# pair order (the n-th entry of each list is one pair).
report() {
    local with_list="$1" without_list="$2"
    local w_mean w_median w_trim w_min w_max o_mean o_median o_trim o_min o_max
    read -r w_mean w_median w_trim w_min w_max <<< "$(printf '%s\n' $with_list | summarise)"
    read -r o_mean o_median o_trim o_min o_max <<< "$(printf '%s\n' $without_list | summarise)"

    echo
    awk -v wm="$w_mean" -v wd="$w_median" -v wt="$w_trim" -v wlo="$w_min" -v whi="$w_max" \
        -v om="$o_mean" -v od="$o_median" -v ot="$o_trim" -v olo="$o_min" -v ohi="$o_max" '
    BEGIN {
        printf "Raw times (background load moves these; see the per-pair figures below)\n"
        printf "%-12s %9s %9s %9s %9s %9s\n", "", "trimmed", "median", "mean", "min", "max"
        printf "%-12s %8.2fs %8.2fs %8.2fs %8.2fs %8.2fs\n", "with rules", wt/1000, wd/1000, wm/1000, wlo/1000, whi/1000
        printf "%-12s %8.2fs %8.2fs %8.2fs %8.2fs %8.2fs\n", "without",    ot/1000, od/1000, om/1000, olo/1000, ohi/1000
    }'

    # Per-pair differences: median, trimmed mean, mean, standard deviation, and a 95% confidence
    # interval for the mean using Student's t (two-sided, n - 1 degrees of freedom).
    awk -v w="$with_list" -v o="$without_list" -v od="$o_median" '
    BEGIN {
        n = split(w, a, " "); m = split(o, b, " ")
        if (n != m || n < 2) { print "Need at least two complete pairs." > "/dev/stderr"; exit 2 }
        for (i = 1; i <= n; i++) { d[i] = a[i] - b[i]; sum += d[i]; if (d[i] > 0) positive++ }
        mean = sum / n
        for (i = 1; i <= n; i++) ss += (d[i] - mean) ^ 2
        sd = sqrt(ss / (n - 1))
        # sort the differences (n is small)
        for (i = 1; i <= n; i++) s[i] = d[i]
        for (i = 1; i <= n; i++) for (j = i + 1; j <= n; j++) if (s[j] < s[i]) { x = s[i]; s[i] = s[j]; s[j] = x }
        median = (n % 2) ? s[(n + 1) / 2] : (s[n / 2] + s[n / 2 + 1]) / 2
        trimmed = mean
        if (n >= 5) { inner = 0; for (i = 2; i < n; i++) inner += s[i]; trimmed = inner / (n - 2) }
        split("12.706 4.303 3.182 2.776 2.571 2.447 2.365 2.306 2.262 2.228 2.201 2.179 2.160 2.145 2.131 2.120 2.110 2.101 2.093", tt, " ")
        df = n - 1
        tv = (df <= 19) ? tt[df] : 1.96
        half = tv * sd / sqrt(n)
        lo = mean - half; hi = mean + half

        printf "\nAdded by the rules, per pair (%d pairs, %d of them positive):\n", n, positive
        printf "  median %.2f s   trimmed mean %.2f s   mean %.2f s   sd %.2f s\n", median/1000, trimmed/1000, mean/1000, sd/1000
        printf "  95%% confidence interval for the mean: %.2f s to %.2f s\n", lo/1000, hi/1000
        if (od > 0) printf "  the median is %.1f%% of a median run without the rules (%.2f s)\n", 100 * median / od, od/1000
        printf "Quote the per-pair median with the confidence interval and the number of pairs.\n"

        if (lo <= 0) {
            printf "\nWarning: the confidence interval includes zero, so these runs do not show that the\n"
            print "rules add measurable time. Re-run on a quieter machine or with more pairs."
        } else if (half > mean / 2) {
            printf "\nWarning: the confidence interval is wider than the effect (+/- %.2f s around %.2f s).\n", half/1000, mean/1000
            print "The pairs disagree too much to quote one figure. Re-run on a quieter machine or with"
            print "more pairs."
        }
    }'
}

if [ "${1:-}" = "--analyse" ]; then
    if [ $# -ne 3 ]; then
        echo "Usage: $0 --analyse \"<with ms ...>\" \"<without ms ...>\"" >&2
        exit 2
    fi
    report "$2" "$3"
    exit $?
fi

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

report "${with[*]}" "${without[*]}"
