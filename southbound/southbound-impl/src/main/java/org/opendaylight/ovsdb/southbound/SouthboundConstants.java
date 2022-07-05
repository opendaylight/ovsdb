/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.QosEntryKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class SouthboundConstants {
    public enum VlanModes {
        ACCESS("access"),
        NATIVE_TAGGED("native-tagged"),
        NATIVE_UNTAGGED("native-untagged"),
        TRUNK("trunk");

        private final String mode;

        VlanModes(final String mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return mode;
        }

        public String getMode() {
            return mode;
        }
    }

    public static final String OPEN_V_SWITCH = "Open_vSwitch";
    public static final @NonNull TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    public static final String TP_URI_PREFIX = "terminationpoint";
    public static final String QOS_URI_PREFIX = "qos";
    public static final String QOS_NAMED_UUID_PREFIX = "QOS";
    public static final @NonNull QosEntryKey PORT_QOS_LIST_KEY = new QosEntryKey(Uint32.ONE);
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
    public static final String URI_SEPERATOR = "/";
    public static final String CREATED_BY = "created_by";
    public static final String ODL = "odl";

    public static final ImmutableBiMap<OvsdbBridgeProtocolBase, String> OVSDB_PROTOCOL_MAP =
        ImmutableBiMap.<OvsdbBridgeProtocolBase, String>builder()
            .put(OvsdbBridgeProtocolOpenflow10.VALUE, "OpenFlow10")
            .put(OvsdbBridgeProtocolOpenflow11.VALUE, "OpenFlow11")
            .put(OvsdbBridgeProtocolOpenflow12.VALUE, "OpenFlow12")
            .put(OvsdbBridgeProtocolOpenflow13.VALUE, "OpenFlow13")
            .put(OvsdbBridgeProtocolOpenflow14.VALUE, "OpenFlow14")
            .put(OvsdbBridgeProtocolOpenflow15.VALUE, "OpenFlow15")
            .build();

    public static final ImmutableBiMap<OvsdbFailModeBase, String> OVSDB_FAIL_MODE_MAP = ImmutableBiMap.of(
            OvsdbFailModeStandalone.VALUE, "standalone",
            OvsdbFailModeSecure.VALUE,     "secure");

    public static final String IID_EXTERNAL_ID_KEY = "opendaylight-iid";
    public static final String QOS_ID_EXTERNAL_ID_KEY = "opendaylight-qos-id";
    public static final String QUEUE_ID_EXTERNAL_ID_KEY = "opendaylight-queue-id";
    public static final String AUTOATTACH_ID_EXTERNAL_ID_KEY = "opendaylight-autoattach-id";

    // Note: _version is an internal column of ovsdb schema, that gets updated with every change in the row
    //       of the table.
    //       The "Manager" entry needs to be a modifiable list, SouthboundProvider::setSkipManagerStatus() modifies it
    static final ImmutableMap<String, List<String>> SKIP_COLUMN_FROM_TABLE =
        ImmutableMap.<String,List<String>>builder()
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

    private SouthboundConstants() {
        // Hidden on purpose
    }
}
