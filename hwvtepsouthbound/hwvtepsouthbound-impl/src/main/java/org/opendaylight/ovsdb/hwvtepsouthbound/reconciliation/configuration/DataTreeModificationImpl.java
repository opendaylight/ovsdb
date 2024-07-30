/*
 * Copyright © 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataTreeModificationImpl<T extends DataObject> implements DataTreeModification<T> {
    InstanceIdentifier<T> nodeId;
    T newNode;
    T oldNode;

    public DataTreeModificationImpl(InstanceIdentifier<T> nodeId, T newNode, T oldNode) {
        this.nodeId = nodeId;
        this.newNode = newNode;
        this.oldNode = oldNode;
    }

    @Override
    public LogicalDatastoreType datastore() {
        return LogicalDatastoreType.CONFIGURATION;
    }

    @Override
    public DataObjectIdentifier<T> path() {
        return nodeId.toIdentifier();
    }

    @Override
    public DataObjectModification<T> getRootNode() {
        return new DataObjectModificationImpl<>(nodeId, newNode, oldNode);
    }
}