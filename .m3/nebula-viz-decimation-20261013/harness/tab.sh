#!/bin/sh
# tabulates label<TAB>metric<TAB>value lines of a log into metric x label (labels in first-seen order)
grep -v "^\s*$" "$1" | awk -F'\t' 'NF==3{k=$2; if(!($1 in Ls)){Ls[$1]=1; L[++nl]=$1}; v[$1,k]=$3; if(!(k in seen)){seen[k]=1; keys[++n]=k}} NF!=3{print "#", $0} END{printf "metric"; for(j=1;j<=nl;j++) printf "\t%s", L[j]; print ""; for(i=1;i<=n;i++){printf "%s", keys[i]; for(j=1;j<=nl;j++) printf "\t%s", v[L[j],keys[i]]; print ""}}'
