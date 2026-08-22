```bash
#!/usr/bin/env bash
#
# fetch-missing-james-modules.sh
#
# Purpose:
#   Restore the Apache James 3.9.0 modules identified as missing by
#   MearvkEmailServer.
#
# Repository:
#   https://github.com/mearvk/MearvkEmailServer
#
# Upstream:
#   Apache James 3.9.0
#
# IMPORTANT:
#   This script does NOT overwrite the existing MEARVK repository.
#   It creates a backup copy and uses the upstream James 3.9.0 tag
#   as the source of missing modules.
#

set -Eeuo pipefail

MEARVK_REPO="https://github.com/mearvk/MearvkEmailServer.git"
JAMES_REPO="https://github.com/apache/james-project.git"

JAMES_TAG="james-project-3.9.0"

WORK_DIR="${WORK_DIR:-$PWD/mearvk-reconstruction}"
MEARVK_DIR="$WORK_DIR/MearvkEmailServer"
JAMES_DIR="$WORK_DIR/james-project"

SOURCE_DIR="$MEARVK_DIR/src/james-project"
BACKUP_DIR="$MEARVK_DIR-backup-before-james-restore"

echo
echo "============================================================"
echo " MEARVK Email Server / Apache James 3.9.0 Source Recovery"
echo "============================================================"
echo

mkdir -p "$WORK_DIR"

#
# ------------------------------------------------------------
# 1. Clone MEARVK repository
# ------------------------------------------------------------
#

if [[ -d "$MEARVK_DIR/.git" ]]; then
    echo "[1/8] MEARVK repository already exists:"
    echo "      $MEARVK_DIR"
else
    echo "[1/8] Cloning MEARVK repository..."
    git clone "$MEARVK_REPO" "$MEARVK_DIR"
fi

#
# ------------------------------------------------------------
# 2. Verify source tree
# ------------------------------------------------------------
#

if [[ ! -d "$SOURCE_DIR" ]]; then
    echo
    echo "ERROR: Expected source directory does not exist:"
    echo "  $SOURCE_DIR"
    exit 1
fi

#
# ------------------------------------------------------------
# 3. Create safety backup
# ------------------------------------------------------------
#

echo "[2/8] Creating backup..."

if [[ ! -d "$BACKUP_DIR" ]]; then
    cp -a "$MEARVK_DIR" "$BACKUP_DIR"
    echo "      Backup created:"
    echo "      $BACKUP_DIR"
else
    echo "      Backup already exists:"
    echo "      $BACKUP_DIR"
fi

#
# ------------------------------------------------------------
# 4. Download Apache James 3.9.0
# ------------------------------------------------------------
#

if [[ -d "$JAMES_DIR/.git" ]]; then
    echo "[3/8] Apache James repository already exists."
else
    echo "[3/8] Cloning Apache James..."
    git clone --filter=blob:none --no-checkout "$JAMES_REPO" "$JAMES_DIR"
fi

cd "$JAMES_DIR"

echo "[4/8] Checking out Apache James 3.9.0..."

git fetch --tags --quiet

if ! git rev-parse --verify "refs/tags/$JAMES_TAG" >/dev/null 2>&1; then
    echo
    echo "ERROR: Apache James tag '$JAMES_TAG' was not found."
    echo
    echo "Available James tags containing 3.9:"
    git tag --list '*3.9*'
    exit 1
fi

git checkout --quiet "$JAMES_TAG"

cd "$MEARVK_DIR"

#
# ------------------------------------------------------------
# 5. Modules explicitly identified as missing
# ------------------------------------------------------------
#

MISSING_MODULES=(
    "backends-common"
    "event-bus"
    "event-sourcing"
    "javax-mail-extension"
    "mailbox"
    "metrics"
    "protocols"
    "testing/base"
    "third-party"

    "server/container"
    "server/dns-service"
    "server/mailet"
    "server/mailrepository"
    "server/protocols"
    "server/queue"
    "server/task"
)

#
# ------------------------------------------------------------
# 6. Copy missing modules
# ------------------------------------------------------------
#

echo "[5/8] Restoring missing modules..."

for module in "${MISSING_MODULES[@]}"; do

    SRC="$JAMES_DIR/$module"
    DST="$SOURCE_DIR/$module"

    echo
    echo "------------------------------------------------------------"
    echo "Module: $module"
    echo "------------------------------------------------------------"

    if [[ ! -d "$SRC" ]]; then
        echo "WARNING: Upstream module does not exist:"
        echo "        $SRC"
        continue
    fi

    if [[ -e "$DST" ]]; then
        echo "Existing destination detected:"
        echo "  $DST"

        read -r -p "Overwrite this module? [y/N] " ANSWER

        if [[ ! "$ANSWER" =~ ^[Yy]$ ]]; then
            echo "Skipping $module"
            continue
        fi

        rm -rf "$DST"
    fi

    mkdir -p "$(dirname "$DST")"

    cp -a "$SRC" "$DST"

    echo "Restored:"
    echo "  $DST"
done

#
# ------------------------------------------------------------
# 7. Compare Maven project structure
# ------------------------------------------------------------
#

echo
echo "[6/8] Checking Maven project structure..."

cd "$SOURCE_DIR"

echo
echo "Root POM modules:"
echo "------------------------------------------------------------"

if [[ -f pom.xml ]]; then
    grep -E '<module>[^<]+</module>' pom.xml || true
else
    echo "WARNING: Root pom.xml is missing."
fi

#
# ------------------------------------------------------------
# 8. Produce a report
# ------------------------------------------------------------

REPORT="$WORK_DIR/recovery-report.txt"

echo "[7/8] Generating recovery report..."

{
    echo "MEARVK EMAIL SERVER SOURCE RECOVERY"
    echo "==================================="
    echo
    echo "MEARVK:"
    echo "$MEARVK_REPO"
    echo
    echo "UPSTREAM:"
    echo "$JAMES_REPO"
    echo
    echo "UPSTREAM VERSION:"
    echo "$JAMES_TAG"
    echo
    echo "SOURCE DIRECTORY:"
    echo "$SOURCE_DIR"
    echo
    echo "RESTORED MODULES:"
    echo

    for module in "${MISSING_MODULES[@]}"; do
        if [[ -d "$SOURCE_DIR/$module" ]]; then
            echo "  [RESTORED] $module"
        else
            echo "  [MISSING]  $module"
        fi
    done

    echo
    echo "JAVA FILE COUNT:"
    find "$SOURCE_DIR" -type f -name '*.java' | wc -l

    echo
    echo "POM FILE COUNT:"
    find "$SOURCE_DIR" -type f -name 'pom.xml' | wc -l

    echo
    echo "GIT STATUS:"
    git -C "$MEARVK_DIR" status --short

} > "$REPORT"

#
# ------------------------------------------------------------
# 9. Optional compilation
# ------------------------------------------------------------
#

echo "[8/8] Source restoration complete."

echo
echo "============================================================"
echo " RESTORATION COMPLETE"
echo "============================================================"
echo
echo "MEARVK source:"
echo "  $SOURCE_DIR"
echo
echo "Backup:"
echo "  $BACKUP_DIR"
echo
echo "Recovery report:"
echo "  $REPORT"
echo
echo "Next step:"
echo
echo "  cd \"$SOURCE_DIR\""
echo
echo "  mvn -DskipTests -Dcheckstyle.skip=true \\"
echo "      -Dscalafix.skip=true \\"
echo "      -Djib.skip=true \\"
echo "      -Dassembly.skipAssembly=true \\"
echo "      compile"
echo
echo "DO NOT deploy this result yet."
echo "The restored modules need dependency/version consistency checks."
echo
```