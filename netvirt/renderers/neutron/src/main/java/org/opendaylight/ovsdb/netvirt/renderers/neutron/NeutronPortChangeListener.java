/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import java.util.*;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev150712.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronPortChangeListener implements ClusteredDataTreeChangeListener<Port>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NeutronPortChangeListener.class);

    private ListenerRegistration<NeutronPortChangeListener> registration;
    private DataBroker db;

    public NeutronPortChangeListener(DataBroker db){
        this.db = db;
        InstanceIdentifier<Port> path = InstanceIdentifier
                .create(Neutron.class)
                .child(Ports.class)
                .child(Port.class);
        LOG.debug("Register netvirt listener for Neutron Port model data changes on path: {}", path);
        DataTreeIdentifier<Port> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, path);
        registration =
                this.db.registerDataTreeChangeListener(treeId, NeutronPortChangeListener.this);

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

    private void createPort(InstanceIdentifier<Port> key, Port add) {
        // add port to netvirt mdsal


    }

    private void updatePort(InstanceIdentifier<Port> key, Port original, Port update) {
    }

    private void deletePort(InstanceIdentifier<Port> key, Port delete) {
    }


    @Override
    public void close() throws Exception {
        registration.close();
    }
}
