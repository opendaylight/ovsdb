/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbNodeDataChangeListener implements DataChangeListener, AutoCloseable {

    private ListenerRegistration<DataChangeListener> registration;
    private OvsdbConnectionManager cm;
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeDataChangeListener.class);

    OvsdbNodeDataChangeListener(DataBroker db, OvsdbConnectionManager cm) {
        LOG.info("Registering OvsdbNodeDataChangeListener");
        this.cm = cm;
        InstanceIdentifier<OvsdbNodeAugmentation> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class)
                .augmentation(OvsdbNodeAugmentation.class);
        registration =
                db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        LOG.info("Received change to ovsdbNode: {}", changes);
        for (Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            // TODO validate we have the correct kind of InstanceIdentifier
            if (created.getValue() instanceof OvsdbNodeAugmentation) {
                try {
                    cm.connect((OvsdbNodeAugmentation) created.getValue());
                } catch (UnknownHostException e) {
                    LOG.warn("Failed to connect to ovsdbNode", e);
                }
            }
        }

        Map<InstanceIdentifier<?>, DataObject> originalDataObject = changes.getOriginalData();
        Set<InstanceIdentifier<?>> iID = changes.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : iID) {
            if (originalDataObject.get(instanceIdentifier) instanceof OvsdbNodeAugmentation) {
                try {
                    cm.disconnect((OvsdbNodeAugmentation) originalDataObject.get(instanceIdentifier));
                } catch (UnknownHostException e) {
                    LOG.warn("Failed to disconnect ovsdbNode", e);
                }
            }
        }

        for (Entry<InstanceIdentifier<?>, DataObject> updated : changes.getUpdatedData().entrySet()) {
            if (updated.getValue() instanceof OvsdbNodeAugmentation) {
                OvsdbNodeAugmentation value = (OvsdbNodeAugmentation) updated.getValue();
                // OvsdbClientKey key = new OvsdbClientKey(value.getIp(), value.getPort());
                OvsdbClient client = cm.getClient(value);
                if (client != null) {
                    LOG.info("sharad - same object");
                } else {
                    for (Entry<InstanceIdentifier<?>, DataObject> original : changes.getOriginalData().entrySet()) {
                        if (original.getValue() instanceof OvsdbNodeAugmentation) {
                            try {
                                cm.disconnect((OvsdbNodeAugmentation) original.getValue());
                                cm.connect(value);
                            } catch (UnknownHostException e) {
                                LOG.warn("Failed to disconnect to ovsdbNode", e);
                            }
                        }
                    }
                }
            }
        }

        /*
         * for( Entry<InstanceIdentifier<?>, DataObject> original : changes.getOriginalData().entrySet()) {
         * if(original.getValue() instanceof OvsdbNodeAugmentation) { try {
         * cm.disconnect((OvsdbNodeAugmentation)original.getValue()); } catch (UnknownHostException e) {
         * LOG.warn("Failed to connect to ovsdbNode", e); } } }
         * 
         * for( Entry<InstanceIdentifier<?>, DataObject> updated : changes.getUpdatedData().entrySet()) { if
         * (updated.getValue() instanceof OvsdbNodeAugmentation) { try {
         * cm.connect((OvsdbNodeAugmentation)updated.getValue()); } catch (UnknownHostException e) {
         * LOG.warn("Failed to update ovsdbNode", e); } } }
         */
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
