#!/bin/sh
# waits (max ~15 min) until memcheck passes; exit 3 if it never does
for i in $(seq 1 90); do sh /c/m3-work-c/viz-build/scripts/memcheck.sh > /dev/null && exit 0; sleep 10; done
echo MEMWAIT_TIMEOUT; exit 3
