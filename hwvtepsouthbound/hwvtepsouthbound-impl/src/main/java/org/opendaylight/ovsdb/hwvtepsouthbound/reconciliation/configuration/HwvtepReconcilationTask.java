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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.ha.HAUtil;
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

    Node getConfigNode() throws InterruptedException, ExecutionException {
        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> configNodeOptional =
                tx.read(CONFIGURATION, (InstanceIdentifier<Node>)nodeIid);
        if (configNodeOptional.get().isPresent())
            return configNodeOptional.get().get();
        else
            return null;//Yes config could be null on a fresh system
    }

    public static InstanceIdentifier<Node> createInstanceIdentifier(String nodeIdString) {
        NodeId nodeId = new NodeId(new Uri(nodeIdString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    Node getConfigPsNode(InstanceIdentifier<Node> psNodeId, Node opPsNode) throws InterruptedException, ExecutionException {

        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> configNodeOptional =
                tx.read(CONFIGURATION, psNodeId);
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
            LOG.error("starting reconcileConfiguration to device node id {} ", psNode.getNodeId().getValue());
            Collection<DataTreeModification<Node>> changes = new ArrayList<>();
            Node configNode = getConfigNode();
            Node operationalNode = getOperationalNode();

            DataTreeModification<Node> change = GlobalConfigOperationalChangeGetter.getModification(
                    (InstanceIdentifier<Node>) nodeIid, configNode, operationalNode);
            changes.add(change);

            InstanceIdentifier<Node> psNodeId = createInstanceIdentifier(psNode.getNodeId().getValue());
            DataTreeModification<Node> switchChange = SwitchConfigOperationalChangeGetter.getModification(
                    psNodeId,
                    getConfigPsNode(psNodeId, psNode), psNode);
            changes.add(switchChange);

            transactChangesToDevice(changes);
            LOG.error("reconciled to device node id {} ",
                    psNode.getNodeId().getValue());
        } catch (Exception e) {
            LOG.error("Failed to process hwvtep reconcilation  "+psNode.getNodeId().getValue(), e);
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
