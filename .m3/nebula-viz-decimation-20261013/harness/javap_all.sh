#!/bin/sh
# usage: javap_all.sh <tag>  -> $B/<tag>/api.javap.txt (public+protected members of every non-example class)
. /c/m3-work-c/viz-build/scripts/env.sh
tag=$1; C=$B/$tag/classes
(cd $C && find . -name '*.class' | sed 's#^\./##;s#\.class$##;s#/#.#g' | grep -v '\.examples\.' | sort) > $B/$tag/api.classes.lst
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
CPW="$(cygpath -w $C);$CP"; : > $B/$tag/api.javap.txt
xargs -n 150 sh -c 'j=$0; cp=$1; shift; "$j" -J-Xmx512m -protected -cp "$cp" "$@"' "$J/javap" "$CPW" < $B/$tag/api.classes.lst >> $B/$tag/api.javap.txt 2> $B/$tag/api.javap.err
echo "javap $tag rc=$? classes=$(wc -l < $B/$tag/api.classes.lst)"
