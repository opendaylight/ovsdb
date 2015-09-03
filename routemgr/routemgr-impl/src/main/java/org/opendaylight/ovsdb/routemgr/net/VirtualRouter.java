/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;


import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

//import org.apache.commons.collections4.trie;

public class VirtualRouter  {

    private Uuid routerUUID;
    private Uuid tenantID;
    private String name;
    private HashMap<Uuid, VirtualSubnet> subnets;
    private HashMap<Uuid, VirtualPort>   interfaces;

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(VirtualRouter.class);

    void init() {
        this.subnets    = new HashMap<>();
        this.interfaces = new HashMap<>();
    }

    public Uuid getRouterUUID() {
        return routerUUID;
    }

    public VirtualRouter setRouterUUID(Uuid routerUUID) {
        this.routerUUID = routerUUID;
        return this;
    }

    public String getName() {
        return name;
    }

    public VirtualRouter setName(String name) {
        this.name = name;
        return this;
    }

    public Uuid getTenantID() {
        return tenantID;
    }

    public VirtualRouter setTenantID(Uuid tenantID) {
        this.tenantID = tenantID;
        return this;
    }

    public void addSubnet(VirtualSubnet snet) {
        subnets.put(snet.getSubnetUUID(), snet);
    }

    public void removeSubnet(VirtualSubnet snet) {
        subnets.remove(snet.getSubnetUUID());
    }

    public void addInterface(VirtualPort intf) {
        interfaces.put(intf.getIntfUUID(), intf);
    }

    public void removeInterface(VirtualPort intf) {
        interfaces.remove(intf.getIntfUUID());
    }

    public void removeSelf() {
        Collection<VirtualPort> intfs = interfaces.values();

        Iterator itr = intfs.iterator();
        while (itr.hasNext()) {
            VirtualPort intf = (VirtualPort) itr.next();
            if (intf != null) {
                intf.setRouter(null);
                intf.setNodeUUID(null);
            }
        }

        Collection<VirtualSubnet> snets = subnets.values();

        Iterator itr2 = snets.iterator();
        while (itr2.hasNext()) {
            VirtualSubnet snet = (VirtualSubnet) itr2.next();
            if (snet != null) {
                snet.setRouter(null);
            }
        }
        return;
    }
}
