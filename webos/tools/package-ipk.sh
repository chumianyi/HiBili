#!/usr/bin/env bash
#
# Hi！Bili - WebOS Reference Build
# Copyright (C) 2024 HiBili Team
# Licensed under GPL-3.0-or-later.
#
# Package the app into an IPK (Debian binary package, used by webOS).
#
# IPK layout (ar archive):
#   debian-binary        -> "2.0\n"
#   control.tar.gz        -> maintainer scripts / metadata
#   data.tar.gz           -> actual app payload (index.html, source/, ...)
#
# Usage:
#   tools/package-ipk.sh [output.ipk]
#
# Requirements: ar, tar, gzip (baseline on ubuntu-latest CI).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OUT="${1:-${PROJECT_ROOT}/hibili-webos_1.0.0_all.ipk}"

STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

echo "[ipk] staging in $STAGE"

# ---- debian-binary ----
echo "2.0" > "$STAGE/debian-binary"

# ---- control.tar.gz ----
CTRL="$STAGE/control"
mkdir -p "$CTRL"
cat > "$CTRL/control" <<EOF
Package: com.hibili.webos
Version: 1.0.0
Architecture: all
Maintainer: HiBili Team <hibili@users.noreply.github.com>
Section: web
Priority: optional
Homepage: https://github.com/chumianyi/HiBili
Description: Hi!Bili WebOS reference build
 A Bilibili client prototype for LG/Open webOS.
 Reference build only; not intended for production TV use.
License: GPL-3.0-or-later
EOF

# postinst (webOS convention: app lives under /usr/palm/applications/<id>/)
cat > "$CTRL/postinst" <<'EOF'
#!/bin/sh
# webOS postinst - no-op for reference build
exit 0
EOF
chmod 0755 "$CTRL/postinst"

tar -C "$STAGE" -czf "$STAGE/control.tar.gz" control
rm -rf "$CTRL"

# ---- data.tar.gz ----
DATA="$STAGE/data"
mkdir -p "$DATA/usr/palm/applications/com.hibili.webos"

# Copy app payload (exclude tools, .git, node_modules, stage files)
rsync -a \
  --exclude 'tools/' \
  --exclude '.git/' \
  --exclude '.github/' \
  --exclude 'node_modules/' \
  --exclude '*.ipk' \
  --exclude 'README.md' \
  --exclude 'LICENSE' \
  "$PROJECT_ROOT/" \
  "$DATA/usr/palm/applications/com.hibili.webos/"

tar -C "$STAGE" -czf "$STAGE/data.tar.gz" data
rm -rf "$DATA"

# ---- ar archive ----
cd "$STAGE"
ar rcs "$OUT" debian-binary control.tar.gz data.tar.gz

echo "[ipk] built: $OUT"
ls -lh "$OUT"
