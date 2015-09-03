/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;


import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

//import org.apache.commons.collections4.trie;

public class VirtualSubnet  {

    private Uuid subnetUUID;
    private Uuid tenantID;
    private String name;
    private IpAddress gatewayIp;
    private Boolean   allocPoolFlag;
    private List<APool> poolList;

    // associated router
    private VirtualRouter router;
    // interface list
    private HashMap<Uuid, VirtualPort> interfaces;


    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(VirtualSubnet.class);

    void init() {
        this.interfaces  = new HashMap<>();
        this.router      = null;
        poolList         = new ArrayList();
    }

    public VirtualSubnet setSubnetUUID(Uuid subnetUUID) {
        this.subnetUUID = subnetUUID;
        return this;
    }

    public Uuid getSubnetUUID() {
        return subnetUUID;
    }

    public String getName() {
        return name;
    }

    public VirtualSubnet setName(String name) {
        this.name = name;
        return this;
    }

    public Uuid getTenantID() {
        return tenantID;
    }

    public VirtualSubnet setTenantID(Uuid tenantID) {
        this.tenantID = tenantID;
        return this;
    }

    public VirtualSubnet setGatewayIp(IpAddress gwIp) {
        this.gatewayIp = gwIp;
        return this;
    }

    public IpAddress getGatewayIp() {
        return gatewayIp;
    }

    public VirtualSubnet setAllocPoolFlag(Boolean flag) {
        allocPoolFlag = flag;
        return this;
    }

    public Boolean getAllocPoolFlag() {
        return allocPoolFlag;
    }

    public void addPool(String start, String end) {
        APool pool = new APool();
        pool.setPoolStart(start);
        pool.setPoolEnd(end);
        poolList.add(pool);
    }

    public void setRouter(VirtualRouter rtr) {
        this.router = rtr;
    }

    public VirtualRouter getRouter() {
        return router;
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
                intf.setSubnetID(null);
                intf.setSubnet(null);
            }
        }

        router.removeSubnet(this);
        return;
    }

    private class APool {
        private String poolStart;
        private String poolEnd;

        public void setPoolStart(String start) {
            this.poolStart = start;
        }

        public String getPoolStart() {
            return poolStart;
        }

        public void setPoolEnd(String end) {
            this.poolEnd = end;
        }

        public String getPoolEnd() {
            return poolEnd;
        }
    }
}
