/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuserclient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGeneve;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeIpsecGre64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeLisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypePatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeStt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeTap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow10;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow12;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow13;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow14;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeProtocolOpenflow15;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeSecure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbFailModeStandalone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeEgressPolicer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxCodel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxFqCodel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxHfsc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxHtb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.QosTypeLinuxSfq;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public interface SouthboundConstants {

    String OPEN_V_SWITCH = "Open_vSwitch";
    String HARDWARE_VTEP = "hardware_vtep";
    TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    String OVSDB_URI_PREFIX = "ovsdb";
    String BRIDGE_URI_PREFIX = "bridge";
    String TP_URI_PREFIX = "terminationpoint";
    String QOS_URI_PREFIX = "qos";
    String QOS_NAMED_UUID_PREFIX = "QOS";
    Integer PORT_QOS_LIST_KEY = 1;
    String QUEUE_URI_PREFIX = "queue";
    String QUEUE_NAMED_UUID_PREFIX = "QUEUE";
    String AUTOATTACH_URI_PREFIX = "autoattach";
    String AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION = "7.11.2";
    Integer DEFAULT_OVSDB_PORT = 6640;
    String DEFAULT_OPENFLOW_PORT = "6653";
    String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    String UUID = "uuid";
    String QOS_LINUX_HTB = "linux-htb";
    String QOS_LINUX_HFSC = "linux-hfsc";
    // The following four QoS types are present in OVS 2.5+
    // Refer to http://openvswitch.org/support/dist-docs/ovs-vswitchd.conf.db.5.txt
    String QOS_LINUX_SFQ = "linux-sfq";
    String QOS_LINUX_CODEL = "linux-codel";
    String QOS_LINUX_FQ_CODEL = "linux-fq_codel";
    String QOS_EGRESS_POLICER = "egress-policer";
    String URI_SEPERATOR = "/";
    String CREATED_BY = "created_by";
    String ODL = "odl";

    ImmutableBiMap<Class<? extends QosTypeBase>,String> QOS_TYPE_MAP
        = new ImmutableBiMap.Builder<Class<? extends QosTypeBase>,String>()
            .put(QosTypeLinuxHtb.class,QOS_LINUX_HTB)
            .put(QosTypeLinuxHfsc.class,QOS_LINUX_HFSC)
            .put(QosTypeLinuxSfq.class,QOS_LINUX_SFQ)
            .put(QosTypeLinuxCodel.class,QOS_LINUX_CODEL)
            .put(QosTypeLinuxFqCodel.class,QOS_LINUX_FQ_CODEL)
            .put(QosTypeEgressPolicer.class,QOS_EGRESS_POLICER)
            .build();

    ImmutableBiMap<Class<? extends OvsdbBridgeProtocolBase>,String> OVSDB_PROTOCOL_MAP
        = new ImmutableBiMap.Builder<Class<? extends OvsdbBridgeProtocolBase>,String>()
            .put(OvsdbBridgeProtocolOpenflow10.class,"OpenFlow10")
            .put(OvsdbBridgeProtocolOpenflow11.class,"OpenFlow11")
            .put(OvsdbBridgeProtocolOpenflow12.class,"OpenFlow12")
            .put(OvsdbBridgeProtocolOpenflow13.class,"OpenFlow13")
            .put(OvsdbBridgeProtocolOpenflow14.class,"OpenFlow14")
            .put(OvsdbBridgeProtocolOpenflow15.class,"OpenFlow15")
            .build();

    ImmutableBiMap<Class<? extends OvsdbFailModeBase>,String> OVSDB_FAIL_MODE_MAP
        = new ImmutableBiMap.Builder<Class<? extends OvsdbFailModeBase>,String>()
            .put(OvsdbFailModeStandalone.class,"standalone")
            .put(OvsdbFailModeSecure.class,"secure")
            .build();

    ImmutableBiMap<String, Class<? extends InterfaceTypeBase>> OVSDB_INTERFACE_TYPE_MAP
        = new ImmutableBiMap.Builder<String, Class<? extends InterfaceTypeBase>>()
            .put("internal", InterfaceTypeInternal.class)
            .put("vxlan", InterfaceTypeVxlan.class)
            .put("vxlan-gpe", InterfaceTypeVxlanGpe.class)
            .put("patch", InterfaceTypePatch.class)
            .put("system", InterfaceTypeSystem.class)
            .put("tap", InterfaceTypeTap.class)
            .put("geneve", InterfaceTypeGeneve.class)
            .put("gre", InterfaceTypeGre.class)
            .put("ipsec_gre", InterfaceTypeIpsecGre.class)
            .put("gre64", InterfaceTypeGre64.class)
            .put("ipsec_gre64", InterfaceTypeIpsecGre64.class)
            .put("lisp", InterfaceTypeLisp.class)
            .put("dpdk", InterfaceTypeDpdk.class)
            .put("dpdkr", InterfaceTypeDpdkr.class)
            .put("dpdkvhost", InterfaceTypeDpdkvhost.class)
            .put("dpdkvhostuser", InterfaceTypeDpdkvhostuser.class)
            .put("dpdkvhostuserclient", InterfaceTypeDpdkvhostuserclient.class)
            .put("stt", InterfaceTypeStt.class)
            .build();

    ImmutableBiMap<Class<? extends DatapathTypeBase>,String> DATAPATH_TYPE_MAP
        = new ImmutableBiMap.Builder<Class<? extends DatapathTypeBase>,String>()
            .put(DatapathTypeSystem.class,"system")
            .put(DatapathTypeNetdev.class,"netdev")
            .build();

    String IID_EXTERNAL_ID_KEY = "opendaylight-iid";
    String QOS_ID_EXTERNAL_ID_KEY = "opendaylight-qos-id";
    String QUEUE_ID_EXTERNAL_ID_KEY = "opendaylight-queue-id";
    String AUTOATTACH_ID_EXTERNAL_ID_KEY = "opendaylight-autoattach-id";

    ImmutableCollection<String> SKIP_OVSDB_TABLE = new ImmutableSet.Builder<String>()
            .add("Flow_Table")
            .add("Mirror")
            .add("NetFlow")
            .add("sFlow")
            .add("IPFIX")
            .add("Flow_Sample_Collector_Set")
            .build();

    //Note: _version is an internal column of ovsdb schema, that gets updated
    //with every change in the row of the table.
    // The "Manager" entry needs to be a modifiable list, SouthboundProvider::setSkipManagerStatus() modifies it
    ImmutableMap<String,List<String>> SKIP_COLUMN_FROM_TABLE
            = new ImmutableMap.Builder<String,List<String>>()
            .put("Open_vSwitch", Arrays.asList("statistics","_version"))
            .put("Port", Arrays.asList("statistics","_version"))
            .put("Manager", new ArrayList<>(Collections.singletonList("_version")))
            .put("SSL", Collections.singletonList("_version"))
            .put("QoS", Collections.singletonList("_version"))
            .put("Queue", Collections.singletonList("_version"))
            .put("Bridge", Collections.singletonList("_version"))
            .put("Interface", Arrays.asList("statistics","_version"))
            .put("Controller", Arrays.asList("status","_version"))
            .build();

    enum VlanModes {
        ACCESS("access"),
        NATIVE_TAGGED("native-tagged"),
        NATIVE_UNTAGGED("native-untagged"),
        TRUNK("trunk");

        private final String mode;

        VlanModes(String mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return mode;
        }

        public String getMode() {
            return this.mode;
        }
    }

    enum OwnershipStates {
        OWNER("OWNER"),
        NONOWNER("NON-OWNER");

        private final String state;

        OwnershipStates(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }

        public String getState() {
            return this.state;
        }
    }

    int EOS_TIMEOUT = Integer.getInteger("southbound.eos.timeout.delay.secs", 240);
}
