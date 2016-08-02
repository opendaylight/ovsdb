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
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.TransactCommandAggregator;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationManager;
import org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.ReconciliationTask;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
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
    private final HwvtepConnectionInstance connectionInstance;

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

    private void updateData(Collection<DataTreeModification<Node>> changes) {
        HwvtepOperationalState hwvtepOperationalState = new HwvtepOperationalState(db, changes);
        connectionInstance.transact(new TransactCommandAggregator(hwvtepOperationalState,changes));
    }

    Node getConfigNode() throws InterruptedException, ExecutionException {
        ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> configNodeOptional =
                tx.read(CONFIGURATION, (InstanceIdentifier<Node>)nodeIid);
        return configNodeOptional.get().get();
    }

    Node getOperationalNode() throws InterruptedException, ExecutionException {
        //TODO add a listener instead of this busy loop
        CheckedFuture<Optional<Node>, ReadFailedException> operationalNodeOptional = null;
        do {
            ReadOnlyTransaction tx = reconciliationManager.getDb().newReadOnlyTransaction();
            operationalNodeOptional = tx.read(OPERATIONAL, (InstanceIdentifier<Node>)nodeIid);
            Thread.sleep(1000);
        } while (!operationalNodeOptional.get().isPresent());

        return operationalNodeOptional.get().get();;
    }

    @Override
    public boolean reconcileConfiguration(HwvtepConnectionManager connectionManagerOfDevice) {
        try {
            Collection<DataTreeModification<Node>> changes = new ArrayList<>();
            Node configNode = getConfigNode();
            Node operationalNode = getOperationalNode();
            DataTreeModification<Node> logicalSwitchChanges = getLogicalSwitchesChanges(configNode, operationalNode);
            changes.add(logicalSwitchChanges);
            updateData(changes);
        } catch (Exception e) {
            LOG.error("failed to process logical switch reconcilation",e);
        }
        return true;
    }


    DataTreeModification<Node> getLogicalSwitchesChanges(Node node, Node oldNode) {
        NodeBuilder newNodeBuilder = new NodeBuilder(node);
        NodeBuilder oldNodeBuilder = new NodeBuilder(oldNode);

        List<LogicalSwitches> newLogicalSwitches = null;
        List<LogicalSwitches> oldLogicalSwitches = null;
        if (node.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
            newLogicalSwitches = node.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        }
        if (oldNode.getAugmentation(HwvtepGlobalAugmentation.class) != null) {
            oldLogicalSwitches = oldNode.getAugmentation(HwvtepGlobalAugmentation.class).getLogicalSwitches();
        }
        newNodeBuilder.removeAugmentation(HwvtepGlobalAugmentation.class);
        newNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class,
                new HwvtepGlobalAugmentationBuilder().setLogicalSwitches(newLogicalSwitches).build());

        oldNodeBuilder.removeAugmentation(HwvtepGlobalAugmentation.class);
        oldNodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class,
                new HwvtepGlobalAugmentationBuilder().setLogicalSwitches(oldLogicalSwitches).build());

        return getDataTreeModificationForNode((InstanceIdentifier<Node>) nodeIid, newNodeBuilder, oldNodeBuilder);
    }

    DataTreeModification getDataTreeModificationForNode(InstanceIdentifier<Node> nodeId, NodeBuilder newNodeBuilder,
                                                        NodeBuilder oldNodeBuilder) {
        return new DataTreeModificationImpl<Node>(nodeId, newNodeBuilder.build(), oldNodeBuilder.build());
    }

    @Override
    public void doRetry(boolean wasPreviousAttemptSuccessful) {
        //TODO
    }

    @Override
    public void checkReadinessAndProcess() {
        //TODO
    }

    @Override
    public long retryDelayInMills() {
        return 0;
    }

}
