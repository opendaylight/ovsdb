/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class HwvtepReconcilationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepReconcilationTask.class);
    private HwvtepConnectionInstance connectionInstance;
    private DataBroker db;

    public HwvtepReconcilationTask(ReconciliationManager reconciliationManager,
                                   HwvtepConnectionManager connectionManager,
                                   InstanceIdentifier<?> nodeId,
                                   HwvtepConnectionInstance connectionInstance,
                                   DataBroker db) {
        super(reconciliationManager, connectionManager, nodeId, null);
        this.db = db;
        this.connectionInstance = connectionInstance;
    }

    private void transactChangesToDevice(Collection<DataTreeModification<Node>> changes) {
        HwvtepOperationalState hwvtepOperationalState = new HwvtepOperationalState(db, changes);
        connectionInstance.transact(new TransactCommandAggregator(hwvtepOperationalState,changes));
    }

    Node getConfigNode() throws InterruptedException, ExecutionException {
        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> configNodeOptional =
                tx.read(CONFIGURATION, (InstanceIdentifier<Node>)nodeIid);
        if (configNodeOptional.get().isPresent())
            return configNodeOptional.get().get();
        else
            return null;//Yes config could be null on a fresh system
    }

    Node getOperationalNode() throws InterruptedException, ExecutionException {
        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> operationalNodeOptional =
                tx.read(OPERATIONAL, (InstanceIdentifier<Node>)nodeIid);
        return operationalNodeOptional.get().get();
    }

    @Override
    public boolean reconcileConfiguration(HwvtepConnectionManager connectionManagerOfDevice) {
        try {
            Collection<DataTreeModification<Node>> changes = new ArrayList<>();
            Node configNode = getConfigNode();
            Node operationalNode = getOperationalNode();

            DataTreeModification<Node> change = GlobalConfigOperationalChangeGetter.getModification(
                    (InstanceIdentifier<Node>) nodeIid, configNode, operationalNode);
            changes.add(change);
            transactChangesToDevice(changes);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to process hwvtep reconcilation", e);
        }
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
