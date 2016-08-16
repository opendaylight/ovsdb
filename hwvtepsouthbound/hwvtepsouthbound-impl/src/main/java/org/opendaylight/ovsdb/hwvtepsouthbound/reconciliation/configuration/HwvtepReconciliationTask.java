/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

public class HwvtepReconciliationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepReconciliationTask.class);
    private HwvtepConnectionInstance connectionInstance;
    private DataBroker db;
    private Node psNode;
    private MdsalUtils mdsalUtils;

    public HwvtepReconciliationTask(ReconciliationManager reconciliationManager,
                                    HwvtepConnectionManager connectionManager,
                                    InstanceIdentifier<?> nodeId,
                                    Node psNode,
                                    HwvtepConnectionInstance connectionInstance,
                                    DataBroker db,
                                    MdsalUtils mdsalUtils) {
        super(reconciliationManager, connectionManager, nodeId, null);
        this.db = db;
        this.psNode = psNode;
        this.connectionInstance = connectionInstance;
        this.mdsalUtils = mdsalUtils;
    }

    private void transactChangesToDevice(Collection<DataTreeModification<Node>> changes) {
        HwvtepOperationalState hwvtepOperationalState = new HwvtepOperationalState(db, changes);
        connectionInstance.transact(new TransactCommandAggregator(hwvtepOperationalState,changes));
    }

    @Override
    public boolean reconcileConfiguration(HwvtepConnectionManager connectionManagerOfDevice) {
        InstanceIdentifier<Node> psNodeIid = HwvtepSouthboundMapper.createInstanceIdentifier(psNode.getNodeId());
        InstanceIdentifier<Node> nodeId = (InstanceIdentifier<Node>)nodeIid;
        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();

        Node globalConfigNode = mdsalUtils.read(CONFIGURATION, nodeId);
        Node globalOpNode = mdsalUtils.read(OPERATIONAL, nodeId);
        Node psConfigNode = mdsalUtils.read(CONFIGURATION, psNodeIid);

        DataTreeModification<Node> change = null;
        Collection<DataTreeModification<Node>> changes = new ArrayList<>();
        change = GlobalConfigOperationalChangeGetter.getModification(nodeId, globalConfigNode, globalOpNode);
        changes.add(change);

        change = SwitchConfigOperationalChangeGetter.getModification(psNodeIid, psConfigNode, psNode);
        changes.add(change);

        transactChangesToDevice(changes);
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
