#!/bin/bash

# Use same path for run.base.sh
RUNSH_DIR=$(dirname $0)
RUN_BASE_SH=${RUNSH_DIR}/run.base.sh

function usage {
    echo -e "You must select one of the 3 supported network virtualization technologies:\n\tovsdb | opendove | vtn"
    echo "Usage: $0 -virt {ovsdb | opendove | vtn | affinity} [advanced options]"
    echo "Advanced options: $($RUN_BASE_SH -help | sed "s;Usage: $RUN_BASE_SH ;;")"
    exit 1
}

virtIndex=0
while true ; do
    (( i += 1 ))
    case "${@:$i:1}" in
        -virt) virtIndex=$i ;;
        "") break ;;
    esac
done

# Virtualization edition select
if [ ${virtIndex} -eq 0 ]; then
    usage
fi

virt=${@:$virtIndex+1:1}
if [ "${virt}" == "" ]; then
    usage
else
    if [ "${virt}" == "ovsdb" ]; then
        ODL_VIRT_FILTER="opendove|vtn"
    elif [ "${virt}" == "opendove" ]; then
        ODL_VIRT_FILTER="ovsdb|vtn"
    elif [ "${virt}" == "vtn" ]; then
        ODL_VIRT_FILTER="affinity|opendove|ovsdb.ovsdb.neutron|controller.(arphandler|samples)"
    elif [ "${virt}" == "affinity" ]; then
        ODL_VIRT_FILTER="vtn|opendove|controller.samples"
    else
        usage
    fi
fi

$RUN_BASE_SH -bundlefilter "org.opendaylight.(${ODL_VIRT_FILTER})" "${@:1:$virtIndex-1}" "${@:virtIndex+2}"
