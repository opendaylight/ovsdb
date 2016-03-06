#!/usr/bin/env bash

set -e
hostnum=${HOSTNAME#"netvirtsfc"}
sw="sw$hostnum"

TABLE=$1

clear
ovs-ofctl dump-groups $sw -OOpenFlow13
if [ "$TABLE" ]
then
        ovs-ofctl dump-flows $sw -OOpenFlow13 table=$TABLE
else
        ovs-ofctl dump-flows $sw -OOpenFlow13
fi

