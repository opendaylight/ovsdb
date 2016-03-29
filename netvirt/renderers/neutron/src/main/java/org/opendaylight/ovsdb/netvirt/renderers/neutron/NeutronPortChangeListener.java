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
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.Port;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortBuilder;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;


/* Multiple Port, PortBuilder. What is best way to resolve? */
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.EndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.PortTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.port.attributes.AllowedAddressPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.PortsBuilder;
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

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.Port> portUuid =
                    MdsalHelper.createInstanceIdentifier();

        portB.setPortType(PortTypeBase.class);
        // Need to find the uuid of the parent, based on portType above.
        //portB.setParent(change.getParent());

        portB.setDeviceUuid(new Uuid(change.getDeviceId()));
        portB.setDeviceLocatorUuid(new Uuid(change.getNetworkId()));

        org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPointsBuilder endPointsBuilder;
        endPointsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.port.EndPointsBuilder();

        if (change.getAllowedAddressPairs() != null) {
            List<IpAddress> ipAddressList = new ArrayList<>();
            endPointsBuilder.setMacaddr(new MacAddress(change.getMacAddress()));
            for (AllowedAddressPairs adPairs : change.getAllowedAddressPairs()) {
                //FIXME - needs to work for IpV6 also
                ipAddressList.add(new IpAddress(new Ipv4Address(adPairs.getIpAddress())));
            }
            endPointsBuilder.setIpaddrs(ipAddressList);
            //??
            //portB.setEndPoints(endPointsBuilder.build());
        }

        //write netvirt port to mdsal
        result = mdsalUtils.put(LogicalDatastoreType.CONFIGURATION, portUuid, portB.build());
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
