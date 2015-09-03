/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;

import java.util.Map;
import java.util.Set;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.port.attributes.FixedIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnet.attributes.AllocationPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Registration;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.Routers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.routers.attributes.routers.Router;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.subnets.rev141002.subnets.attributes.subnets.Subnet;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.Port;

import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.networks.attributes.Networks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.networks.rev141002.networks.attributes.networks.Network;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;

public class NetDataListener implements DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(NetDataListener.class);
    private final DataBroker dataService;

    //private static final String CURRTOPO = "router:1";
    private Registration rtrListener;
    private Registration portListener;
    private Registration subnetListener;
    private Registration networkListener;

    // Interface Manager
    private IfMgr ifMgr;

    private static final String NETWORK_ROUTER_INTERFACE = "network:router_interface";


    public NetDataListener(DataBroker dataBroker) {
        this.dataService = dataBroker;
    }

    public void registerDataChangeListener() {
        LOG.debug("registering as listener for router, subnet and port");
        InstanceIdentifier<Router> rtrInstance = InstanceIdentifier.create(Neutron.class)
                .child(Routers.class)
                .child(Router.class); // , new RouterKey(new Uuid(CURRTOPO)))

        rtrListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, rtrInstance,
                                                     this, AsyncDataBroker.DataChangeScope.ONE);
        InstanceIdentifier<Subnet> subnetInstance = InstanceIdentifier.create(Neutron.class)
                .child(Subnets.class)
                .child(Subnet.class); //, new SubnetKey(new Uuid("subnet:1")))

        subnetListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, subnetInstance,
                                                             this, AsyncDataBroker.DataChangeScope.ONE);

        InstanceIdentifier<Port> portInstance = InstanceIdentifier.create(Neutron.class)
                .child(Ports.class)
                .child(Port.class); //, new PortKey(new Uuid("port:1")))

        portListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, portInstance,
                this, AsyncDataBroker.DataChangeScope.ONE);

        InstanceIdentifier<Network> networkInstance = InstanceIdentifier.create(Neutron.class)
                .child(Networks.class)
                .child(Network.class); //, new PortKey(new Uuid("port:1")))

        networkListener = dataService.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, networkInstance,
                this, AsyncDataBroker.DataChangeScope.ONE);
        LOG.info("registered");
        return;
    }
    public void closeListeners() throws Exception  {
        if (rtrListener != null) {
            rtrListener.close();
        }
        if (subnetListener != null) {
            subnetListener.close();
        }
        if (portListener != null) {
            portListener.close();
        }

        return;
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
        if (dataChangeEvent == null) {
            return;
        }
        Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
        Map<InstanceIdentifier<?>, DataObject> updatedData = dataChangeEvent.getUpdatedData();
        Set<InstanceIdentifier<?>> removedData = dataChangeEvent.getRemovedPaths();
        Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
        if ((createdData != null) && !(createdData.isEmpty())) {
            Set<InstanceIdentifier<?>> createdSet = createdData.keySet();
            for (InstanceIdentifier<?> instanceId : createdSet) {
                processInstanceId(instanceId, createdData.get(instanceId), true);
            }
        }
        if ((updatedData != null) && !(updatedData.isEmpty())) {
            Set<InstanceIdentifier<?>> updatedSet = updatedData.keySet();
            for (InstanceIdentifier<?> instanceId : updatedSet) {
                processInstanceId(instanceId, updatedData.get(instanceId), true);
            }
        }
        if ((removedData != null) && (!removedData.isEmpty()) && (originalData != null) && (!originalData.isEmpty())) {
            for (InstanceIdentifier<?> instanceId : removedData) {
                processInstanceId(instanceId, originalData.get(instanceId), false);
            }
        }
    }

    public void readDataStore() {
        // TODO:: Need to perform initial data store read
    }

    private void processInstanceId(InstanceIdentifier instanceId, DataObject data, boolean updDelFlag) {
        LOG.info("entering processInstanceId");
        if (instanceId.getTargetType().equals(Router.class)) {
            Router rtr = (Router)data;
            LOG.debug("processing router up/down events");
            if (updDelFlag == true) {
                addRouter(rtr);
            } else {
                removeRouter(rtr);
            }
        } else if (instanceId.getTargetType().equals(Subnet.class)) {
            Subnet snet = (Subnet)data;
            LOG.debug("processing subnet up/down events");
            if (updDelFlag == true) {
                addSubnet(snet);
            } else {
                removeSubnet(snet);
            }
        } else if (instanceId.getTargetType().equals(Port.class)) {
            Port port = (Port)data;
            LOG.debug("processing port up/down events");
            if (updDelFlag == true) {
                addPort(port);
            } else {
                removePort(port);
            }
        }
    }

    private void addRouter(Router rtr) {
        LOG.info("added neutron router {}", rtr);

        // Example:
        // added neutron router Router{getInterfaces=[], getName=rtr2, getRoutes=[],
        // getStatus=ACTIVE, getTenantId=Uuid [_value=2ab244af-1f65-47f4-bba8-81e645d7beef],
        // getUuid=Uuid [_value=5c9d785b-bfc1-4ad0-93da-935ce4508a36], isAdminStateUp=true, isDistributed=false,
        // augmentations={}}

        // Add router
        ifMgr.addRouter(rtr.getUuid(), rtr.getName(), rtr.getTenantId(), rtr.isAdminStateUp());

        return;
    }

    private void removeRouter(Router rtr) {
        LOG.info("remove neutron router {}", rtr);

        // remove router
        ifMgr.removeRouter(rtr.getUuid());
        return;
    }

    private void addSubnet(Subnet snet) {
        LOG.info("added neutron subnet {}", snet);

        // Example:
        // added neutron subnet Subnet{getAllocationPools=[AllocationPools{getEnd=2001:db9:0:ffff:ffff:ffff:ffff:fffe,
        // getStart=2001:db9::2, augmentations={}}],
        // getCidr=2001:db9::/48, getDnsNameservers=[],
        // getGatewayIp=IpAddress [_ipv6Address=Ipv6Address [_value=2001:db9::1],
        // _value=[2, 0, 0, 1, :, d, b, 9, :, :, 1]],
        // getIpVersion=class org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.constants.rev160807.IpVersionV6,
        // getName=snet2, getNetworkId=Uuid [_value=3b3cc735-008a-4f7c-8532-7293fc0b5697],
        // getTenantId=Uuid [_value=2ab244af-1f65-47f4-bba8-81e645d7beef],
        // getUuid=Uuid [_value=c972dbab-e7a1-418c-9189-6703dbe9f1f7],
        // isEnableDhcp=true, augmentations={}}

        List<AllocationPools> poolsList = snet.getAllocationPools();

        ifMgr.addSubnet(snet.getUuid(), snet.getName(),
                    snet.getNetworkId(), snet.getTenantId(),
                    snet.getGatewayIp(), poolsList);

        return;
    }

    private void removeSubnet(Subnet snet) {
        LOG.info("remove neutron subnet {}", snet);

        // Remove subnet
        ifMgr.removeSubnet(snet.getUuid());

        return;
    }

    private void addPort(Port port) {
        LOG.info("added neutron port {}", port);

        // Example 1:
        // added neutron port Port{getAllowedAddressPairs=[], getDeviceId=5c9d785b-bfc1-4ad0-93da-935ce4508a36,
        // getDeviceOwner=network:router_interface,
        // getFixedIps=[FixedIps{getIpAddress=IpAddress [_ipv6Address=Ipv6Address [_value=2001:db9::1],
        // _value=[2, 0, 0, 1, :, d, b, 9, :, :, 1]],
        // getSubnetId=Uuid [_value=c972dbab-e7a1-418c-9189-6703dbe9f1f7], augmentations={}}],
        // getMacAddress=FA:16:3E:01:09:65, getName=,
        // getNetworkId=Uuid [_value=3b3cc735-008a-4f7c-8532-7293fc0b5697],
        // getSecurityGroups=[],
        // getTenantId=Uuid [_value=2ab244af-1f65-47f4-bba8-81e645d7beef],
        // getUuid=Uuid [_value=c9d1f06b-65a6-4eac-aef3-7bb43b69dde9],
        // isAdminStateUp=true,
        // augmentations={interface org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.binding.rev141002
        // .PortBindingExtension=PortBindingExtension{getHostId=,
        // getVifDetails=[VifDetails{augmentations={}}], getVifType=unbound, getVnicType=normal}}}


        List<FixedIps> ipList = port.getFixedIps();

        for (FixedIps fixedip : ipList) {
            if (port.getDeviceOwner().equalsIgnoreCase(NETWORK_ROUTER_INTERFACE)) {

                // Add router interface
                ifMgr.addRouterIntf(port.getUuid(),
                        new Uuid(port.getDeviceId()),
                        fixedip.getSubnetId(),
                        port.getNetworkId(),
                        fixedip.getIpAddress(),
                        port.getMacAddress());
            } else {
                // Add host interface
                ifMgr.addHostIntf(port.getUuid(),
                        new Uuid(port.getDeviceId()),
                        fixedip.getSubnetId(),
                        port.getNetworkId(),
                        fixedip.getIpAddress(),
                        port.getMacAddress());
            }
        }

        // Example 2:
        // added neutron port Port{getAllowedAddressPairs=[],
        // getDeviceId=dhcp1627e2a8-b561-5745-8bbe-a6a7e806d5df-3b3cc735-008a-4f7c-8532-7293fc0b5697,
        // getDeviceOwner=network:dhcp, getExtraDhcpOpts=[],
        // getFixedIps=[FixedIps{getIpAddress=IpAddress [_ipv6Address=Ipv6Address [_value=2001:db9::2],
        // _value=[2, 0, 0, 1, :, d, b, 9, :, :, 2]],
        // getSubnetId=Uuid [_value=c972dbab-e7a1-418c-9189-6703dbe9f1f7], augmentations={}}],
        // getMacAddress=FA:16:3E:4E:7F:63, getName=,
        // getNetworkId=Uuid [_value=3b3cc735-008a-4f7c-8532-7293fc0b5697],
        // getSecurityGroups=[],
        // getTenantId=Uuid [_value=2ab244af-1f65-47f4-bba8-81e645d7beef],
        // getUuid=Uuid [_value=946e5408-68d0-40d6-957d-826c9e508bbb],
        // isAdminStateUp=true, augmentations={interface org.opendaylight.yang.gen.v1.urn.opendaylight
        // .neutron.binding.rev141002.PortBindingExtension=PortBindingExtension{getHostId=ma-os,
        // getVifDetails=[VifDetails{isPortFilter=true, augmentations={}}],
        // getVifType=ovs, getVnicType=normal}}}


        return;
    }

    private void removePort(Port port) {
        LOG.info("remove neutron port {}", port);

        // remove port
        ifMgr.removePort(port.getUuid());

        return;
    }
}
