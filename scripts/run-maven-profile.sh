#!/bin/bash
#
# Maven performance profiling script for OpenClaw Java
#
# Usage: ./scripts/run-maven-profile.sh [test|compile|package] [--output-dir <dir>]
#

set -e

# Parse arguments
MODE="${1:-test}"
OUTPUT_DIR=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        *)
            shift
            ;;
    esac
done

# Validate mode
if [[ "$MODE" != "test" && "$MODE" != "compile" && "$MODE" != "package" ]]; then
    echo "Usage: $0 [test|compile|package] [--output-dir <dir>]"
    exit 1
fi

# Set output directory
if [[ -z "$OUTPUT_DIR" ]]; then
    OUTPUT_DIR="/tmp/openclaw-maven-profile-$(date +%Y%m%d-%H%M%S)-$MODE"
fi

mkdir -p "$OUTPUT_DIR"

echo "[run-maven-profile] Running Maven $MODE with profiling..."
echo "[run-maven-profile] Output directory: $OUTPUT_DIR"

# Run Maven with profiling
# Using MAVEN_OPTS to enable JVM profiling
export MAVEN_OPTS="-Xmx4g -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints"

# For compilation profiling
if [[ "$MODE" == "compile" ]]; then
    echo "[run-maven-profile] Profiling compilation phase..."
    time mvn clean compile -DskipTests \
        -Dmaven.compiler.showCompilationTimes=true \
        -Dmaven.compiler.showWarnings=false \
        2>&1 | tee "$OUTPUT_DIR/maven-compile.log"
fi

# For test profiling
if [[ "$MODE" == "test" ]]; then
    echo "[run-maven-profile] Profiling test execution..."
    time mvn test \
        -Dmaven.test.failure.ignore=true \
        -DtrimStackTrace=false \
        2>&1 | tee "$OUTPUT_DIR/maven-test.log"
fi

# For package profiling
if [[ "$MODE" == "package" ]]; then
    echo "[run-maven-profile] Profiling package phase..."
    time mvn clean package -DskipTests \
        2>&1 | tee "$OUTPUT_DIR/maven-package.log"
fi

# Generate summary
echo ""
echo "[run-maven-profile] Profile complete!"
echo "[run-maven-profile] Output directory: $OUTPUT_DIR"
echo ""
echo "Files generated:"
ls -la "$OUTPUT_DIR/"

# Show timing summary if available
if [[ -f "$OUTPUT_DIR/maven-$MODE.log" ]]; then
    echo ""
    echo "Build log summary:"
    tail -20 "$OUTPUT_DIR/maven-$MODE.log"
fi
