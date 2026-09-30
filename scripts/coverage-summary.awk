# Prints per-package line and branch coverage from JaCoCo's jacoco.csv
function pct(covered, missed) {
    return covered + missed == 0 ? "-" : sprintf("%.1f%%", 100 * covered / (covered + missed))
}

BEGIN {
    FS = ","
    printf "\n%-20s %8s %9s\n", "PACKAGE", "LINES", "BRANCHES"
}

NR > 1 {
    pkg = $2
    sub(/^com\.hotelbooking\.?/, "", pkg)
    if (pkg == "") pkg = "(root)"
    branchMissed[pkg] += $6; branchCovered[pkg] += $7
    lineMissed[pkg] += $8;   lineCovered[pkg] += $9
}

END {
    for (pkg in lineMissed) {
        printf "%-20s %8s %9s\n", pkg, pct(lineCovered[pkg], lineMissed[pkg]), pct(branchCovered[pkg], branchMissed[pkg]) | "sort"
        totalLineMissed += lineMissed[pkg];     totalLineCovered += lineCovered[pkg]
        totalBranchMissed += branchMissed[pkg]; totalBranchCovered += branchCovered[pkg]
    }
    close("sort")
    printf "%-20s %8s %9s\n", "TOTAL", pct(totalLineCovered, totalLineMissed), pct(totalBranchCovered, totalBranchMissed)
}
