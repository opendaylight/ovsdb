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

//import org.apache.commons.collections4.trie;

public class VirtualPort  {

    private Uuid      intfUUID;
    private Uuid      nodeUUID;
    private Uuid      subnetID;
    private Uuid      networkID;
    private IpAddress ipAddr;
    private String    macAddress;
    private Boolean   routerIntfFlag;

    // associated subnet
    private VirtualSubnet subnet = null;

    // associated router if any
    private VirtualRouter router = null;

    // TODO:: Need Openflow port

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(VirtualPort.class);

    public Uuid getIntfUUID() {
        return intfUUID;
    }

    public VirtualPort setIntfUUID(Uuid intfUUID) {
        this.intfUUID = intfUUID;
        return this;
    }

    public Uuid getNodeUUID() {
        return nodeUUID;
    }

    public VirtualPort setNodeUUID(Uuid nodeUUID) {
        this.nodeUUID = nodeUUID;
        return this;
    }

    public Uuid getSubnetID() {
        return subnetID;
    }

    public VirtualPort setSubnetID(Uuid subnetID) {
        this.subnetID = subnetID;
        return this;
    }

    public Uuid getNetworkID() {
        return intfUUID;
    }

    public VirtualPort setNetworkID(Uuid networkID) {
        this.networkID = networkID;
        return this;
    }

    public IpAddress getIpAddr() {
        return ipAddr;
    }

    public VirtualPort setIpAddr(IpAddress ipAddr) {
        this.ipAddr = ipAddr;
        return this;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public VirtualPort setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        return this;
    }

    public Boolean getRouterIntfFlag() {
        return routerIntfFlag;
    }

    public VirtualPort setRouterIntfFlag(Boolean routerIntfFlag) {
        this.routerIntfFlag = routerIntfFlag;
        return this;
    }

    public void setRouter(VirtualRouter rtr) {
        this.router = rtr;
    }

    public VirtualRouter getRouter() {
        return router;
    }

    public void setSubnet(VirtualSubnet subnet) {
        this.subnet = subnet;
    }

    public VirtualSubnet getSubnet() {
        return subnet;
    }

    public void removeSelf() {
        if (routerIntfFlag == true) {
            if (router != null) {
                router.removeInterface(this);
            }
        }

        if (subnet != null) {
            subnet.removeInterface(this);
        }
    }
}
