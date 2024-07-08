/*
 * Copyright (c) 2016, 2017 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.reconciliation.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.BridgeOperationalState;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.DataChangeEvent;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TerminationPointCreateCommand;
import org.opendaylight.ovsdb.southbound.ovsdb.transact.TerminationPointDeleteCommand;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.southbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsKey;
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
    private static final PortExternalIdsKey CREATED_BY_KEY = new PortExternalIdsKey(SouthboundConstants.CREATED_BY);

    private final OvsdbConnectionInstance connectionInstance;
    private final InstanceIdentifierCodec instanceIdentifierCodec;
    private final Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
        operTerminationPoints;

    public TerminationPointConfigReconciliationTask(final ReconciliationManager reconciliationManager,
            final OvsdbConnectionManager connectionManager, final Node bridgeNode,
            final InstanceIdentifier<?> bridgeIid, final OvsdbConnectionInstance connectionInstance,
            final Map<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation>
                operTerminationPoints,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        super(reconciliationManager, connectionManager, bridgeIid, bridgeNode);
        this.connectionInstance = connectionInstance;
        this.instanceIdentifierCodec = instanceIdentifierCodec;
        this.operTerminationPoints = operTerminationPoints;
    }

    @Override
    public boolean reconcileConfiguration(final OvsdbConnectionManager connectionManager) {
        final Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        final Node configNodeData = (Node) configData;
        LOG.debug("Reconcile Termination Point Configuration for node {}", configNodeData.getNodeId());
        changes.putAll(SouthboundMapper.extractTerminationPointConfigurationChanges(configNodeData));
        DataChangeEvent changeEvents = new DataChangeEvent() {
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
        };

        connectionInstance.transact(new TerminationPointCreateCommand(connectionInstance.ops()),
                        new BridgeOperationalState(reconciliationManager.getDb(), changeEvents),
                        changeEvents, instanceIdentifierCodec);

        List<String> configTerminationPoints = new ArrayList<>();
        if (configNodeData.getTerminationPoint() != null) {
            configNodeData.getTerminationPoint().values().forEach(entry -> {
                configTerminationPoints.add(entry.getTpId().getValue());
            });
        }

        Set<InstanceIdentifier<?>> removeTerminationPoints = new HashSet<>();
        final Map<InstanceIdentifier<?>, DataObject> original = new HashMap<>();
        final InstanceIdentifier<Node> bridgeNodeIid =
                SouthboundMapper.createInstanceIdentifier(configNodeData.getNodeId());
        original.put(bridgeNodeIid, configNodeData);
        LOG.trace("Config Topology Termination Points during Reconciliation {} for bridge {}",
            configTerminationPoints, bridgeNodeIid);
        LOG.trace("Oper Topology Termination Points during Reconciliation {} for bridge {}",
            operTerminationPoints, bridgeNodeIid);
        for (Map.Entry<InstanceIdentifier<OvsdbTerminationPointAugmentation>, OvsdbTerminationPointAugmentation> entry :
                operTerminationPoints.entrySet()) {
            OvsdbTerminationPointAugmentation terminationPoint = entry.getValue();
            if (configTerminationPoints.contains(terminationPoint.getName())) {
                LOG.trace("Termination Point {} from Oper Topology also present in config topology During Reconcile",
                    terminationPoint.getName());
            } else {
                LOG.trace("Termination Point {} from Oper Topology NOT present in config topology During Reconcile,"
                        + "checking if this created by ODL and perform delete reconciliation",
                    terminationPoint.getName());
                Map<PortExternalIdsKey, PortExternalIds> externalIds = terminationPoint.getPortExternalIds();
                if (externalIds != null) {
                    final PortExternalIds portExternalIds = externalIds.get(CREATED_BY_KEY);
                    if (portExternalIds != null
                            && SouthboundConstants.ODL.equals(portExternalIds.getExternalIdValue())) {
                        LOG.trace("Termination Point {} created by ODL. Marking for deletion during reconcile",
                            entry.getKey());
                        removeTerminationPoints.add(entry.getKey());
                        original.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }
        }

        DataChangeEvent deleteChangeEvents = new DataChangeEvent() {
            @Override
            public Map<InstanceIdentifier<?>, DataObject> getCreatedData() {
                return Collections.emptyMap();
            }

            @Override
            public Map<InstanceIdentifier<?>, DataObject> getUpdatedData() {
                return original;
            }

            @Override
            public Map<InstanceIdentifier<?>, DataObject> getOriginalData() {
                return original;
            }

            @Override
            public Set<InstanceIdentifier<?>> getRemovedPaths() {
                return removeTerminationPoints;
            }
        };

        connectionInstance.transact(new TerminationPointDeleteCommand(connectionInstance.ops()),
                new BridgeOperationalState(reconciliationManager.getDb(), deleteChangeEvents),
                deleteChangeEvents, instanceIdentifierCodec);

        return true;
    }

    @Override
    public void doRetry(final boolean wasPreviousAttemptSuccessful) {
    }

    @Override
    public void checkReadinessAndProcess() {
    }

    @Override
    public long retryDelayInMills() {
        return 0;
    }
}
