#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Start the examples as a RAP application (embedded Jetty, 127.0.0.1) and take one screenshot per entry point with
# headless Chrome (no window). Usage: browser_run.sh <build-dir> <port> <paths-file> <shots-dir>
# paths-file: one "id<TAB>path" per line. Writes <shots-dir>/<id>.png and <shots-dir>/shots.tsv.
set -u
OUT=$1; PORT=$2; PATHS=$3; SHOTS=$4
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}/bin"
CHROME="${M3RWT_CHROME:-C:/Program Files/Google/Chrome/Application/chrome.exe}"
mkdir -p "$SHOTS" "$OUT/web" "$OUT/tmp"
CP="$OUT/examples-classes;$(cat "$OUT/examples-classpath.txt")"
"$JDK/java" -Xmx1g -Djava.io.tmpdir="$OUT/tmp" -cp "$CP" org.eclipse.nebula.widgets.m3.rwt.examples.RwtServer "$PORT" "$OUT/web" > "$OUT/server.log" 2>&1 &
SERVER=$!
trap 'kill $SERVER 2>/dev/null' EXIT
for i in $(seq 1 60); do grep -q "RWT examples ready" "$OUT/server.log" && break; sleep 1; done
grep -q "RWT examples ready" "$OUT/server.log" || { echo "server did not start"; tail -20 "$OUT/server.log"; exit 1; }
printf 'id\turl\thttp_status\tpng\tpng_bytes\n' > "$SHOTS/shots.tsv"
while IFS=$'\t' read -r id path; do
	url="http://127.0.0.1:$PORT$path"
	status=$(curl -s -o /dev/null -w '%{http_code}' "$url")
	rm -f "$SHOTS/$id.png"
	"$CHROME" --headless=new --disable-gpu --hide-scrollbars --no-first-run --no-default-browser-check \
		--user-data-dir="$OUT/chrome-profile" --window-size=1000,700 --virtual-time-budget=15000 \
		--screenshot="$SHOTS/$id.png" "$url" > "$OUT/chrome-$id.log" 2>&1
	size=$(stat -c %s "$SHOTS/$id.png" 2>/dev/null || echo 0)
	printf '%s\t%s\t%s\t%s\t%s\n' "$id" "$url" "$status" "$id.png" "$size" >> "$SHOTS/shots.tsv"
done < "$PATHS"
column -t -s$'\t' "$SHOTS/shots.tsv"
