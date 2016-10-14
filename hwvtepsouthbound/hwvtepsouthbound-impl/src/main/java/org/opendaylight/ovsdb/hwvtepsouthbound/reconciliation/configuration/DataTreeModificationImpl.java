/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataTreeModificationImpl<T extends DataObject> implements DataTreeModification {

    InstanceIdentifier<T> nodeId;
    T newNode;
    T oldNode;

    public DataTreeModificationImpl(InstanceIdentifier<T> nodeId, T newNode, T oldNode) {
        this.nodeId = nodeId;
        this.newNode = newNode;
        this.oldNode = oldNode;
    }

    @Override
    public DataTreeIdentifier<T> getRootPath() {
        return new DataTreeIdentifier<T>(LogicalDatastoreType.CONFIGURATION, nodeId);
    }

    @Override
    public DataObjectModification<T> getRootNode() {
        return new DataObjectModificationImpl<T>(nodeId, newNode, oldNode);
    }
}