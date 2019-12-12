/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation;

import java.util.Collection;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.serviceutils.upgrade.rev180702.UpgradeConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbUpgradeStateListener implements ClusteredDataTreeChangeListener<UpgradeConfig>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbUpgradeStateListener.class);

    private final DataBroker dataBroker;

    /** The connection manager. */
    private final OvsdbConnectionManager cm;

    /** Our registration. */
    private final ListenerRegistration<DataTreeChangeListener<UpgradeConfig>> registration;

    public OvsdbUpgradeStateListener(final DataBroker db, OvsdbConnectionManager cm) {

        DataTreeIdentifier<UpgradeConfig> dataTreeIdentifier =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier.create(UpgradeConfig.class));
        registration = db.registerDataTreeChangeListener(dataTreeIdentifier, this);

        this.dataBroker = db;
        this.cm = cm;
        LOG.info("OvsdbUpgradeStateListener (ovsdb) initialized");
    }

    @Override
    public void close() {
        registration.close();
        LOG.info("OVSDB topology listener has been closed.");
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<UpgradeConfig>> changes) {
        LOG.trace("onDataTreeChanged: {}", changes);
        for (DataTreeModification<UpgradeConfig> change: changes) {

            if (change.getRootNode().getModificationType() == ModificationType.WRITE) {
                UpgradeConfig before = change.getRootNode().getDataBefore();
                UpgradeConfig after = change.getRootNode().getDataAfter();
                if (before != null && before.isUpgradeInProgress() && after != null && !after.isUpgradeInProgress()) {
                    LOG.info("Upgrade Flag is set from {} to {}, Trigger Reconciliation",
                        before.isUpgradeInProgress(), after.isUpgradeInProgress());
                    //TODO Trigger Reconciliation on all the ovsDbConnectionInstance
                    for (Map.Entry<ConnectionInfo, OvsdbConnectionInstance> entry : cm.getClients().entrySet()) {
                        ConnectionInfo connectionInfo = entry.getKey();
                        OvsdbConnectionInstance connectionInstance = entry.getValue();
                        LOG.trace("ConnectionInfo : {}", connectionInfo);
                        LOG.trace("OvsdbConnectionInstance : {}", connectionInstance);
                        cm.reconcileBridgeConfigurations(connectionInstance);
                    }
                }
            }

        }
    }
}
