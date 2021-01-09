/*
 * Copyright © 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeVxlanOverIpv4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public interface HwvtepSouthboundConstants {
    TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));
    Integer DEFAULT_OVSDB_PORT = 6640;
    long PORT_OPEN_MAX_DELAY_IN_MINS = 5;
    String IID_OTHER_CONFIG_KEY = "opendaylight-iid";
    String UUID = "uuid";
    ImmutableBiMap<Class<? extends EncapsulationTypeBase>,String> ENCAPS_TYPE_MAP
        = new ImmutableBiMap.Builder<Class<? extends EncapsulationTypeBase>,String>()
            .put(EncapsulationTypeVxlanOverIpv4.class,"vxlan_over_ipv4").build();
    MacAddress UNKNOWN_DST_MAC = new MacAddress("00:00:00:00:00:00");
    String UNKNOWN_DST_STRING = "unknown-dst";
    String HWVTEP_URI_PREFIX = "hwvtep";
    String PSWITCH_URI_PREFIX = "physicalswitch";
    String LOGICALSWITCH_UUID_PREFIX = "LogicalSwitch_";
    String LOGICALROUTER_UUID_PREFIX = "LogicalRouter_";
    String ACL_UUID_PREFIX = "Acl_";
    ImmutableMap<String,String> SKIP_HWVTEP_TABLE = new ImmutableMap.Builder<String,String>()
            .put("Logical_Binding_Stats", "Update callback registration for Logical_Binding_Stats Table is skipped")
            .build();
    String VERSION_COLUMN = "_version";
    ImmutableMap<String, List<String>> SKIP_COLUMN_FROM_HWVTEP_TABLE = new ImmutableMap.Builder<String, List<String>>()
            .put("Manager", Arrays.asList(VERSION_COLUMN, "status")).build();
    int WAITING_QUEUE_CAPACITY = Integer.getInteger("hwvtep.wait.queue.capacity", 1000);
    long WAITING_JOB_EXPIRY_TIME_MILLIS = Integer.getInteger(
            "hwvtep.wait.job.expiry.time.millis", 90000);
    int STALE_HWVTEP_CLEANUP_DELAY_SECS = Integer.getInteger("stale.hwvtep.node.cleanup.delay.secs", 240);
    int HWVTEP_REGISTER_CALLBACKS_WAIT_TIMEOUT = Integer.getInteger("hwvtep.max.oper.wait.time.secs", 10);
    long IN_TRANSIT_STATE_EXPIRY_TIME_MILLIS = Integer.getInteger(
            "hwvtep.intransit.job.expiry.time.millis", 10000);
    long IN_TRANSIT_STATE_CHECK_PERIOD_MILLIS = Integer.getInteger(
            "hwvtep.intransit.job.check.period.millis", 30000);
    long CONFIG_NODE_UPDATE_MAX_DELAY_MS = Integer.getInteger(
            "config.node.update.max.delay.ms", 10000);
    int EOS_TIMEOUT = Integer.getInteger("hwvtep.eos.timeout.delay.secs", 240);
    int CHAIN_RETRY_COUNT = 10;
    long LS_REMOVE_DELAY_SECS = 5;
    int LS_REMOVE_RETRIES = 10;

}
