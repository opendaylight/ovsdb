/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeVxlanOverIpv4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

public class HwvtepSouthboundConstants {

    public static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));
    public static final Integer DEFAULT_OVSDB_PORT = 6640;
    public static final String IID_OTHER_CONFIG_KEY = "opendaylight-iid";
    public static final String UUID = "uuid";
    public static final ImmutableBiMap<Class<? extends EncapsulationTypeBase>,String> ENCAPS_TYPE_MAP
    = new ImmutableBiMap.Builder<Class<? extends EncapsulationTypeBase>,String>()
        .put(EncapsulationTypeVxlanOverIpv4.class,"vxlan_over_ipv4")
        .build();
    public static final MacAddress UNKNOWN_DST_MAC = new MacAddress("00:00:00:00:00:00");
    public static final String UNKNOWN_DST_STRING = "unknown-dst";
    public static final String HWVTEP_URI_PREFIX = "hwvtep";
    public static final String PSWITCH_URI_PREFIX = "physicalswitch";
    public static final String LOGICALSWITCH_UUID_PREFIX = "LogicalSwitch_";
    public static final String LOGICALROUTER_UUID_PREFIX = "LogicalRouter_";
    public static final String ACL_UUID_PREFIX = "Acl_";
    public static final ImmutableMap<String,String> SKIP_HWVTEP_TABLE
            = new ImmutableMap.Builder<String,String>()
            .put("Logical_Binding_Stats", "Update callback registration for Logical_Binding_Stats Table is skipped")
            .build();

    public static final ImmutableMap<String,List<String>> SKIP_COLUMN_FROM_HWVTEP_TABLE
            = new ImmutableMap.Builder<String,List<String>>()
            .put("Manager", Arrays.asList(new String[]{"_version", "status"}))
            .build();
    public static final int WAITING_QUEUE_CAPACITY = 1000;
    public static final long WAITING_JOB_EXPIRY_TIME_MILLIS = 90000;
}
