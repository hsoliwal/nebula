#!/bin/sh
# Waits (poll 15 s, max 40 min) until memcheck passes; exit 1 on timeout.
i=0
while ! sh /c/m3-work-c/nebula-tt-build/scripts/memcheck.sh > /dev/null; do
  i=$((i + 1)); [ $i -gt 160 ] && { echo "GATE-TIMEOUT"; exit 1; }
  sleep 15
done
sh /c/m3-work-c/nebula-tt-build/scripts/memcheck.sh; exit 0
