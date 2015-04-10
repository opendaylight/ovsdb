/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TopologyTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TopologyTypeOverlay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

import com.google.common.collect.ImmutableBiMap;

public class OverlayConstants {
    public static final TopologyId OVERLAY_TOPOLOGY_ID = new TopologyId(new Uri("overlay:1"));
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVERLAY_URI_PREFIX = "overlay";
    public static final String NODE_URI_PREFIX = "node";
    public static final String LINK_URI_PREFIX = "link";
    public static final String TUNNEL_URI_PREFIX = "tunnel";
    public static final ImmutableBiMap<String, Class<? extends TunnelTypeBase>> OVERLAY_TUNNEL_TYPE_MAP =
            new ImmutableBiMap.Builder<String, Class<? extends TunnelTypeBase>>()
                    .put("gre", TunnelTypeGre.class)
                    .put("vxlan", TunnelTypeVxlan.class)
                    .put("vxlan_gpe", TunnelTypeVxlanGpe.class)
                    .build();

    public static final ImmutableBiMap<String, Class<? extends TopologyTypeBase>> OVERLAY_TOPOLOGY_TYPE_MAP =
            new ImmutableBiMap.Builder<String, Class<? extends TopologyTypeBase>>()
            .put("overlay", TopologyTypeOverlay.class)
                    .build();
}
