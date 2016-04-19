/*
 * Copyright (c) 2016 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.utils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class MdsalUtilsAsyncTest extends AbstractDataBrokerTest {

    private MdsalUtilsAsync mdsalUtilsAsync;

    private DataBroker databroker;


    abstract class Tests implements DataObject {}

    private static final TopologyId TOPOLOGY_TEST = new TopologyId("test:1");

    private static final NodeId id = new NodeId("test");
    private static final NodeKey key =  new NodeKey(id);
    private static final Node data = new NodeBuilder().setKey(key).setNodeId(id).build();

    private static final InstanceIdentifier<Node> TEST_IID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(TOPOLOGY_TEST))
            .child(Node.class, key);

    @Before
    public void setUp() {
        databroker = getDataBroker();
        mdsalUtilsAsync = Mockito.spy(new MdsalUtilsAsync(databroker));
    }

    @Test
    public void testDelete() {
        final CheckedFuture<Void, TransactionCommitFailedException> fut = mdsalUtilsAsync.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data);
        Futures.addCallback(fut, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                final CheckedFuture<Void, TransactionCommitFailedException> future = mdsalUtilsAsync.delete(LogicalDatastoreType.CONFIGURATION, TEST_IID);
                Futures.addCallback(future, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(final Void result) {
                        Assert.assertNull(readDS());
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        Assert.fail(t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                Assert.fail(t.getMessage());
            }
        });
    }

    @Test
    public void testPutWithoutCallback() {
        final String operationDesc = "testPut";
        final SupportingNode supportingNodeBuilder1 = new SupportingNodeBuilder().setKey(new SupportingNodeKey(new NodeId("id1"), TOPOLOGY_TEST)).build();
        final SupportingNode supportingNodeBuilder2 = new SupportingNodeBuilder().setKey(new SupportingNodeKey(new NodeId("id2"), TOPOLOGY_TEST)).build();

        final Node data1 = new NodeBuilder(data).setSupportingNode(Arrays.asList(supportingNodeBuilder1)).build();
        final Node data2 = new NodeBuilder(data).setSupportingNode(Arrays.asList(supportingNodeBuilder2)).build();

        mdsalUtilsAsync.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data1, operationDesc);
        Assert.assertEquals(data1, readDS());

        final CheckedFuture<Void, TransactionCommitFailedException> future = mdsalUtilsAsync.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data2);
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                Assert.assertEquals(1, readDS().getSupportingNode().size());
            }

            @Override
            public void onFailure(final Throwable t) {
                Assert.fail(t.getMessage());
            }
        });
    }

    @Test
    public void testMerge() {
        final String operationDesc = "testMerge";
        final SupportingNode supportingNodeBuilder1 = new SupportingNodeBuilder().setKey(new SupportingNodeKey(new NodeId("id1"), TOPOLOGY_TEST)).build();
        final SupportingNode supportingNodeBuilder2 = new SupportingNodeBuilder().setKey(new SupportingNodeKey(new NodeId("id2"), TOPOLOGY_TEST)).build();

        final Node data1 = new NodeBuilder(data).setSupportingNode(Arrays.asList(supportingNodeBuilder1)).build();
        final Node data2 = new NodeBuilder(data).setSupportingNode(Arrays.asList(supportingNodeBuilder2)).build();

        mdsalUtilsAsync.merge(LogicalDatastoreType.CONFIGURATION, TEST_IID, data1, operationDesc, true);
        Assert.assertEquals(data1, readDS());

        final CheckedFuture<Void, TransactionCommitFailedException> future = mdsalUtilsAsync.merge(LogicalDatastoreType.CONFIGURATION, TEST_IID, data2, true);
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                Assert.assertEquals(2, readDS().getSupportingNode().size());
            }

            @Override
            public void onFailure(final Throwable t) {
                Assert.fail(t.getMessage());
            }
        });
    }

    @Test
    public void testRead() {
        final CheckedFuture<Void, TransactionCommitFailedException> fut = mdsalUtilsAsync.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data);

        Futures.addCallback(fut, new FutureCallback<Void>() {

            @Override
            public void onSuccess(final Void result) {
                final CheckedFuture<Optional<Node>, ReadFailedException> future = mdsalUtilsAsync.read(LogicalDatastoreType.CONFIGURATION, TEST_IID);
                Optional<Node> optNode;
                try {
                    optNode = future.get();
                    if (optNode.isPresent()) {
                        Assert.assertEquals(data, optNode.get());
                    } else {
                        Assert.fail("Couldn't read node");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Assert.fail(e.getMessage());
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                Assert.fail(t.getMessage());
            }
        });
    }

    private Node readDS() {
        try {
            final Optional<Node> result = databroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, TEST_IID).get();
            if (result.isPresent()) {
                return result.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }
}
