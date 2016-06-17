/*
 * Copyright (c) 2016 , NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;


import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TerminationPointCreateCommand;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reconciliation task to reconcile termination point configurations in the config datastore and existing
 * bridge configuration in the switch. Termination Point configuration can be added to config DS in two ways:
 * 1) Embedded as part of bridge configuration - New bridge with associated termination points are to be created at the
 *    switch as a result.
 * 2) New termination point configurations for existing bridges at the switch - Only termination points for the
 *    associated bridge are to be created
 *
 * This task handles the reconciliation for 2). The reconciliation for 1) is handled by BridgeConfigReconciliationTask
 *
 * Created by Vinh Nguyen (vinh.nguyen@hcl.com) on 6/28/16.
 */
public class TerminationPointConfigReconciliationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointConfigReconciliationTask.class);
    private final OvsdbConnectionInstance connectionInstance;

    public TerminationPointConfigReconciliationTask(final ReconciliationManager reconciliationManager,
                                                    final Node bridgeNode,
                                                    final InstanceIdentifier<?> bridgeIid,
                                                    final OvsdbConnectionInstance connectionInstance) {
        super(reconciliationManager, bridgeIid, bridgeNode);
        this.connectionInstance = connectionInstance;
    }

    @Override
    public boolean reconcileConfiguration() {
        LOG.debug("Reconcile Termination Point Configuration for node {}", ((Node) configData).getNodeId());
        final Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        changes.putAll(SouthboundMapper.extractTerminationPointConfigurationChanges((Node) configData));
        BridgeOperationalState bridgeOperationalState =
            new BridgeOperationalState(reconciliationManager.getDb(), Collections.EMPTY_LIST) {
                @Override
                public Optional<TerminationPoint> getBridgeTerminationPoint(InstanceIdentifier<?> iid) {
                    return Optional.absent();
                }
            };

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
                bridgeOperationalState, changeEvents);

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
