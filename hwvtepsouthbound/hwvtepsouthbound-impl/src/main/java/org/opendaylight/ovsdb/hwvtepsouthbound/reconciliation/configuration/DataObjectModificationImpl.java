/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectWritten;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.ExactDataObjectStep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataObjectModificationImpl<T extends DataObject> extends DataObjectWritten<T> {
    private final Collection<DataObjectModification<? extends DataObject>> childNodesCache = new ArrayList<>();
    InstanceIdentifier<T> nodeId;
    T newNode;
    T oldNode;

    public DataObjectModificationImpl(final InstanceIdentifier<T> nodeId, final T newData, final T oldData) {
        this.nodeId = nodeId;
        newNode = newData;
        oldNode = oldData;
    }

    @Override
    public T dataBefore() {
        return oldNode;
    }

    @Override
    public T dataAfter() {
        return newNode;
    }

    @Override
    public ExactDataObjectStep<T> step() {
        return (ExactDataObjectStep<T>) nodeId.lastStep();
    }

    @Override
    public <C extends DataObject> @Nullable DataObjectModification<C> modifiedChild(final ExactDataObjectStep<C> step) {
        return null;
    }

    @Override
    public Collection<DataObjectModification<? extends DataObject>> modifiedChildren() {
        return childNodesCache;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("identifier", nodeId);
    }
}
