/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class HwvtepSouthboundConstants {

    public static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));
    public static final String HWVTEP_URI_PREFIX = "hwvtep";
    public static final Integer DEFAULT_OVSDB_PORT = 6640;
    public static final String IID_OTHER_CONFIG_KEY = "opendaylight-iid";
    public static final String UUID = "uuid";
    public static final String DB_NAME = "hardware_vtep";
}
