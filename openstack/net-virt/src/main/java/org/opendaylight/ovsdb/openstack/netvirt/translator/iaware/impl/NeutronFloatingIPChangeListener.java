/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.impl;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronFloatingIPAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.floatingips.attributes.Floatingips;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.l3.rev141002.floatingips.attributes.floatingips.Floatingip;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronFloatingIPChangeListener implements DataChangeListener, AutoCloseable{
    private static final Logger LOG = LoggerFactory.getLogger(NeutronFloatingIPChangeListener.class);

    private ListenerRegistration<DataChangeListener> registration;
    private DataBroker db;

    public NeutronFloatingIPChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Floatingip> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Floatingips.class)
                .child(Floatingip.class);
        LOG.debug("Register listener for Neutron FloatingIp model data changes");
        registration =
                this.db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.trace("Data changes : {}",changes);

        Object[] subscribers = NeutronIAwareUtil.getInstances(INeutronFloatingIPAware.class, this);
        createFloatingIP(changes, subscribers);
        updateFloatingIP(changes, subscribers);
        deleteFloatingIP(changes, subscribers);
    }

    private void createFloatingIP(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> newFloatingIP : changes.getCreatedData().entrySet()) {
            NeutronFloatingIP floatingip= fromMd((Floatingip)newFloatingIP.getValue());
            for(Object entry: subscribers){
                INeutronFloatingIPAware subscriber = (INeutronFloatingIPAware)entry;
                subscriber.neutronFloatingIPCreated(floatingip);
            }
        }

    }

    private void updateFloatingIP(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (Entry<InstanceIdentifier<?>, DataObject> updateFloatingIP : changes.getUpdatedData().entrySet()) {
            NeutronFloatingIP floatingip = fromMd((Floatingip)updateFloatingIP.getValue());
            for(Object entry: subscribers){
                INeutronFloatingIPAware subscriber = (INeutronFloatingIPAware)entry;
                subscriber.neutronFloatingIPUpdated(floatingip);
            }
        }
    }

    private void deleteFloatingIP(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Object[] subscribers) {
        for (InstanceIdentifier<?> deletedFloatingIPPath : changes.getRemovedPaths()) {
            NeutronFloatingIP floatingip = fromMd((Floatingip)changes.getOriginalData().get(deletedFloatingIPPath));
            for(Object entry: subscribers){
                INeutronFloatingIPAware subscriber = (INeutronFloatingIPAware)entry;
                subscriber.neutronFloatingIPDeleted(floatingip);
            }
        }
    }

    /*
     * This method is borrowed from NeutronFloatingIPInterface.java class of Neutron Northbound class.
     * We will be utilizing similar code from other classes from the same package of neutron project.
     */
    private NeutronFloatingIP fromMd(Floatingip fip) {
        NeutronFloatingIP result = new NeutronFloatingIP();
        result.setID(String.valueOf(fip.getUuid().getValue()));
        if (fip.getFloatingNetworkId() != null) {
            result.setFloatingNetworkUUID(String.valueOf(fip.getFloatingNetworkId().getValue()));
        }
        if (fip.getPortId() != null) {
            result.setPortUUID(String.valueOf(fip.getPortId().getValue()));
        }
        if (fip.getFixedIpAddress() != null ) {
            result.setFixedIPAddress(String.valueOf(fip.getFixedIpAddress().getValue()));
        }
        if (fip.getFloatingIpAddress() != null) {
            result.setFloatingIPAddress(String.valueOf(fip.getFloatingIpAddress().getValue()));
        }
        if (fip.getTenantId() != null) {
            result.setTenantUUID(String.valueOf(fip.getTenantId().getValue()));
        }
        if (fip.getRouterId() != null) {
            result.setRouterUUID(String.valueOf(fip.getRouterId().getValue()));
        }
        result.setStatus(fip.getStatus());
        return result;
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
