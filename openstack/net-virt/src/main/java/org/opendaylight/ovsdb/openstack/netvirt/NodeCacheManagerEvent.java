/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @author Flavio Fernandes (ffernand@redhat.com)
 * @author Sam Hague (shague@redhat.com)
 */
public class NodeCacheManagerEvent extends AbstractEvent {
    private Node node;

    public NodeCacheManagerEvent(Node node, Action action) {
        super(HandlerType.NODE, action);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public String getNodeIdentifier() {
        return node.getNodeId().getValue();
    }

    @Override
    public String toString() {
        return "NodeCacheManagerEvent [action=" + super.getAction()
               + ", node=" + node
               + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        NodeCacheManagerEvent other = (NodeCacheManagerEvent) obj;
        if (node == null) {
            if (other.node != null) {
                return false;
            }
        } else if (!node.equals(other.node)) {
            return false;
        }
        return true;
    }
}
