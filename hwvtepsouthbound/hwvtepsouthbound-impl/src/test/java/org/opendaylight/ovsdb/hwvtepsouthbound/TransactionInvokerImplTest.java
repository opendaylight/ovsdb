/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionCommand;
import org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md.TransactionInvokerImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
public class TransactionInvokerImplTest extends AbstractConcurrentDataBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListenerTestBase.class);

    private TransactionCommand SLEEPING_PILL = new TransactionCommand() {
        @Override
        public void execute(ReadWriteTransaction transaction) {
            try {
                LOG.debug("Running sleeping pill");
                SLEEPING_PILL_STARTED_LATCH.countDown();
                SLEEPING_PILL_END_LATCH.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //ignore the error
            }
        }
    };

    private TransactionCommand NULL_POINTER_PILL = new TransactionCommand() {
        @Override
        public void execute(ReadWriteTransaction transaction) {
            LOG.debug("Running npe TransactionCommand");
            NULL_POINTER_PILL_START.countDown();
            String s = null;
            s.toString();
        }
    };

    private InstanceIdentifier<Node> nodeIid1;
    private InstanceIdentifier<Node> nodeIid2;
    private InstanceIdentifier<Node> nodeIid3;

    private DataBroker dataBroker;
    private TransactionInvokerImpl invoker;
    private CountDownLatch SLEEPING_PILL_STARTED_LATCH;
    private CountDownLatch SLEEPING_PILL_END_LATCH;
    private CountDownLatch NULL_POINTER_PILL_START;

    @Before
    public void setupTest() throws Exception {
        dataBroker = getDataBroker();
        invoker = new TransactionInvokerImpl(dataBroker);
        nodeIid1 = createInstanceIdentifier(java.util.UUID.randomUUID().toString());
        nodeIid2 = createInstanceIdentifier(java.util.UUID.randomUUID().toString());
        nodeIid3 = createInstanceIdentifier(java.util.UUID.randomUUID().toString());
        SLEEPING_PILL_STARTED_LATCH = new CountDownLatch(1);
        SLEEPING_PILL_END_LATCH = new CountDownLatch(1);
        NULL_POINTER_PILL_START = new CountDownLatch(1);
    }

    @After
    public void cleanup() throws Exception {
        deleteNode(nodeIid1);
        deleteNode(nodeIid2);
        deleteNode(nodeIid3);
    }

    private void deleteNode(InstanceIdentifier<Node> iid) {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.delete(LogicalDatastoreType.CONFIGURATION, iid);
        tx.submit();
    }

    @Test
    public void testMiddleCommandNullPointerFailure() throws Exception {
        SettableFuture ft1 = SettableFuture.create();
        SettableFuture ft2 = SettableFuture.create();
        SettableFuture ft3 = SettableFuture.create();

        //add a command which does a sleep of 500ms
        invoker.invoke(SLEEPING_PILL);

        //wait fot the above one to be scheduled
        SLEEPING_PILL_STARTED_LATCH.await(5, TimeUnit.SECONDS);

        //Now add the commands which will be picked up in one lot
        invoker.invoke(new AddNodeCmd(nodeIid1, ft1));
        invoker.invoke(NULL_POINTER_PILL);
        invoker.invoke(new AddNodeCmd(nodeIid2, ft2));

        SLEEPING_PILL_END_LATCH.countDown();

        ft1.get(5, TimeUnit.SECONDS);
        ft2.get(5, TimeUnit.SECONDS);

        NULL_POINTER_PILL_START = new CountDownLatch(1);
        invoker.invoke(NULL_POINTER_PILL);
        NULL_POINTER_PILL_START.await(5, TimeUnit.SECONDS);

        //make sure that any commands which are submitted after the previous failure run smoothly
        invoker.invoke(new AddNodeCmd(nodeIid3, ft3));
        ft3.get(5, TimeUnit.SECONDS);
    }


    private InstanceIdentifier<Node> createInstanceIdentifier(String nodeIdString) {
        NodeId nodeId = new NodeId(new Uri(nodeIdString));
        NodeKey nodeKey = new NodeKey(nodeId);
        TopologyKey topoKey = new TopologyKey(HwvtepSouthboundConstants.HWVTEP_TOPOLOGY_ID);
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, topoKey)
                .child(Node.class, nodeKey)
                .build();
    }

    private static class AddNodeCmd extends DefaultTransactionComamndImpl {
        InstanceIdentifier<Node> iid;

        AddNodeCmd(InstanceIdentifier<Node> iid, SettableFuture ft) {
            super(ft);
            this.iid = iid;
        }

        @Override
        public void execute(ReadWriteTransaction transaction) {
            NodeBuilder nodeBuilder = new NodeBuilder();
            nodeBuilder.setNodeId(iid.firstKeyOf(Node.class).getNodeId());
            HwvtepGlobalAugmentationBuilder builder = new HwvtepGlobalAugmentationBuilder();
            nodeBuilder.addAugmentation(HwvtepGlobalAugmentation.class, builder.build());
            transaction.put(LogicalDatastoreType.CONFIGURATION, iid, nodeBuilder.build(), WriteTransaction.CREATE_MISSING_PARENTS);

        }
    }

    private static class DeleteNodeCmd extends DefaultTransactionComamndImpl {
        InstanceIdentifier<Node> iid;

        DeleteNodeCmd(InstanceIdentifier<Node> iid, SettableFuture ft) {
            super(ft);
            this.iid = iid;
        }

        @Override
        public void execute(ReadWriteTransaction transaction) {
            transaction.delete(LogicalDatastoreType.CONFIGURATION, iid);
        }
    }

    private static class DefaultTransactionComamndImpl implements TransactionCommand {
        SettableFuture ft;
        DefaultTransactionComamndImpl(SettableFuture ft) {
            this.ft = ft;
        }

        @Override
        public void execute(ReadWriteTransaction transaction) {

        }

        public void setResult(ListenableFuture future) {
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    ft.set(null);
                }
                @Override
                public void onFailure(Throwable throwable) {
                    ft.setException(throwable);
                }
            });
        }
    }
}
