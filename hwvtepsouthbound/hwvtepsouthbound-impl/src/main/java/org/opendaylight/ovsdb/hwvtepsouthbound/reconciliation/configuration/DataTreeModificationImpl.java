package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.*;

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