#!/bin/bash

# Use same path for run.internal.sh
RUNSH_DIR=$(dirname $0)
CONTROLLER_RUNSH=${RUNSH_DIR}/run.internal.sh

OF_FILTER=
OF_PLUGIN_RUNTIME=

# Be extra careful to pass on usage from run.internal.sh, but add our
# usage as well in the standard way
function usage {
    $CONTROLLER_RUNSH -help | sed 's/\[-help\]/\[-help\] \[-of10\] \[-bundlefilter \<bundlefilter\> \]/' | sed "s;$CONTROLLER_RUNSH;$0;"
    exit 1
}

OF13=1
BUNDLEFILTER=
while true ; do
    (( i += 1 ))
    case "${@:$i:1}" in
        -of10) OF13=0 ; (( i += 1 ));;
        -of13) OF13=1 ; (( i += 1 ));;
        -bundlefilter) (( i += 1 )); BUNDLEFILTER="|${@:$i:1}";;
        -help) (( i += 1 )); helparg=${@:$i:1}; usage ;;
        "") break ;;
    esac
done

# clean available optional configurations (links)
find configuration/initial -type l -exec rm {} \;

# OF Filter selection
OF_FILTER="org.opendaylight.(openflowplugin|openflowjava|controller.sal-compatibility)"
OF_PLUGIN_RUNTIME="1.0"
if (( $OF13 != 0 )); then
    OF_PLUGIN_RUNTIME="1.3"
    OF_FILTER="org.opendaylight.controller.(thirdparty.org.openflow|protocol_plugins.openflow)"
    while read ofConfig; do
        ln -s ../initial.available/$(basename ${ofConfig}) configuration/initial/
    done < <(find configuration/initial.available -name '*openflowplugin.xml')
fi

# Make sure we suck out our additional args so as to not confuse
# run.internal.sh
NEWARGS=`echo $@|sed 's/-of13//'|sed 's/-of10//'|sed 's/-bundlefilter[ ]*[^ ]*//'`


# Build the filter string
FILTERBEGINING='^(?!'
FILTERENDING=').*'
FILTER=${FILTERBEGINING}${OF_FILTER}${BUNDLEFILTER}${FILTERENDING}

# Run the command
$CONTROLLER_RUNSH -Dfelix.fileinstall.filter="$FILTER" -Dovsdb.of.version="$OF_PLUGIN_RUNTIME" $NEWARGS
