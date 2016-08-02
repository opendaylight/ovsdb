package org.opendaylight.ovsdb.hwvtepsouthbound.reconciliation.configuration;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.yangtools.yang.binding.*;

import java.util.ArrayList;
import java.util.Collection;

public class DataObjectModificationImpl<T extends DataObject> implements DataObjectModification<T> {

    private Collection<DataObjectModification<? extends DataObject>> childNodesCache = new ArrayList<>();
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
        return nodeId.getPathArguments().iterator().next();//TODO
    }

    @Override
    public ModificationType getModificationType() {
        return ModificationType.WRITE;

    }

    @Override
    public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
        return childNodesCache;
    }

    @Override
    public DataObjectModification<? extends DataObject> getModifiedChild(final InstanceIdentifier.PathArgument arg) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Identifiable<K> & ChildOf<? super T>, K extends Identifier<C>> DataObjectModification<C>
        getModifiedChildListItem(final Class<C> listItem, final K listKey) {
        return (DataObjectModification<C>) getModifiedChild(
                new InstanceIdentifier.IdentifiableItem<>(listItem, listKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends ChildOf<? super T>> DataObjectModification<C> getModifiedChildContainer(final Class<C> arg) {
        return (DataObjectModification<C>) getModifiedChild(new InstanceIdentifier.Item<>(arg));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Augmentation<T> & DataObject> DataObjectModification<C> getModifiedAugmentation(
            final Class<C> augmentation) {
        return (DataObjectModification<C>) getModifiedChild(new InstanceIdentifier.Item<>(augmentation));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{identifier = " + nodeId +"}";
    }
}