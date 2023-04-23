/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepReconciliationTask extends ReconciliationTask {
    private final HwvtepConnectionInstance connectionInstance;
    private final DataBroker db;
    private final Node psNode;

    public HwvtepReconciliationTask(ReconciliationManager reconciliationManager,
                                    HwvtepConnectionManager connectionManager,
                                    InstanceIdentifier<?> nodeId,
                                    Node psNode,
                                    HwvtepConnectionInstance connectionInstance,
                                    DataBroker db) {
        super(reconciliationManager, connectionManager, nodeId, null);
        this.db = db;
        this.psNode = psNode;
        this.connectionInstance = connectionInstance;
    }

    private void transactChangesToDevice(final Collection<DataTreeModification<Node>> changes,
                                         final Node globalOperNode,
                                         final Node node) {
        HwvtepOperationalState hwvtepOperationalState = new HwvtepOperationalState(db, connectionInstance, changes,
                globalOperNode, node);
        hwvtepOperationalState.setInReconciliation(true);
        boolean reconcile = true;
        connectionInstance.transact(new TransactCommandAggregator(hwvtepOperationalState,changes), reconcile);
    }

    @Override
    public boolean reconcileConfiguration(HwvtepConnectionManager connectionManagerOfDevice) {
        InstanceIdentifier<Node> psNodeIid = HwvtepSouthboundMapper.createInstanceIdentifier(psNode.getNodeId());
        InstanceIdentifier<Node> nodeId = (InstanceIdentifier<Node>)nodeIid;

        ReadTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
        Node globalConfigNode = readNode(tx, LogicalDatastoreType.CONFIGURATION, nodeId);
        Node globalOpNode = readNode(tx, LogicalDatastoreType.OPERATIONAL, nodeId);
        Node psConfigNode = readNode(tx, LogicalDatastoreType.CONFIGURATION, psNodeIid);

        DataTreeModification<Node> change = null;
        Collection<DataTreeModification<Node>> changes = new ArrayList<>();
        change = GlobalConfigOperationalChangeGetter.getModification(nodeId, globalConfigNode, globalOpNode);
        changes.add(change);

        change = SwitchConfigOperationalChangeGetter.getModification(psNodeIid, psConfigNode, psNode);
        changes.add(change);

        if (globalConfigNode != null) {
            HwvtepGlobalAugmentation augmentation = globalConfigNode.augmentation(HwvtepGlobalAugmentation.class);
            if (augmentation != null) {
                Map<LogicalSwitchesKey, LogicalSwitches> switches = augmentation.getLogicalSwitches();
                if (switches != null) {
                    for (LogicalSwitches logicalSwitches : switches.values()) {
                        connectionInstance.getDeviceInfo().updateConfigData(LogicalSwitches.class,
                                nodeId.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class,
                                        logicalSwitches.key()), logicalSwitches);
                    }
                }
            }
        }
        transactChangesToDevice(changes, globalOpNode, psNode);
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

    private static Node readNode(ReadTransaction transaction,
                                 LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<Node> iid) {
        return HwvtepSouthboundUtil.readNode(transaction, logicalDatastoreType, iid).orElse(null);
    }
}
