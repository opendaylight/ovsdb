/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeL2Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.AllowedAddressPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronPortChangeListener implements ClusteredDataTreeChangeListener<Port>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronPortChangeListener.class);

    private static ListenerRegistration<NeutronPortChangeListener> registration;
    private DataBroker db;
    private MdsalUtils mdsalUtils;

    private static final String OWNER_ROUTER_INTERFACE = "network:router_interface";
    private static final String OWNER_ROUTER_INTERFACE_DISTRIBUTED = "network:router_interface_distributed";
    private static final String OWNER_ROUTER_GATEWAY = "network:router_gateway";
    private static final String OWNER_NETWORK_DHCP = "network:dhcp";
    private static final String OWNER_FLOATING_IP = "network:floatingip";
    private static final String DEFAULT_EXT_RTR_MAC = "00:00:5E:00:01:01";
    private static final String OWNER_COMPUTE_NOVA = "compute:nova";

    public NeutronPortChangeListener(DataBroker db){
        this.db = db;
        mdsalUtils = new MdsalUtils(db);

        InstanceIdentifier<Port> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Ports.class)
                .child(Port.class);
        DataTreeIdentifier<Port> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, path);
        LOG.debug("Register netvirt listener for Neutron Port model data changes on path: {} treeId: {}", path, treeId);
        try {
            registration =
                    this.db.registerDataTreeChangeListener(treeId, this);
        } catch (final Exception e){
            LOG.warn("NeutronPortChangeListener registration failed", e);
        }
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Port>> changes) {
        LOG.trace("Neutron netvirt port data changes : {}",changes);
        for (DataTreeModification<Port> change : changes) {
            final InstanceIdentifier<Port> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Port> mod = change.getRootNode();
            LOG.trace("neutron netvirt create port. Data Tree changed update of Type={} Key={}",
                    mod.getModificationType(), key);
            switch (mod.getModificationType()) {
                case DELETE:
                    deletePort(key, mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                    updatePort(key, mod.getDataBefore(), mod.getDataAfter());
                    break;
                case WRITE:
                    if (mod.getDataBefore() == null) {
                        createPort(key, mod.getDataAfter());
                    } else {
                        updatePort(key, mod.getDataBefore(), mod.getDataAfter());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled mod type " + mod.getModificationType());

            }
        }
    }

    private void createPort(InstanceIdentifier<Port> key, Port change) {
        // add port to netvirt mdsal
        boolean result;
        LOG.debug("Create Neutron Port model data changes for key: {} add: {}", key, change);
        org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortBuilder portB;
        portB = new org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortBuilder();

        // Is this the correct way to generate a uuid when adding an entry to mdsal?
        String portUuid = java.util.UUID.randomUUID().toString();
        LOG.debug("Create Neutron Port model data changes for portUuid: {}", portUuid);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.Port> portIid =
                   MdsalHelper.createInstanceIdentifier(portUuid);

        portB.setStatus(change.getStatus());
        portB.setAdminStateUp(change.isAdminStateUp());
        portB.setName(change.getName());

        // How do these neutron fields map to netvirt port fields?
        // tenant-id
        // network-id
        // extra-dhcp-opts list
        // fixed-ips list
        // security groups
        // vif-details list
        // vif-type
        // vnic-type
        // profile
        // port-security-enabled
        // host-id

        if (change.getDeviceOwner().equalsIgnoreCase(OWNER_ROUTER_GATEWAY) ||
                change.getDeviceOwner().equalsIgnoreCase(OWNER_ROUTER_INTERFACE)) {
            portB.setPortType(PortTypeRouter.class);
        } else {
            portB.setPortType(PortTypeL2Network.class);
        }

        // how to figure out the parent?
        //portB.setParent(change.getParent());

        portB.setDeviceUuid(new Uuid(change.getDeviceId()));
        portB.setDeviceLocatorUuid(new Uuid(change.getNetworkId()));
        portB.setKey(new PortKey(new Uuid(portUuid)));

        org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPointsBuilder endPointsBuilder;
        endPointsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPointsBuilder();

        // Is the netvirt model correct? Endpoint with one mac, many ip addrs?
        // Neutron port has allowed address pairs, one mac to one ip addr.
        if (change.getAllowedAddressPairs() != null) {
            List<IpAddress> ipAddressList = new ArrayList<>();
            List<EndPoints> endpoints = new ArrayList<>();

            endPointsBuilder.setMacaddr(new MacAddress(change.getMacAddress()));
            for (AllowedAddressPairs adPairs : change.getAllowedAddressPairs()) {
                ipAddressList.add(new IpAddress(adPairs.getIpAddress().toCharArray()));
            }
            endPointsBuilder.setIpaddrs(ipAddressList);
            endpoints.add(endPointsBuilder.build());
            portB.setEndPoints(endpoints);
        }

        //write netvirt port to mdsal
        result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, portIid, portB.build());
        LOG.info("createPort:addToMdSal result: {}", result);
    }

    private void updatePort(InstanceIdentifier<Port> key, Port original, Port update) {
        LOG.debug("Update Neutron Port model data changes for original: {} update: {}", original,  update);
    }

    private void deletePort(InstanceIdentifier<Port> key, Port delete) {
        LOG.debug("Delete Neutron Port model data changes for key: {} delete: {}", key, delete);
    }


    @Override
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }
}
