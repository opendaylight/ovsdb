#!/usr/bin/env bash

hostnum=${HOSTNAME#"netvirtsfc"}
sw="sw$hostnum"
set -e
if [ "$1" ]
then
    echo;echo "FLOWS:";ovs-ofctl dump-flows $sw -OOpenFlow13 table=$1 --rsort=priority
    echo
    printf "Flow count: "
    echo $(($(ovs-ofctl dump-flows $sw -OOpenFlow13 table=$1 | wc -l)-1))
else
    echo;echo "FLOWS:";ovs-ofctl dump-flows $sw -OOpenFlow13
    printf "No table entered. $sw flow count: ";
    echo $(($(ovs-ofctl dump-flows $sw -OOpenFlow13 | wc -l)-1))
    printf "\nTable0: base:  "; echo $(($(ovs-ofctl dump-flows $sw -OOpenFlow13 table=0| wc -l)-1))
    printf "\nTable50: sfc:   "; echo $(($(ovs-ofctl dump-flows $sw -OOpenFlow13 table=6| wc -l)-1))
fi

