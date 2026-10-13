#!/bin/sh
# usage: ui.sh <command...>   Runs one UI chunk under the shared desktop lock C:/m3-work-c/ui-test.lock
# (mkdir is the atomic acquire; retry every 30 s; owner.txt names the holder; released on exit).
LOCK=/c/m3-work-c/ui-test.lock
waited=0
while ! mkdir $LOCK 2>/dev/null; do
  [ $waited -eq 0 ] && echo "ui-lock busy: $(cat $LOCK/owner.txt 2>/dev/null)"
  waited=$((waited + 30)); [ $waited -gt 14400 ] && { echo "ui-lock timeout"; exit 4; }
  sleep 30
done
echo "nebula-tree-table agent pid=$$ $(date -Iseconds) $*" > $LOCK/owner.txt
trap 'rm -rf $LOCK' EXIT INT TERM
echo "ui-lock acquired after ${waited}s: $*"
"$@"
