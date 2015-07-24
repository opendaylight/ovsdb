/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.NodeCacheManagerEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Flavio Fernandes (ffernand@redhat.com)
 * @author Sam Hague (shague@redhat.com)
 */
public class NodeCacheManagerImpl extends AbstractHandler implements NodeCacheManager, ConfigInterface {
    private static final Logger logger = LoggerFactory.getLogger(NodeCacheManagerImpl.class);
    private final Object nodeCacheLock = new Object();
    private Map<NodeId, Node> nodeCache = new ConcurrentHashMap<>();
    private Map<Long, NodeCacheListener> handlers = Maps.newHashMap();
    private volatile Southbound southbound;

    @Override
    public void nodeAdded(Node node) {
        logger.debug("nodeAdded: {}", node);
        enqueueEvent(new NodeCacheManagerEvent(node, Action.UPDATE));
    }

    @Override
    public void nodeRemoved(Node node) {
        logger.debug("nodeRemoved: {}", node);
        enqueueEvent(new NodeCacheManagerEvent(node, Action.DELETE));
    }

    // TODO SB_MIGRATION
    // might need to break this into two different events
    // notifyOvsdbNode, notifyBridgeNode or just make sure the
    // classes implementing the interface check for ovsdbNode or bridgeNode
    private void processNodeUpdate(Node node) {
        Action action = Action.UPDATE;

        NodeId nodeId = node.getNodeId();
        if (nodeCache.get(nodeId) == null) {
            action = Action.ADD;
        }
        nodeCache.put(nodeId, node);

        logger.debug("processNodeUpdate: {} Node type {} {}: {}",
                nodeCache.size(),
                southbound.getBridge(node) != null ? "BridgeNode" : "OvsdbNode",
                action == Action.ADD ? "ADD" : "UPDATE",
                node);

        for (NodeCacheListener handler : handlers.values()) {
            try {
                handler.notifyNode(node, action);
            } catch (Exception e) {
                logger.error("Failed notifying node add event", e);
            }
        }
        logger.debug("processNodeUpdate returns");
    }

    private void processNodeRemoved(Node node) {
        nodeCache.remove(node.getNodeId());
        for (NodeCacheListener handler : handlers.values()) {
            try {
                handler.notifyNode(node, Action.DELETE);
            } catch (Exception e) {
                logger.error("Failed notifying node remove event", e);
            }
        }
        logger.warn("processNodeRemoved returns");
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NodeCacheManagerEvent)) {
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        NodeCacheManagerEvent ev = (NodeCacheManagerEvent) abstractEvent;
        logger.debug("NodeCacheManagerImpl: dequeue: {}", ev);
        switch (ev.getAction()) {
            case DELETE:
                processNodeRemoved(ev.getNode());
                break;
            case UPDATE:
                processNodeUpdate(ev.getNode());
                break;
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }

    public void cacheListenerAdded(final ServiceReference ref, NodeCacheListener handler){
        Long pid = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        handlers.put(pid, handler);
        logger.info("Node cache listener registered, pid {} {}", pid, handler.getClass().getName());
    }

    public void cacheListenerRemoved(final ServiceReference ref){
        Long pid = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        handlers.remove(pid);
        logger.debug("Node cache listener unregistered, pid {}", pid);
    }

    @Override
    public Map<NodeId,Node> getOvsdbNodes() {
        Map<NodeId,Node> ovsdbNodesMap = new ConcurrentHashMap<>();
        for (Map.Entry<NodeId, Node> ovsdbNodeEntry : nodeCache.entrySet()) {
            if (southbound.extractOvsdbNode(ovsdbNodeEntry.getValue()) != null) {
                ovsdbNodesMap.put(ovsdbNodeEntry.getKey(), ovsdbNodeEntry.getValue());
            }
        }
        return ovsdbNodesMap;
    }

    @Override
    public List<Node> getBridgeNodes() {
        List<Node> nodes = Lists.newArrayList();
        for (Node node : nodeCache.values()) {
            if (southbound.getBridge(node) != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public List<Node> getNodes() {
        List<Node> nodes = Lists.newArrayList();
        for (Node node : nodeCache.values()) {
            nodes.add(node);
        }
        return nodes;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(NodeCacheManager.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
