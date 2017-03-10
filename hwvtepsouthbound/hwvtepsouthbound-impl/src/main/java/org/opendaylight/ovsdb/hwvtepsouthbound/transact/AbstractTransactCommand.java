/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTransactCommand<T extends Identifiable, Aug extends Augmentation<Node>> implements TransactCommand<T> {

    private HwvtepOperationalState operationalState;
    private Collection<DataTreeModification<Node>> changes;

    protected AbstractTransactCommand() {
        // NO OP
    }

    public AbstractTransactCommand(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        this.operationalState = state;
        this.changes = changes;
    }

    public HwvtepOperationalState getOperationalState() {
        return operationalState;
    }

    public Collection<DataTreeModification<Node>> getChanges() {
        return changes;
    }

    void updateCurrentTxDeleteData(Class<? extends Identifiable> cls, InstanceIdentifier key, T data) {
        operationalState.updateCurrentTxDeleteData(cls, key);
        operationalState.getDeviceInfo().clearConfigData(cls, key);
    }

    void updateCurrentTxData(Class<? extends Identifiable> cls, InstanceIdentifier key, UUID uuid, Object data) {
        operationalState.updateCurrentTxData(cls, key, uuid);
        operationalState.getDeviceInfo().markKeyAsInTransit(cls, key);
        operationalState.getDeviceInfo().updateConfigData(cls, key, data);
    }

    void processDependencies(UnMetDependencyGetter<T> unMetDependencyGetter,
                             TransactionBuilder transaction,
                             final InstanceIdentifier<Node> nodeIid,
                             final InstanceIdentifier key,
                             final T data, final Object... extraData) {

        HwvtepDeviceInfo deviceInfo = operationalState.getDeviceInfo();
        Map inTransitDependencies = unMetDependencyGetter.getInTransitDependencies(operationalState, data);
        Map confingDependencies = unMetDependencyGetter.getUnMetConfigDependencies(operationalState, data);
        //we can skip the config termination point dependency as we can create them in device as part of this tx
        confingDependencies.remove(TerminationPoint.class);

        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType)type).getActualTypeArguments()[0];

        //If this key itself is in transit wait for the response of this key itself
        if (deviceInfo.isKeyInTransit((Class<? extends Identifiable>) classType, key)) {
            inTransitDependencies.put(classType, Collections.singletonList(key));
        }

        if (HwvtepSouthboundUtil.isEmptyMap(confingDependencies) && HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {
            doDeviceTransaction(transaction, nodeIid, data, key, extraData);
            //TODO put proper uuid
            updateCurrentTxData((Class<? extends Identifiable>) classType, key, new UUID("uuid"), data);
        }
        if (!HwvtepSouthboundUtil.isEmptyMap(confingDependencies)) {
            DependentJob<T> configWaitingJob = new DependentJob.ConfigWaitingJob(
                    key, data, confingDependencies) {

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder) {
                    AbstractTransactCommand.this.operationalState = operationalState;
                    onConfigUpdate(transactionBuilder, nodeIid, data, key, extraData);
                }
            };
            deviceInfo.addJobToQueue(configWaitingJob);
        }
        if (!HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {

            DependentJob<T> opWaitingJob = new DependentJob.OpWaitingJob(
                    key, data, inTransitDependencies) {

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder) {
                    AbstractTransactCommand.this.operationalState = operationalState;
                    onConfigUpdate(transactionBuilder, nodeIid, data, key, extraData);
                }
            };
            deviceInfo.addJobToQueue(opWaitingJob);
        }
    }

    public void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data,
                                    InstanceIdentifier key, Object... extraData) {
        //tobe removed as part of refactoring patch
    }

    public void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data,
                               InstanceIdentifier key, Object... extraData) {
        //tobe removed as part of refactoring patch
    }

    protected Aug getAugmentation(Node node) {
        if (node == null) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        Class<? extends Augmentation<Node>> augType = (Class<? extends Augmentation<Node>>) parameterizedType.getActualTypeArguments()[1];
        Augmentation<Node> augmentation = node.getAugmentation(augType);
        return (Aug)augmentation;
    }

    protected List<T> getData(Aug augmentation) {
        return Collections.EMPTY_LIST;
    }

    protected List<T> getData(Node node) {
        Aug augmentation = getAugmentation(node);
        if (augmentation != null) {
            List<T> data = getData(augmentation);
            if (data != null) {
                return new ArrayList<>(data);
            }
        }
        return Collections.emptyList();
    }

    protected Map<InstanceIdentifier<Node>, List<T>> extractRemoved(
            Collection<DataTreeModification<Node>> changes, Class<T> class1) {
        Map<InstanceIdentifier<Node>, List<T>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                Class<? extends Identifiable> classType = (Class<? extends Identifiable>) getClassType();
                List<T> removed;
                if (operationalState.isInReconciliation()) {
                    removed = getRemoved(change);
                } else {
                    removed = (List<T>) operationalState.getDeletedData(key, classType);
                }
                removed.addAll(getCascadeDeleteData(change));
                result.put(key, removed);
            }
        }
        return result;
    }

    protected Map<InstanceIdentifier<Node>, List<T>> extractUpdated(
            Collection<DataTreeModification<Node>> changes, Class<T> class1) {
        Map<InstanceIdentifier<Node>, List<T>> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                Class<? extends Identifiable> classType = (Class<? extends Identifiable>) getClassType();
                List<T> updated = null;
                if (operationalState.isInReconciliation()) {
                    updated = getUpdated(change);
                } else {
                    updated = (List<T>) operationalState.getUpdatedData(key, classType);
                }
                result.put(key, updated);
            }
        }
        return result;
    }

    List<T>  getCascadeDeleteData(DataTreeModification<Node> change) {
        if (!cascadeDelete()) {
            return Collections.EMPTY_LIST;
        }
        DataObjectModification<Node> mod = change.getRootNode();
        Node updatedNode = TransactUtils.getUpdated(mod);
        List<T> updatedData = getData(updatedNode);
        Set<InstanceIdentifier> deleted = getOperationalState().getDeletedKeysInCurrentTx(LogicalSwitches.class);
        UnMetDependencyGetter dependencyGetter = getDependencyGetter();
        if (!HwvtepSouthboundUtil.isEmpty(deleted) && !HwvtepSouthboundUtil.isEmpty(updatedData) && dependencyGetter != null) {
            List<T> removed = new ArrayList<>();
            for (T ele : updatedData) {
                if (deleted.containsAll(dependencyGetter.getLogicalSwitchDependencies(ele))) {
                    removed.add(ele);
                }
            }
            return removed;
        }
        return Collections.EMPTY_LIST;
    }

    List<T> getRemoved(DataTreeModification<Node> change) {
        DataObjectModification<Node> mod = change.getRootNode();

        Node removed = TransactUtils.getRemoved(mod);
        Node updated = TransactUtils.getUpdated(mod);
        Node before = mod.getDataBefore();
        return diffOf(removed, before, updated, true);
    }

    List<T> getUpdated(DataTreeModification<Node> change) {
        DataObjectModification<Node> mod = change.getRootNode();
        Node updated = TransactUtils.getUpdated(mod);
        Node before = mod.getDataBefore();
        return diffOf(updated, before, false);
    }

    List<T> diffOf(Node include, Node a, Node b, boolean compareKeyOnly) {
        List<T> data1 = getData(include);
        List<T> data2 = diffOf(a, b, compareKeyOnly);
        if (HwvtepSouthboundUtil.isEmpty(data1) && HwvtepSouthboundUtil.isEmpty(data2)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(data1);
        result.addAll(data2);
        return result;
    }

    List<T> diffOf(Node a, Node b, boolean compareKeyOnly) {
        List<T> result = new ArrayList<>();

        List<T> list1 = getData(a);
        List<T> list2 = getData(b);

        if (HwvtepSouthboundUtil.isEmpty(list1)) {
            return Collections.emptyList();
        }
        if (HwvtepSouthboundUtil.isEmpty(list2)) {
            return HwvtepSouthboundUtil.isEmpty(list1) ? Collections.emptyList() : list1;
        }

        Iterator<T> it1 = list1.iterator();

        while(it1.hasNext()) {
            T ele = it1.next();
            Iterator<T> it2 = list2.iterator();
            boolean found = false;
            while (it2.hasNext()) {
                T other  = it2.next();
                found = compareKeyOnly ? Objects.equals(ele.getKey(), other.getKey()) : areEqual(ele, other);
                if ( found ) {
                    it2.remove();
                    break;
                }
            }
            if (!found) {
                result.add(ele);
            }
        }
        return result;
    }


    protected Type getClassType() {
        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType)type).getActualTypeArguments()[0];
        return classType;
    }

    protected boolean areEqual(T a , T b) {
        return a.getKey().equals(b.getKey());
    }

    protected UnMetDependencyGetter getDependencyGetter() {
        return null;
    }

    /**
     * Tells if this object needs to be deleted if its dependent object gets deleted
     * Ex : LocalUcastMac and LocalMacstMac
     * @return true if this object needs to be deleted if its dependent object gets deleted
     */
    protected boolean cascadeDelete() {
        return false;
    }
}
