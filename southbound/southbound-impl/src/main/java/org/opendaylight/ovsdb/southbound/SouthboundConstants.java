/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.collect.ImmutableBiMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeNetdev;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.DatapathTypeSystem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhost;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeDpdkvhostuser;
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

public class SouthboundConstants {
    public static final String OPEN_V_SWITCH = "Open_vSwitch";
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    public static final String TP_URI_PREFIX = "terminationpoint";
    public static final String QOS_URI_PREFIX = "qos";
    public static final String QOS_NAMED_UUID_PREFIX = "QOS";
    public static final Integer PORT_QOS_LIST_KEY = 1;
    public static final String QUEUE_URI_PREFIX = "queue";
    public static final String QUEUE_NAMED_UUID_PREFIX = "QUEUE";
    public static final String AUTOATTACH_URI_PREFIX = "autoattach";
    public static final String AUTOATTACH_SUPPORTED_OVS_SCHEMA_VERSION = "7.11.2";
    public static final Integer DEFAULT_OVSDB_PORT = 6640;
    public static final String DEFAULT_OPENFLOW_PORT = "6653";
    public static final String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    public static final String UUID = "uuid";
    public static final String QOS_LINUX_HTB = "linux-htb";
    public static final String QOS_LINUX_HFSC = "linux-hfsc";
    // The following four QoS types are present in OVS 2.5+
    // Refer to http://openvswitch.org/support/dist-docs/ovs-vswitchd.conf.db.5.txt
    public static final String QOS_LINUX_SFQ = "linux-sfq";
    public static final String QOS_LINUX_CODEL = "linux-codel";
    public static final String QOS_LINUX_FQ_CODEL = "linux-fq_codel";
    public static final String QOS_EGRESS_POLICER = "egress-policer";
    public static final ImmutableBiMap<Class<? extends QosTypeBase>,String> QOS_TYPE_MAP
        = new ImmutableBiMap.Builder<Class<? extends QosTypeBase>,String>()
            .put(QosTypeLinuxHtb.class,QOS_LINUX_HTB)
            .put(QosTypeLinuxHfsc.class,QOS_LINUX_HFSC)
            .put(QosTypeLinuxSfq.class,QOS_LINUX_SFQ)
            .put(QosTypeLinuxCodel.class,QOS_LINUX_CODEL)
            .put(QosTypeLinuxFqCodel.class,QOS_LINUX_FQ_CODEL)
            .put(QosTypeEgressPolicer.class,QOS_EGRESS_POLICER)
            .build();

    public static final ImmutableBiMap<Class<? extends OvsdbBridgeProtocolBase>,String> OVSDB_PROTOCOL_MAP
        = new ImmutableBiMap.Builder<Class<? extends OvsdbBridgeProtocolBase>,String>()
            .put(OvsdbBridgeProtocolOpenflow10.class,"OpenFlow10")
            .put(OvsdbBridgeProtocolOpenflow11.class,"OpenFlow11")
            .put(OvsdbBridgeProtocolOpenflow12.class,"OpenFlow12")
            .put(OvsdbBridgeProtocolOpenflow13.class,"OpenFlow13")
            .put(OvsdbBridgeProtocolOpenflow14.class,"OpenFlow14")
            .put(OvsdbBridgeProtocolOpenflow15.class,"OpenFlow15")
            .build();

    public static final ImmutableBiMap<Class<? extends OvsdbFailModeBase>,String> OVSDB_FAIL_MODE_MAP
        = new ImmutableBiMap.Builder<Class<? extends OvsdbFailModeBase>,String>()
            .put(OvsdbFailModeStandalone.class,"standalone")
            .put(OvsdbFailModeSecure.class,"secure")
            .build();

    public static final ImmutableBiMap<String, Class<? extends InterfaceTypeBase>> OVSDB_INTERFACE_TYPE_MAP
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
            .put("stt", InterfaceTypeStt.class)
            .build();

    public static final ImmutableBiMap<Class<? extends DatapathTypeBase>,String> DATAPATH_TYPE_MAP
        = new ImmutableBiMap.Builder<Class<? extends DatapathTypeBase>,String>()
            .put(DatapathTypeSystem.class,"system")
            .put(DatapathTypeNetdev.class,"netdev")
            .build();
    public static final String IID_EXTERNAL_ID_KEY = "opendaylight-iid";
    public static final String AUTOATTACH_ID_EXTERNAL_ID_KEY = "opendaylight-autoattach-id";

    public enum VlanModes {
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

    public enum OwnershipStates {
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
}
