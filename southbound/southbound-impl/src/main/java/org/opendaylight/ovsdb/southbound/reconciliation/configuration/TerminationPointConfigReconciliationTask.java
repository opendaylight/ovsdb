/*
 * Copyright (c) 2016, 2017 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TerminationPointCreateCommand;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reconciliation task to reconcile termination point configurations in the config datastore.
 * When the OVS switch connects to the ODL, the list of all bridge and termination point
 * configurations in the config datastore are obtained.
 * We then listens for any new bridge show up in the operational data store.
 * If the new bridge is in the list of bridges to be reconciled as described above
 * termination point reconciliation is triggered for that bridge.
 */
public class TerminationPointConfigReconciliationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointConfigReconciliationTask.class);
    private final OvsdbConnectionInstance connectionInstance;
    private final InstanceIdentifierCodec instanceIdentifierCodec;

    public TerminationPointConfigReconciliationTask(final ReconciliationManager reconciliationManager,
            final OvsdbConnectionManager connectionManager, final Node bridgeNode,
            final InstanceIdentifier<?> bridgeIid, final OvsdbConnectionInstance connectionInstance,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        super(reconciliationManager, connectionManager, bridgeIid, bridgeNode);
        this.connectionInstance = connectionInstance;
        this.instanceIdentifierCodec = instanceIdentifierCodec;
    }

    @Override
    public boolean reconcileConfiguration(final OvsdbConnectionManager connectionManager) {
        LOG.debug("Reconcile Termination Point Configuration for node {}", ((Node) configData).getNodeId());
        final Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        changes.putAll(SouthboundMapper.extractTerminationPointConfigurationChanges((Node) configData));

        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvents = new AsyncDataChangeEvent() {
            @Override
            public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
                return changes;
            }

            @Override
            public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
                return Collections.emptyMap();
            }

            @Override
            public Map<InstanceIdentifier<?>, DataObject> getOriginalData() {
                return Collections.emptyMap();
            }

            @Override
            public Set<InstanceIdentifier<?>> getRemovedPaths() {
                return Collections.emptySet();
            }

            @Override
            public DataObject getOriginalSubtree() {
                return null;
            }

            @Override
            public DataObject getUpdatedSubtree() {
                return null;
            }
        };

        connectionInstance.transact(new TerminationPointCreateCommand(),
                        new BridgeOperationalState(reconciliationManager.getDb(), changeEvents),
                        changeEvents, instanceIdentifierCodec);

        return true;
    }

    @Override
    public void doRetry(boolean wasPreviousAttemptSuccessful) {
    }

    @Override
    public void checkReadinessAndProcess() {
    }

    @Override
    public long retryDelayInMills() {
        return 0;
    }
}
