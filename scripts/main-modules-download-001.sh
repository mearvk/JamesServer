!/usr/bin/env bash

set -Eeuo pipefail

echo "=============================================="
echo " MEARVK EMAIL SERVER SOURCE RECOVERY"
echo "=============================================="
echo

BASE="$PWD/mearvk-recovery"
MEARVK="$BASE/MearvkEmailServer"
JAMES="$BASE/james-project"

MEARVK_URL="https://github.com/mearvk/MearvkEmailServer.git"
JAMES_URL="https://github.com/apache/james-project.git"
JAMES_TAG="james-project-3.9.0"

echo "[1/7] Creating working directory..."
mkdir -p "$BASE"

echo "[2/7] Downloading MEARVK repository..."

if [ -d "$MEARVK/.git" ]; then
    echo "      MEARVK already exists."
else
    git clone "$MEARVK_URL" "$MEARVK"
fi

echo
echo "[3/7] Downloading Apache James 3.9.0..."

if [ -d "$JAMES/.git" ]; then
    echo "      Apache James already exists."
else
    git clone \
        --branch "$JAMES_TAG" \
        --depth 1 \
        "$JAMES_URL" \
        "$JAMES"
fi

echo
echo "[4/7] Verifying source trees..."

if [ ! -d "$MEARVK" ]; then
    echo "ERROR: MEARVK source was not downloaded."
    exit 1
fi

if [ ! -d "$JAMES" ]; then
    echo "ERROR: Apache James source was not downloaded."
    exit 1
fi

echo "      MEARVK: OK"
echo "      James:  OK"

echo
echo "[5/7] Locating James modules..."

MODULES=(
    "backends-common"
    "event-bus"
    "event-sourcing"
    "javax-mail-extension"
    "mailbox"
    "metrics"
    "protocols"
    "server/container"
    "server/dns-service"
    "server/mailet"
    "server/mailrepository"
    "server/protocols"
    "server/queue"
    "server/task"
)

for module in "${MODULES[@]}"; do

    if [ -d "$JAMES/$module" ]; then
        echo "      FOUND: $module"
    else
        echo "      MISSING UPSTREAM: $module"
    fi

done

echo
echo "[6/7] Source statistics..."

MEARVK_JAVA=$(find "$MEARVK" -type f -name "*.java" | wc -l)
JAMES_JAVA=$(find "$JAMES" -type f -name "*.java" | wc -l)

MEARVK_POM=$(find "$MEARVK" -type f -name "pom.xml" | wc -l)
JAMES_POM=$(find "$JAMES" -type f -name "pom.xml" | wc -l)

echo
echo "      MEARVK Java files : $MEARVK_JAVA"
echo "      James Java files  : $JAMES_JAVA"
echo
echo "      MEARVK POM files  : $MEARVK_POM"
echo "      James POM files   : $JAMES_POM"

echo
echo "[7/7] Download complete."
echo
echo "=============================================="
echo " SOURCE TREES"
echo "=============================================="
echo
echo "MEARVK:"
echo "  $MEARVK"
echo
echo "Apache James 3.9.0:"
echo "  $JAMES"
echo
echo "=============================================="
echo
echo "No files have been copied or overwritten."
echo "The next step is comparison and selective recovery."
echo