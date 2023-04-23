/*
 * Copyright © 2016, 2017 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.mdsal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class MdsalUtilsAsyncTest extends AbstractDataBrokerTest {

    private MdsalUtilsAsync mdsalUtilsAsync;
    private DataBroker databroker;

    private static final TopologyId TOPOLOGY_TEST = new TopologyId("test:1");

    private static final NodeId NODE_ID = new NodeId("test");
    private static final NodeKey NODE_KEY =  new NodeKey(NODE_ID);
    private static final Node DATA = new NodeBuilder().withKey(NODE_KEY).setNodeId(NODE_ID).build();

    private static final InstanceIdentifier<Node> TEST_IID = InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(TOPOLOGY_TEST))
            .child(Node.class, NODE_KEY);

    @Before
    public void setUp() {
        databroker = getDataBroker();
        mdsalUtilsAsync = Mockito.spy(new MdsalUtilsAsync(databroker));
    }

    @Test
    public void testDelete() {
        final FluentFuture<? extends CommitInfo> fut = mdsalUtilsAsync.put(
                LogicalDatastoreType.CONFIGURATION, TEST_IID, DATA);
        fut.addCallback(new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(final CommitInfo result) {
                final FluentFuture<? extends CommitInfo> future =
                        mdsalUtilsAsync.delete(LogicalDatastoreType.CONFIGURATION, TEST_IID);
                future.addCallback(new FutureCallback<CommitInfo>() {

                    @Override
                    public void onSuccess(final CommitInfo result) {
                        assertNull(readDS());
                    }

                    @Override
                    public void onFailure(final Throwable ex) {
                        fail(ex.getMessage());
                    }
                }, MoreExecutors.directExecutor());
            }

            @Override
            public void onFailure(final Throwable ex) {
                fail(ex.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    @Test
    public void testPutWithoutCallback() {
        final String operationDesc = "testPut";
        final SupportingNode supportingNodeBuilder1 = new SupportingNodeBuilder().withKey(
                new SupportingNodeKey(new NodeId("id1"), TOPOLOGY_TEST)).build();
        final SupportingNode supportingNodeBuilder2 = new SupportingNodeBuilder().withKey(
                new SupportingNodeKey(new NodeId("id2"), TOPOLOGY_TEST)).build();

        final Node data1 = new NodeBuilder(DATA).setSupportingNode(
                Collections.singletonMap(supportingNodeBuilder1.key(), supportingNodeBuilder1)).build();
        final Node data2 = new NodeBuilder(DATA).setSupportingNode(
                Collections.singletonMap(supportingNodeBuilder2.key(), supportingNodeBuilder2)).build();

        mdsalUtilsAsync.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, data1, operationDesc);
        assertEquals(data1, readDS());

        final FluentFuture<? extends CommitInfo> future = mdsalUtilsAsync.put(
                LogicalDatastoreType.CONFIGURATION, TEST_IID, data2);
        future.addCallback(new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(final CommitInfo result) {
                assertEquals(1, readDS().getSupportingNode().size());
            }

            @Override
            public void onFailure(final Throwable ex) {
                fail(ex.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    @Test
    public void testMerge() {
        final String operationDesc = "testMerge";
        final SupportingNode supportingNodeBuilder1 = new SupportingNodeBuilder().withKey(
                new SupportingNodeKey(new NodeId("id1"), TOPOLOGY_TEST)).build();
        final SupportingNode supportingNodeBuilder2 = new SupportingNodeBuilder().withKey(
                new SupportingNodeKey(new NodeId("id2"), TOPOLOGY_TEST)).build();

        final Node data1 = new NodeBuilder(DATA).setSupportingNode(
                Collections.singletonMap(supportingNodeBuilder1.key(), supportingNodeBuilder1)).build();
        final Node data2 = new NodeBuilder(DATA).setSupportingNode(
                Collections.singletonMap(supportingNodeBuilder2.key(), supportingNodeBuilder2)).build();

        mdsalUtilsAsync.merge(LogicalDatastoreType.CONFIGURATION, TEST_IID, data1, operationDesc, true);
        assertEquals(data1, readDS());

        final FluentFuture<? extends CommitInfo> future =
                mdsalUtilsAsync.merge(LogicalDatastoreType.CONFIGURATION, TEST_IID, data2, true);
        future.addCallback(new FutureCallback<CommitInfo>() {

            @Override
            public void onSuccess(final CommitInfo result) {
                assertEquals(2, readDS().getSupportingNode().size());
            }

            @Override
            public void onFailure(final Throwable ex) {
                fail(ex.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    @Test
    public void testRead() {
        final FluentFuture<? extends CommitInfo> fut =
                mdsalUtilsAsync.put(LogicalDatastoreType.CONFIGURATION, TEST_IID, DATA);

        fut.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                final FluentFuture<Optional<Node>> future =
                        mdsalUtilsAsync.read(LogicalDatastoreType.CONFIGURATION, TEST_IID);
                Optional<Node> optNode;
                try {
                    optNode = future.get();
                    assertEquals(Optional.of(DATA), optNode);
                } catch (InterruptedException | ExecutionException e) {
                    fail(e.getMessage());
                }
            }

            @Override
            public void onFailure(final Throwable ex) {
                fail(ex.getMessage());
            }
        }, MoreExecutors.directExecutor());
    }

    private Node readDS() {
        try {
            final Optional<Node> result = databroker.newReadOnlyTransaction().read(
                    LogicalDatastoreType.CONFIGURATION, TEST_IID).get();
            if (result.isPresent()) {
                return result.orElseThrow();
            }
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
        return null;
    }
}
