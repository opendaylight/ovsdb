/*
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Flavio Fernandes
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent;
import org.opendaylight.ovsdb.openstack.netvirt.AbstractHandler;
import org.opendaylight.ovsdb.openstack.netvirt.NodeCacheManagerEvent;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.utils.mdsal.node.NodeUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NodeCacheManagerImpl extends AbstractHandler
        implements NodeCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(NodeCacheManagerImpl.class);
    private List<Node> nodeCache = Lists.newArrayList();


    @Override
    public void nodeAdded(String nodeIdentifier) {
        logger.debug(">>>>> enqueue: Node added : {}", nodeIdentifier);
        enqueueEvent(new NodeCacheManagerEvent(nodeIdentifier, Action.ADD));
    }
    @Override
    public void nodeRemoved(String nodeIdentifier) {
        logger.debug(">>>>> enqueue: Node removed : {}", nodeIdentifier);
        enqueueEvent(new NodeCacheManagerEvent(nodeIdentifier, Action.DELETE));
    }
    @Override
    public List<Node> getNodes() {
        return nodeCache;
    }

    private void _processNodeAdded(Node node) {
        nodeCache.add(node);
    }
    private void _processNodeRemoved(Node node) {
        nodeCache.remove(node);
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
                _processNodeAdded(NodeUtils.getOpenFlowNode(ev.getNodeIdentifier()));
                break;
            case DELETE:
                _processNodeRemoved(NodeUtils.getOpenFlowNode(ev.getNodeIdentifier()));
                break;
            case UPDATE:
                break;
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }
}
