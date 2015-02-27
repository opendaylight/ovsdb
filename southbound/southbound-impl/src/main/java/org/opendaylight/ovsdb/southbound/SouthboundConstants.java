/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

public class SouthboundConstants {
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVSDB_URI_PREFIX = "ovsdb";
    public static final String BRIDGE_URI_PREFIX = "bridge";
    public static final String TP_URI_PREFIX = "tp";
    public static final Integer DEFAULT_OVSDB_PORT = 6640;
}
