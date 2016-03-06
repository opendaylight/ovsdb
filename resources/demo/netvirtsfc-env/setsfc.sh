#!/usr/bin/env bash

ovsdbversion="1.2.1-SNAPSHOT"

# Attempt to keep l2switch from monkeying with the flows
#sed -i 's/<is-proactive-flood-mode>true<\/is-proactive-flood-mode>/<is-proactive-flood-mode>false<\/is-proactive-flood-mode>/' ${ODL_ROOT_DIR}/system/org/opendaylight/l2switch/arphandler/arphandler-config/$l2switchversion/arphandler-config-$l2switchversion-config.xml
#sed -i 's/<is-install-lldp-flow>true<\/is-install-lldp-flow>/<is-install-lldp-flow>false<\/is-install-lldp-flow>/' ${ODL_ROOT_DIR}/system/org/opendaylight/l2switch/loopremover/loopremover-config/$l2switchversion/loopremover-config-$l2switchversion-config.xml

# enable NetvirtSfc for standalone mode
sed -i -e 's/<of13provider>[a-z]\{1,\}<\/of13provider>/<of13provider>standalone<\/of13provider>/g' ${ODL_ROOT_DIR}/system/org/opendaylight/ovsdb/openstack.net-virt-sfc-impl/$ovsdbversion/openstack.net-virt-sfc-impl-$ovsdbversion-config.xml

# Automatically install the feature odl-ovsdb-sfc-ui upon ODL start
ODL_NETVIRT_SFC_KARAF_FEATURE='odl-ovsdb-sfc-ui'
ODLFEATUREMATCH=$(cat ${ODL_ROOT_DIR}/etc/org.apache.karaf.features.cfg | \
                            grep -e "featuresBoot=" -e "featuresBoot =" | grep $ODL_NETVIRT_SFC_KARAF_FEATURE)
if [ "$ODLFEATUREMATCH" == "" ]; then
   sed -i -e "/^featuresBoot[ ]*=/ s/$/,$ODL_NETVIRT_SFC_KARAF_FEATURE/" \
       ${ODL_ROOT_DIR}/etc/org.apache.karaf.features.cfg
fi

# Set the logging levels for troubleshooting
logcfg=${ODL_ROOT_DIR}/etc/org.ops4j.pax.logging.cfg
echo "log4j.logger.org.opendaylight.ovsdb.openstack.netvirt.sfc = TRACE" >> $logcfg
#echo "log4j.logger.org.opendaylight.ovsdb.lib = INFO" >> $logcfg
echo "log4j.logger.org.opendaylight.sfc = TRACE" >> $logcfg
echo "log4j.logger.org.opendaylight.openflowplugin.applications.statistics.manager.impl.StatListenCommitFlow = ERROR" >> $logcfg

