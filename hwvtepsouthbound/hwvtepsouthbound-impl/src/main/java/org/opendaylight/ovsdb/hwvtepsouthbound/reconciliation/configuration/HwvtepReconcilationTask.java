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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

public class HwvtepReconcilationTask extends ReconciliationTask {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepReconcilationTask.class);
    private HwvtepConnectionInstance connectionInstance;
    private DataBroker db;
    private Node psNode;

    public HwvtepReconcilationTask(ReconciliationManager reconciliationManager,
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

    private void transactChangesToDevice(Collection<DataTreeModification<Node>> changes) {
        HwvtepOperationalState hwvtepOperationalState = new HwvtepOperationalState(db, changes);
        connectionInstance.transact(new TransactCommandAggregator(hwvtepOperationalState,changes));
    }

    Node readNode(ReadOnlyTransaction tx, InstanceIdentifier<Node> iid, LogicalDatastoreType datastoreType)
            throws InterruptedException, ExecutionException {
        CheckedFuture<Optional<Node>, ReadFailedException> nodeOptional =
                tx.read(datastoreType, iid);
        if (nodeOptional.get().isPresent())
            return nodeOptional.get().get();
        else
            return null;//Yes config could be null on a fresh system
    }


    @Override
    public boolean reconcileConfiguration(HwvtepConnectionManager connectionManagerOfDevice) {
        try {
            InstanceIdentifier<Node> psNodeIid = HwvtepSouthboundMapper.createInstanceIdentifier(psNode.getNodeId());
            ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();

            Node globalConfigNode = readNode(tx, (InstanceIdentifier<Node>)nodeIid, CONFIGURATION);
            Node globalOpNode = readNode(tx, (InstanceIdentifier<Node>) nodeIid, OPERATIONAL);
            Node psConfigNode = readNode(tx, psNodeIid, CONFIGURATION);

            DataTreeModification<Node> change = null;
            Collection<DataTreeModification<Node>> changes = new ArrayList<>();
            change = GlobalConfigOperationalChangeGetter.getModification(
                    (InstanceIdentifier<Node>) nodeIid, globalConfigNode, globalOpNode);
            changes.add(change);

            change = SwitchConfigOperationalChangeGetter.getModification(psNodeIid,psConfigNode, psNode);
            changes.add(change);

            transactChangesToDevice(changes);
        } catch (InterruptedException e) {
            LOG.warn("Failed to process hwvtep reconcilation interrupted ", e);
        } catch (ExecutionException e) {
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
