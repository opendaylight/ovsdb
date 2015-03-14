/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Sam Hague
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.utils.mdsal.node.NodeUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableNodeDataChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FlowCapableNodeDataChangeListener.class);
    private ListenerRegistration<DataChangeListener> registration;
    private List<Node> nodeCache = Lists.newArrayList();
    private NetworkingProviderManager networkingProviderManager = null;
    private PipelineOrchestrator pipelineOrchestrator = null;
    private NodeCacheManager nodeCacheManager = null;

    public static final InstanceIdentifier<FlowCapableNode> createFlowCapableNodePath () {
        return InstanceIdentifier.builder(Nodes.class)
                .child(Node.class)
                .augmentation(FlowCapableNode.class)
                .build();
    }

    public FlowCapableNodeDataChangeListener (DataBroker dataBroker) {
        LOG.info("Registering FlowCapableNodeChangeListener");
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                createFlowCapableNodePath(), this, AsyncDataBroker.DataChangeScope.ONE);
    }

    @Override
    public void close () throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged (AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        checkMemberInitialization();

        LOG.debug(">>>> onDataChanged: {}", changes);

        for (InstanceIdentifier instanceIdentifier : changes.getRemovedPaths()) {
            DataObject originalDataObject = changes.getOriginalData().get(instanceIdentifier);
            LOG.info(">>>>> removed iiD: {} - dataObject {}", instanceIdentifier, originalDataObject);
            if (originalDataObject != null && originalDataObject instanceof Node){
                Node node = (Node) originalDataObject;
                notifyNodeRemoved(NodeUtils.getOpenFlowNode(node.getId().getValue()));
            }
        }

        for (Map.Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            InstanceIdentifier<?> iID = created.getKey();
            String openflowId = iID.firstKeyOf(Node.class, NodeKey.class).getId().getValue();
            LOG.debug(">>>>> created iiD: {} - first: {} - NodeKey: {}",
                    iID, iID.firstIdentifierOf(Node.class), openflowId);
            Node openFlowNode = NodeUtils.getOpenFlowNode(openflowId);
            if (nodeCache.contains(openFlowNode)) {
                notifyNodeUpdated(openFlowNode);
            } else {
                notifyNodeCreated(openFlowNode);
            }
        }

        for (Map.Entry<InstanceIdentifier<?>, DataObject> updated : changes.getUpdatedData().entrySet()) {
            InstanceIdentifier<?> iID = updated.getKey();
            String openflowId = iID.firstKeyOf(Node.class, NodeKey.class).getId().getValue();
            LOG.debug(">>>>> updated iiD: {} - first: {} - NodeKey: {}",
                    iID, iID.firstIdentifierOf(Node.class), openflowId);
            Node openFlowNode = NodeUtils.getOpenFlowNode(openflowId);
            if (nodeCache.contains(openFlowNode)) {
                notifyNodeUpdated(openFlowNode);
            } else {
                notifyNodeCreated(openFlowNode);
            }
        }
    }

    private void notifyNodeUpdated (Node openFlowNode) {
        final String openflowId = openFlowNode.getId().getValue();
        LOG.info("notifyNodeUpdated: Node {} from Controller's inventory Service", openflowId);

        // TODO: will do something amazing here, someday
    }

    private void notifyNodeCreated (Node openFlowNode) {
        final String openflowId = openFlowNode.getId().getValue();
        LOG.info("notifyNodeCreated: Node {} from Controller's inventory Service", openflowId);

        nodeCache.add(openFlowNode);

        if (networkingProviderManager != null) {
            networkingProviderManager.getProvider(openFlowNode).initializeOFFlowRules(openFlowNode);
        }
        if (pipelineOrchestrator != null) {
            pipelineOrchestrator.enqueue(openflowId);
        }
        if (nodeCacheManager != null) {
            nodeCacheManager.nodeAdded(openflowId);
        }
    }

    private void notifyNodeRemoved (Node openFlowNode) {
        LOG.info("notifyNodeRemoved: Node {} from Controller's inventory Service",
                openFlowNode.getId().getValue());

        nodeCache.remove(openFlowNode);

        if (nodeCacheManager != null) {
            nodeCacheManager.nodeRemoved(openFlowNode.getId().getValue());
        }
    }

    private void checkMemberInitialization() {
        /**
         * Obtain local ref to members, if needed. Having these local saves us from calling getGlobalInstance
         * upon every event.
         */
        if (networkingProviderManager == null) {
            networkingProviderManager =
                    (NetworkingProviderManager) ServiceHelper.getGlobalInstance(NetworkingProviderManager.class, this);
        }
        if (pipelineOrchestrator == null) {
            pipelineOrchestrator =
                    (PipelineOrchestrator) ServiceHelper.getGlobalInstance(PipelineOrchestrator.class, this);
        }
        if (nodeCacheManager == null) {
            nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        }
    }
}
