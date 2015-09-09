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
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnet.attributes.AllocationPools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IfMgr  {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(IfMgr.class);

    // router objects - routers, subnets, interfaces
    private HashMap<Uuid, VirtualRouter>      vrouters;
    private HashMap<Uuid, VirtualSubnet>      vsubnets;
    private HashMap<Uuid, VirtualPort>        vintfs;
    private HashMap<Uuid, List<VirtualPort>>  unprocessedRouterIntfs;
    private HashMap<Uuid, List<VirtualPort>>  unprocessedSubnetIntfs;

    void init() {
        this.vrouters               = new HashMap<>();
        this.vsubnets               = new HashMap<>();
        this.vintfs                 = new HashMap<>();
        this.unprocessedRouterIntfs = new HashMap<>();
        this.unprocessedSubnetIntfs = new HashMap<>();
        logger.info("IfMgr is enabled");
    }

    public void addRouter(Uuid rtrUuid, String rtrName, Uuid tenantId, Boolean isAdminStateUp) {

        VirtualRouter rtr = new VirtualRouter();
        if (rtr != null) {
            rtr.setTenantID(tenantId)
                .setRouterUUID(rtrUuid)
                .setName(rtrName);
            vrouters.put(rtrUuid, rtr);

            List<VirtualPort> intfList = unprocessedRouterIntfs.get(rtrUuid);

            for (VirtualPort intf : intfList) {
                if (intf != null) {
                    intf.setRouter(rtr);
                    rtr.addInterface(intf);

                    VirtualSubnet snet = intf.getSubnet();
                    if (snet != null) {
                        snet.setRouter(rtr);
                    }
                }
            }

            removeUnprocessed(unprocessedRouterIntfs, rtrUuid);

        } else {
            logger.error("Create router failed for :{}", rtrUuid);
        }

        return;
    }

    public void removeRouter(Uuid rtrUuid) {

        VirtualRouter rtr = vrouters.get(rtrUuid);
        if (rtr != null) {
            rtr.removeSelf();
            vrouters.remove(rtrUuid);
            removeUnprocessed(unprocessedRouterIntfs, rtrUuid);
            rtr = null;
        } else {
            logger.error("Delete router failed for :{}", rtrUuid);
        }
        return;
    }

    public void addSubnet(Uuid snetId, String name, Uuid networkId, Uuid tenantId,
                          IpAddress gatewayIp, List<AllocationPools> poolsList) {

        VirtualSubnet snet = new VirtualSubnet();
        if (snet != null) {
            snet.setTenantID(tenantId)
                    .setSubnetUUID(snetId)
                    .setName(name)
                    .setGatewayIp(gatewayIp);

            // Add address pool
            for (AllocationPools pool : poolsList) {
                snet.addPool(pool.getStart(), pool.getEnd());
            }

            vsubnets.put(snetId, snet);

            List<VirtualPort> intfList = unprocessedSubnetIntfs.get(snetId);

            for (VirtualPort intf : intfList) {
                if (intf != null) {
                    intf.setSubnet(snet);
                    snet.addInterface(intf);

                    VirtualRouter rtr = intf.getRouter();
                    if (rtr != null) {
                        rtr.addSubnet(snet);
                    }
                }
            }

            removeUnprocessed(unprocessedSubnetIntfs, snetId);

        } else {
            logger.error("Create subnet failed for :{}", snetId);
        }
        return;
    }

    public void removeSubnet(Uuid snetId) {

        VirtualSubnet snet = vsubnets.get(snetId);
        if (snet != null) {
            snet.removeSelf();
            vsubnets.remove(snetId);
            removeUnprocessed(unprocessedSubnetIntfs, snetId);
            snet = null;
        } else {
            logger.error("Delete subnet failed for :{}", snetId);
        }
        return;
    }

    public void addRouterIntf(Uuid portId, Uuid rtrId, Uuid snetId,
                              Uuid networkId, IpAddress fixedIp, String macAddress) {
        VirtualPort intf = vintfs.get(portId);
        if (intf == null) {
            intf = new VirtualPort();
            if (intf != null) {
                vintfs.put(portId, intf);
            }  else {
                logger.error("Create rtr intf failed for :{}", portId);
            }
        }

        if (intf != null) {
            intf.setIntfUUID(portId)
                    .setNodeUUID(rtrId)
                    .setSubnetID(snetId)
                    .setIpAddr(fixedIp)
                    .setNetworkID(networkId)
                    .setMacAddress(macAddress)
                    .setRouterIntfFlag(true);

            VirtualRouter rtr = vrouters.get(rtrId);
            VirtualSubnet snet = vsubnets.get(snetId);

            if (rtr != null && snet != null) {
                snet.setRouter(rtr);
                intf.setSubnet(snet);
                rtr.addSubnet(snet);
            } else if (snet != null) {
                intf.setSubnet(snet);
                addUnprocessed(unprocessedRouterIntfs, rtrId, intf);
            } else {
                addUnprocessed(unprocessedRouterIntfs, rtrId, intf);
                addUnprocessed(unprocessedSubnetIntfs, snetId, intf);
            }
        }
        return;
    }

    public void addHostIntf(Uuid portId, Uuid hostId, Uuid snetId,
                            Uuid networkId, IpAddress fixedIp, String macAddress) {
        VirtualPort intf = vintfs.get(portId);
        if (intf == null) {
            intf = new VirtualPort();
            if (intf != null) {
                vintfs.put(portId, intf);
            }  else {
                logger.error("Create host intf failed for :{}", portId);
            }
        }

        if (intf != null) {
            intf.setIntfUUID(portId)
                    .setNodeUUID(hostId)
                    .setSubnetID(snetId)
                    .setIpAddr(fixedIp)
                    .setNetworkID(networkId)
                    .setMacAddress(macAddress)
                    .setRouterIntfFlag(false);

            VirtualSubnet snet = vsubnets.get(snetId);

            if (snet != null) {
                intf.setSubnet(snet);
            } else {
                addUnprocessed(unprocessedSubnetIntfs, snetId, intf);
            }
        }
        return;
    }

    public void updateInterface(Uuid portId, String dpId, Long ofPort) {
        VirtualPort intf = vintfs.get(portId);

        if (intf == null) {
            intf = new VirtualPort();
            if (intf != null) {
                vintfs.put(portId, intf);
            }  else {
                logger.error("updateInterface failed for :{}", portId);
            }
        }

        if (intf != null) {
            intf.setDpId(dpId)
                    .setOfPort(ofPort);
        }
        return;
    }

    public void removePort(Uuid portId) {
        VirtualPort intf = vintfs.get(portId);
        if (intf != null) {
            intf.removeSelf();
            vintfs.remove(portId);
            intf = null;
        } else {
            logger.error("Delete intf failed for :{}", portId);
        }
        return;
    }

    public void deleteInterface(Uuid interfaceUuid, String dpId) {
        // Nothing to do for now
        return;
    }

    public void addUnprocessed(HashMap<Uuid, List<VirtualPort>> unprocessed, Uuid id, VirtualPort intf) {

        List<VirtualPort> intfList = unprocessed.get(id);

        if (intfList == null) {
            intfList = new ArrayList();
            intfList.add(intf);
            unprocessed.put(id, intfList);
        } else {
            intfList.add(intf);
        }
        return;
    }

    public void removeUnprocessed(HashMap<Uuid, List<VirtualPort>> unprocessed, Uuid id) {

        List<VirtualPort> intfList = unprocessed.get(id);
        intfList = null;
        return;
    }

}
