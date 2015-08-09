/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

public enum Service {

    CLASSIFIER ((short) 0, "Classifier"),
    GATEWAY_RESOLVER((short) 0, "External Network Gateway Resolver"),
    DIRECTOR ((short) 10, "Director"),
    ARP_RESPONDER ((short) 20, "Distributed ARP Responder"),
    INBOUND_NAT ((short) 30, "DNAT for inbound floating-ip traffic"),
    EGRESS_ACL ((short) 40, "Egress Acces-control"),
    LOAD_BALANCER ((short) 50, "Distributed LBaaS"),
    ROUTING ((short) 60, "Distributed Virtual Routing (DVR)"),
    L3_FORWARDING ((short) 70, "Layer 3 forwarding/lookup service"),
    L2_REWRITE ((short) 80, "Layer2 rewrite service"),
    INGRESS_ACL ((short) 90, "Ingress Acces-control"),
    OUTBOUND_NAT ((short) 100, "SNAT for traffic accessing external network"),
    L2_FORWARDING ((short) 110, "Layer2 mac,vlan based forwarding");

    short table;
    String description;

    private Service (short table, String description)  {
        this.table = table;
        this.description = description;
    }

    public short getTable() {
        return table;
    }

    public String getDescription() {
        return description;
    }
}
