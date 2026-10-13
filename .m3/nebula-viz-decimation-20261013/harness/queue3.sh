#!/bin/sh
B=/c/m3-work-c/viz-build; H=/c/m3-work-c/nebula-viz/.m3/nebula-viz-decimation-20261013/harness; L=/c/m3-work-c/viz-logs
(sh $B/scripts/examples.sh base $B/base-src; sh $B/scripts/examples.sh after /c/m3-work-c/nebula-viz) > $L/examples.log 2>&1
sh $B/scripts/javap_all.sh base > $L/javap.log 2>&1
sh $B/scripts/javap_all.sh after >> $L/javap.log 2>&1
(sh $B/scripts/bench.sh base XYGraphBench $H 2g "" 1000000 circular 30 base-1M-circular; sh $B/scripts/bench.sh after XYGraphBench $H 2g "" 1000000 packed 30 after-1M-packed; sh $B/scripts/bench.sh after XYGraphBench $H 2g "" 10000000 packed 30 after-10M-packed) > $L/bench-xy-final.log 2>&1
(sh $B/scripts/bench.sh base IntensityBench $H 2g "" 3162 30 base-3162 4096; sh $B/scripts/bench.sh after IntensityBench $H 2g "" 3162 30 after-3162 4096) > $L/bench-intensity-final.log 2>&1
echo QUEUE3_DONE
