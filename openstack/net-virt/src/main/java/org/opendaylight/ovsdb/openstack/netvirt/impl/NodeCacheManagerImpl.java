/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.NodeCacheManagerEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Flavio Fernandes (ffernand@redhat.com)
 * @author Sam Hague (shague@redhat.com)
 */
public class NodeCacheManagerImpl extends AbstractHandler implements NodeCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeCacheManagerImpl.class);
    private final Object nodeCacheLock = new Object();
    private List<Node> nodeCache = Lists.newArrayList();
    private Map<Long, NodeCacheListener> handlers = Maps.newHashMap();

    void init() {
        logger.info(">>>>> init {}", this.getClass());
    }

    @Override
    public void nodeAdded(Node node) {
        logger.debug("nodeAdded: Node added: {}", node);
        if (addNodeToCache(node)) {
            enqueueEvent(new NodeCacheManagerEvent(node, Action.ADD));
        } else {
            enqueueEvent(new NodeCacheManagerEvent(node, Action.UPDATE));
        }

    }

    @Override
    public void nodeRemoved(Node node) {
        logger.debug("nodeRemoved: Node removed: {}", node);
        if (removeNodeFromCache(node)) {
            enqueueEvent(new NodeCacheManagerEvent(node, Action.DELETE));
        }
    }

    @Override
    public List<Node> getNodes() {
        return nodeCache;
    }

    /**
     * This method returns the true if node was added to the nodeCache. If param node
     * is already in the cache, this method is expected to return false.
     *
     * @param openFlowNode the node to be added to the cache, if needed
     * @return whether new node entry was added to cache
     */
    private Boolean addNodeToCache (Node openFlowNode) {
        synchronized (nodeCacheLock) {
            if (nodeCache.contains(openFlowNode)) {
                return false;
            }
            return nodeCache.add(openFlowNode);
        }
    }

    /**
     * This method returns the true if node was removed from the nodeCache. If param node
     * is not in the cache, this method is expected to return false.
     *
     * @param openFlowNode the node to be removed from the cache, if needed
     * @return whether new node entry was removed from cache
     */
    private Boolean removeNodeFromCache (Node openFlowNode) {
        synchronized (nodeCacheLock) {
            return nodeCache.remove(openFlowNode);
        }
    }

    private void processNodeAdded(Node node) {
        nodeCache.add(node);
        for (NodeCacheListener handler : handlers.values()) {
            try {
                handler.notifyNode(node, Action.ADD);
            } catch (Exception e) {
                logger.error("Failed notifying node add event", e);
            }
        }
    }
    private void processNodeRemoved(Node node) {
        nodeCache.remove(node);
        for (NodeCacheListener handler : handlers.values()) {
            try {
                handler.notifyNode(node, Action.DELETE);
            } catch (Exception e) {
                logger.error("Failed notifying node remove event", e);
            }
        }
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
        logger.debug(">>>>> dequeue: {}", ev);
        switch (ev.getAction()) {
            case ADD:
                processNodeAdded(ev.getNode());
                break;
            case DELETE:
                processNodeRemoved(ev.getNode());
                break;
            case UPDATE:
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
}
