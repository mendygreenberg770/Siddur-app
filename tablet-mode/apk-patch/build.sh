#!/usr/bin/env bash
#
# Rebuild Siddur.apk with the two-column tablet spread.
#
#   ./build.sh /path/to/Siddur.apk
#
# Output: out/Siddur-tablet-aligned-debugSigned.apk  (re-signed with a debug key)
#
# Requires: java 11+, curl, and internet on first run (to fetch the tools below).
set -euo pipefail

SRC_APK="${1:?usage: ./build.sh /path/to/Siddur.apk}"
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$HERE/.work"
TOOLS="$HERE/.tools"
OUT="$HERE/out"
APKTOOL_VER="2.10.0"
SIGNER_VER="1.3.0"

mkdir -p "$TOOLS" "$OUT"
APKTOOL="$TOOLS/apktool-$APKTOOL_VER.jar"
SIGNER="$TOOLS/uber-apk-signer-$SIGNER_VER.jar"
[ -f "$APKTOOL" ] || curl -sSL -o "$APKTOOL" \
  "https://github.com/iBotPeaches/Apktool/releases/download/v$APKTOOL_VER/apktool_$APKTOOL_VER.jar"
[ -f "$SIGNER" ] || curl -sSL -o "$SIGNER" \
  "https://github.com/patrickfav/uber-apk-signer/releases/download/v$SIGNER_VER/uber-apk-signer-$SIGNER_VER.jar"

echo ">> decoding"
rm -rf "$WORK"; mkdir -p "$WORK"
java -jar "$APKTOOL" d "$SRC_APK" -o "$WORK/apk_src" >/dev/null

echo ">> installing tablet layout"
cp "$HERE/layout-sw600dp/prayer_fragment.xml" "$WORK/apk_src/res/layout-sw600dp/prayer_fragment.xml"

echo ">> patching PrayerFragment smali"
PF="$(find "$WORK/apk_src" -path '*org/chabad/kehossiddur/PrayerFragment.smali')"
python3 - "$PF" <<'PY'
import io, re, sys
path = sys.argv[1]
s = io.open(path, encoding="utf-8").read()

# (1) bump .locals of onViewCreated from 5 to 8
s = s.replace(
    ".method public onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V\n    .locals 5",
    ".method public onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V\n    .locals 8",
    1,
)

# (2) insert the spread block after the left reader's openFile, before ".line 158"
anchor = ("    invoke-virtual {p2, p1, v0, v1, v2}, Lorg/chabad/kehossiddur/PDFView;"
          "->openFile(Ljava/lang/String;Ljava/util/ArrayList;II)Lru/mobigroup/bookreader/MuPDFCore;\n")
block = (
    "\n"
    "    invoke-virtual {p2}, Landroid/view/View;->getParent()Landroid/view/ViewParent;\n\n"
    "    move-result-object v5\n\n"
    "    instance-of v6, v5, Landroid/view/ViewGroup;\n\n"
    "    if-eqz v6, :spread_skip\n\n"
    "    check-cast v5, Landroid/view/ViewGroup;\n\n"
    "    invoke-virtual {v5}, Landroid/view/ViewGroup;->getChildCount()I\n\n"
    "    move-result v6\n\n"
    "    const/4 v7, 0x2\n\n"
    "    if-lt v6, v7, :spread_skip\n\n"
    "    const/4 v7, 0x1\n\n"
    "    invoke-virtual {v5, v7}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;\n\n"
    "    move-result-object v5\n\n"
    "    instance-of v6, v5, Lorg/chabad/kehossiddur/PDFView;\n\n"
    "    if-eqz v6, :spread_skip\n\n"
    "    check-cast v5, Lorg/chabad/kehossiddur/PDFView;\n\n"
    "    add-int/lit8 v6, v1, 0x1\n\n"
    "    invoke-virtual {v5, p1, v0, v6, v2}, Lorg/chabad/kehossiddur/PDFView;"
    "->openFile(Ljava/lang/String;Ljava/util/ArrayList;II)Lru/mobigroup/bookreader/MuPDFCore;\n\n"
    "    invoke-virtual {v5}, Lorg/chabad/kehossiddur/PDFView;->checkHasSizes()V\n\n"
    "    :spread_skip\n"
)
assert anchor in s, "openFile anchor not found - the APK layout may differ from the expected build"
if ":spread_skip" not in s:
    s = s.replace(anchor, anchor + block, 1)
io.open(path, "w", encoding="utf-8").write(s)
print("   patched:", path)
PY

echo ">> rebuilding"
java -jar "$APKTOOL" b "$WORK/apk_src" -o "$WORK/Siddur-tablet.apk" >/dev/null

echo ">> signing"
java -jar "$SIGNER" --apks "$WORK/Siddur-tablet.apk" --out "$OUT" >/dev/null

echo ">> done:"
ls -la "$OUT"/*.apk
