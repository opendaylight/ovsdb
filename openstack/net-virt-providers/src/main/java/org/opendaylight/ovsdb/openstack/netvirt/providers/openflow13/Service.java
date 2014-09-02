/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

public enum Service {

    CLASSIFIER ((short) 0, "Classifier"),
    DIRECTOR ((short) 10, "Director"),
    ARP_RESPONDER ((short) 20, "Distributed ARP Responder"),
    INBOUND_NAT ((short) 30, "DNAT for inbound floating-ip traffic"),
    INGRESS_ACL ((short) 40, "Ingress Acces-control. Typically Openstack Egress Security group policies are applied here."),
    LOAD_BALANCER ((short) 50, "Distributed LBaaS"),
    ROUTING ((short) 60, "Distributed Virtual Routing (DVR)"),
    L3_FORWARDING ((short) 70, "Layer 3 forwarding/lookup service"),
    L2_REWRITE ((short) 80, "Layer2 rewrite service"),
    L2_FORWARDING ((short) 90, "Layer2 mac,vlan based forwarding"),
    EGRESS_ACL ((short) 100, "Egress Acces-control.Typically Openstack Ingress Security group policies are applied here."),
    OUTBOUND_NAT ((short) 110, "SNAT for traffic accessing external network");

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
