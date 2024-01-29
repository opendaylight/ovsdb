/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import java.util.ArrayList;
import java.util.Collection;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.ExactDataObjectStep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Key;
import org.opendaylight.yangtools.yang.binding.KeyAware;

public class DataObjectModificationImpl<T extends DataObject> implements DataObjectModification<T> {

    private final Collection<DataObjectModification<? extends DataObject>> childNodesCache = new ArrayList<>();
    InstanceIdentifier<T> nodeId;
    T newNode;
    T oldNode;

    public DataObjectModificationImpl(InstanceIdentifier<T> nodeId, T newData, T oldData) {
        this.nodeId = nodeId;
        this.newNode = newData;
        this.oldNode = oldData;
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
    public Class<T> dataType() {
        return (Class<T>) newNode.getClass();
    }

    @Override
    public ExactDataObjectStep<T> step() {
        return (ExactDataObjectStep<T>) nodeId.getPathArguments().iterator().next();
    }

    @Override
    public ModificationType modificationType() {
        return ModificationType.WRITE;

    }

    @Override
    public Collection<DataObjectModification<? extends DataObject>> modifiedChildren() {
        return childNodesCache;
    }

    @Override
    public <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>>
            Collection<DataObjectModification<C>> getModifiedChildren(Class<H> caseType, Class<C> childType) {
        return null;
    }

    @Override
    public <C extends ChildOf<? super T>> Collection<DataObjectModification<C>> getModifiedChildren(
            Class<C> childType) {
        return null;
    }

    @Override
    public DataObjectModification<? extends DataObject> getModifiedChild(final InstanceIdentifier.PathArgument arg) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends KeyAware<K> & ChildOf<? super T>, K extends Key<C>> DataObjectModification<C>
        getModifiedChildListItem(final Class<C> listItem, final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(InstanceIdentifier.IdentifiableItem.of(listItem, listKey));
    }

    @Override
    public <H extends ChoiceIn<? super T> & DataObject, C extends KeyAware<K> & ChildOf<? super H>,
            K extends Key<C>> DataObjectModification<C> getModifiedChildListItem(Class<H> caseType,
                    Class<C> listItem, K listKey) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(final Class<C> arg) {
        return (DataObjectModification<C>) getModifiedChild(InstanceIdentifier.Item.of(arg));
    }

    @Override
    public <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>> DataObjectModification<C>
            getModifiedChildContainer(Class<H> caseType, Class<C> child) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Augmentation<T> & DataObject> DataObjectModification<C> getModifiedAugmentation(
            final Class<C> augmentation) {
        return (DataObjectModification<C>) getModifiedChild(InstanceIdentifier.Item.of(augmentation));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{identifier = " + nodeId + "}";
    }
}
