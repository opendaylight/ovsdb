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
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.ChoiceIn;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataObjectModificationImpl<T extends DataObject> implements DataObjectModification<T> {

    private final Collection<DataObjectModification<T>> childNodesCache = new ArrayList<>();
    InstanceIdentifier<T> nodeId;
    T newNode;
    T oldNode;

    public DataObjectModificationImpl(InstanceIdentifier<T> nodeId, T newData, T oldData) {
        this.nodeId = nodeId;
        this.newNode = newData;
        this.oldNode = oldData;
    }


    @Override
    public T getDataBefore() {
        return oldNode;
    }

    @Override
    public T getDataAfter() {
        return newNode;
    }

    @Override
    public Class<T> getDataType() {
        return (Class<T>) newNode.getClass();
    }

    @Override
    public InstanceIdentifier.PathArgument getIdentifier() {
        return nodeId.getPathArguments().iterator().next();
    }

    @Override
    public ModificationType getModificationType() {
        return ModificationType.WRITE;

    }

    @Override
    public Collection<DataObjectModification<T>> getModifiedChildren() {
        return childNodesCache;
    }

    @Override
    public <C extends ChildOf<? super T>> Collection<DataObjectModification<C>> getModifiedChildren(
            @NonNull Class<C> childType) {
        return childNodesCache.stream().filter(modification -> childType.equals(modification.getDataType())).map(
            modification -> (DataObjectModification<C>) modification).collect(Collectors.toList());
    }

    @Override
    public <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>>
        Collection<DataObjectModification<C>> getModifiedChildren(
            @NonNull Class<H> caseType, @NonNull Class<C> childType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataObjectModification<? extends DataObject> getModifiedChild(final InstanceIdentifier.PathArgument arg) {
        return null;
    }

    @Override
    public @Nullable <H extends ChoiceIn<? super T> & DataObject, C extends Identifiable<K> & ChildOf<? super H>,
            K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                @NonNull Class<H> caseType, @NonNull Class<C> listItem, @NonNull K listKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<C>> DataObjectModification<C>
        getModifiedChildListItem(final Class<C> listItem, final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(InstanceIdentifier.IdentifiableItem.of(listItem, listKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(final Class<C> arg) {
        return (DataObjectModification<C>) getModifiedChild(InstanceIdentifier.Item.of(arg));
    }

    @Override
    public @Nullable <H extends ChoiceIn<? super T> & DataObject, C extends ChildOf<? super H>>
    DataObjectModification<C> getModifiedChildContainer(@NonNull Class<H> caseType, @NonNull Class<C> child) {
        throw new UnsupportedOperationException();
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
