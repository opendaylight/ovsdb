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
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransactCommand<T extends Identifiable, A extends Augmentation<Node>>
        implements TransactCommand<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactCommand.class);
    protected static final UUID TXUUID = new UUID("TXUUID");
    protected ThreadLocal<HwvtepOperationalState> threadLocalOperationalState = new ThreadLocal<>();
    protected ThreadLocal<TransactionBuilder> threadLocalDeviceTransaction = new ThreadLocal<>();
    private Collection<DataTreeModification<Node>> changes;
    protected Map<TransactionBuilder, List<MdsalUpdate<T>>> updates = new ConcurrentHashMap<>();

    protected AbstractTransactCommand() {
        // NO OP
    }

    public AbstractTransactCommand(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        this.threadLocalOperationalState.set(state);
        this.changes = changes;
    }

    public HwvtepOperationalState getOperationalState() {
        return threadLocalOperationalState.get();
    }

    public DataBroker getDataBroker() {
        return getOperationalState().getDataBroker();
    }

    public Collection<DataTreeModification<Node>> getChanges() {
        return changes;
    }

    void updateCurrentTxDeleteData(Class<? extends Identifiable> cls, InstanceIdentifier key, T data) {
        getOperationalState().getDeviceInfo().markKeyAsInTransit(cls, key);
        addToUpdates(key, data);
        getOperationalState().getDeviceInfo().clearConfigData(cls, key);
    }

    void updateCurrentTxData(Class<? extends Identifiable> cls, InstanceIdentifier key, UUID uuid, T data) {
        getOperationalState().getDeviceInfo().markKeyAsInTransit(cls, key);
        addToUpdates(key, data);
        getOperationalState().getDeviceInfo().updateConfigData(cls, key, data);
    }

    void addToUpdates(InstanceIdentifier key, T data) {
        T oldData = null;
        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (getDeviceInfo().getConfigData((Class<? extends Identifiable>) classType, key) != null) {
            oldData = (T) getDeviceInfo().getConfigData((Class<? extends Identifiable>) classType, key);
        }
        updates.putIfAbsent(getDeviceTransaction(), new ArrayList<MdsalUpdate<T>>());
        updates.get(getDeviceTransaction()).add(new MdsalUpdate<>(key, data, oldData));
    }

    void processDependencies(final UnMetDependencyGetter<T> unMetDependencyGetter,
            final TransactionBuilder transaction,
            final InstanceIdentifier<Node> nodeIid,
            final InstanceIdentifier key,
            final T data, final Object... extraData) {

        this.threadLocalDeviceTransaction.set(transaction);
        HwvtepDeviceInfo deviceInfo = getOperationalState().getDeviceInfo();
        Map inTransitDependencies = new HashMap<>();
        Map configDependencies = new HashMap<>();

        if (!isRemoveCommand() && unMetDependencyGetter != null) {
            inTransitDependencies = unMetDependencyGetter.getInTransitDependencies(getOperationalState(), data);
            configDependencies = unMetDependencyGetter.getUnMetConfigDependencies(getOperationalState(), data);
            //we can skip the config termination point dependency as we can create them in device as part of this tx
            configDependencies.remove(TerminationPoint.class);
        }

        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType) type).getActualTypeArguments()[0];

        //If this key itself is in transit wait for the response of this key itself
        if (deviceInfo.isKeyInTransit((Class<? extends Identifiable>) classType, key)) {
            inTransitDependencies.put(classType, Collections.singletonList(key));
        }

        if (HwvtepSouthboundUtil.isEmptyMap(configDependencies) && HwvtepSouthboundUtil.isEmptyMap(
                inTransitDependencies)) {
            doDeviceTransaction(transaction, nodeIid, data, key, extraData);
            if (isRemoveCommand()) {
                getDeviceInfo().clearConfigData((Class<? extends Identifiable>) classType, key);
            } else {
                getDeviceInfo().updateConfigData((Class<? extends Identifiable>) classType, key, data);
            }
        }
        if (!HwvtepSouthboundUtil.isEmptyMap(configDependencies)) {
            DependentJob<T> configWaitingJob = new DependentJob.ConfigWaitingJob<T>(
                    key, data, configDependencies) {

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                        TransactionBuilder transactionBuilder) {
                    AbstractTransactCommand.this.threadLocalOperationalState.set(operationalState);
                    AbstractTransactCommand.this.threadLocalDeviceTransaction.set(transactionBuilder);
                    onConfigUpdate(transactionBuilder, nodeIid, data, key, extraData);
                }
            };
            deviceInfo.addJobToQueue(configWaitingJob);
        }

        if (!HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {

            DependentJob<T> opWaitingJob = new DependentJob.OpWaitingJob<T>(
                    key, data, inTransitDependencies) {

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                        TransactionBuilder transactionBuilder) {
                    //data would have got deleted by , push the data only if it is still in configds
                    threadLocalOperationalState.set(operationalState);
                    threadLocalDeviceTransaction.set(transactionBuilder);
                    T data = (T) new MdsalUtils(operationalState.getDataBroker()).read(
                            LogicalDatastoreType.CONFIGURATION, key);
                    if (data != null) {
                        onConfigUpdate(transactionBuilder, nodeIid, data, key, extraData);
                    } else {
                        LOG.warn("Skipping add of key: {} as it is not present txId: {}", key);
                    }
                }
            };
            deviceInfo.addJobToQueue(opWaitingJob);
        }
    }

    @Override
    public void doDeviceTransaction(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data,
            InstanceIdentifier key, Object... extraData) {
        //tobe removed as part of refactoring patch
    }

    @Override
    public void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data,
            InstanceIdentifier key, Object... extraData) {
        //tobe removed as part of refactoring patch
    }

    protected A augmentation(Node node) {
        if (node == null) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        Class<? extends Augmentation<Node>> augType =
                (Class<? extends Augmentation<Node>>) parameterizedType.getActualTypeArguments()[1];
        Augmentation<Node> augmentation = node.augmentation(augType);
        return (A) augmentation;
    }

    protected List<T> getData(A augmentation) {
        return Collections.emptyList();
    }

    protected List<T> getData(Node node) {
        A augmentation = augmentation(node);
        if (augmentation != null) {
            List<T> data = getData(augmentation);
            if (data != null) {
                return new ArrayList<>(data);
            }
        }
        return Collections.emptyList();
    }

    @Nonnull
    protected Map<InstanceIdentifier<Node>, List<T>> extractRemoved(
            Collection<DataTreeModification<Node>> modification, Class<T> class1) {
        Map<InstanceIdentifier<Node>, List<T>> result = new HashMap<>();
        if (modification != null && !modification.isEmpty()) {
            for (DataTreeModification<Node> change : modification) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                Class<? extends Identifiable> classType = (Class<? extends Identifiable>) getClassType();
                List<T> removed;
                if (getOperationalState().isInReconciliation()) {
                    removed = getRemoved(change);
                } else {
                    removed = (List<T>) getOperationalState().getDeletedData(key, classType);
                }
                removed.addAll(getCascadeDeleteData(change));
                result.put(key, removed);
            }
        }
        return result;
    }

    @Nonnull
    protected Map<InstanceIdentifier<Node>, List<T>> extractUpdated(
            Collection<DataTreeModification<Node>> modification, Class<T> class1) {
        Map<InstanceIdentifier<Node>, List<T>> result = new HashMap<>();
        if (modification != null && !modification.isEmpty()) {
            for (DataTreeModification<Node> change : modification) {
                InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                Class<? extends Identifiable> classType = (Class<? extends Identifiable>) getClassType();
                List<T> updated = null;
                if (getOperationalState().isInReconciliation()) {
                    updated = getUpdated(change);
                } else {
                    updated = (List<T>) getOperationalState().getUpdatedData(key, classType);
                }
                result.put(key, updated);
            }
        }
        return result;
    }

    List<T> getCascadeDeleteData(DataTreeModification<Node> change) {
        if (!cascadeDelete()) {
            return Collections.emptyList();
        }
        DataObjectModification<Node> mod = change.getRootNode();
        Node updatedNode = TransactUtils.getUpdated(mod);
        List<T> updatedData = getData(updatedNode);
        Set<InstanceIdentifier> deleted = getOperationalState().getDeletedKeysInCurrentTx(LogicalSwitches.class);
        UnMetDependencyGetter dependencyGetter = getDependencyGetter();
        if (!HwvtepSouthboundUtil.isEmpty(deleted) && !HwvtepSouthboundUtil.isEmpty(updatedData)
                && dependencyGetter != null) {
            List<T> removed = new ArrayList<>();
            for (T ele : updatedData) {
                if (deleted.containsAll(dependencyGetter.getLogicalSwitchDependencies(ele))) {
                    removed.add(ele);
                }
            }
            return removed;
        }
        return Collections.emptyList();
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

    List<T> diffOf(Node include, Node node1, Node node2, boolean compareKeyOnly) {
        List<T> data1 = getData(include);
        List<T> data2 = diffOf(node1, node2, compareKeyOnly);
        if (HwvtepSouthboundUtil.isEmpty(data1) && HwvtepSouthboundUtil.isEmpty(data2)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(data1);
        result.addAll(data2);
        return result;
    }

    List<T> diffOf(Node node1, Node node2, boolean compareKeyOnly) {
        List<T> result = new ArrayList<>();

        List<T> list1 = getData(node1);
        List<T> list2 = getData(node2);

        if (HwvtepSouthboundUtil.isEmpty(list1)) {
            return Collections.emptyList();
        }
        if (HwvtepSouthboundUtil.isEmpty(list2)) {
            return HwvtepSouthboundUtil.isEmpty(list1) ? Collections.emptyList() : list1;
        }

        Iterator<T> it1 = list1.iterator();

        while (it1.hasNext()) {
            T ele = it1.next();
            Iterator<T> it2 = list2.iterator();
            boolean found = false;
            while (it2.hasNext()) {
                T other = it2.next();
                found = compareKeyOnly ? Objects.equals(ele.getKey(), other.getKey()) : areEqual(ele, other);
                if (found) {
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
        Type classType = ((ParameterizedType) type).getActualTypeArguments()[0];
        return classType;
    }

    protected boolean areEqual(T obj1, T obj2) {
        return obj1.getKey().equals(obj2.getKey());
    }

    protected UnMetDependencyGetter getDependencyGetter() {
        return null;
    }

    /**
     * Tells if this object needs to be deleted if its dependent object gets deleted
     * Ex : LocalUcastMac and LocalMacstMac.
     *
     * @return true if this object needs to be deleted if its dependent object gets deleted
     */
    protected boolean cascadeDelete() {
        return false;
    }

    protected boolean isRemoveCommand() {
        return false;
    }

    protected HwvtepDeviceInfo getDeviceInfo() {
        return getOperationalState().getDeviceInfo();
    }

    protected TransactionBuilder getDeviceTransaction() {
        return threadLocalDeviceTransaction.get();
    }

    @Override
    public void onSuccess(TransactionBuilder deviceTransaction) {
        if (deviceTransaction == null || !updates.containsKey(deviceTransaction)) {
            return;
        }
        onCommandSucceeded();
    }

    @Override
    public void onFailure(TransactionBuilder deviceTransaction) {
        if (deviceTransaction == null || !updates.containsKey(deviceTransaction)) {
            return;
        }
        for (MdsalUpdate mdsalUpdate : updates.get(deviceTransaction)) {
            getDeviceInfo().clearInTransit((Class<? extends Identifiable>) mdsalUpdate.getClass(),
                    mdsalUpdate.getKey());
        }
        onCommandFailed();
    }

    protected void onCommandSucceeded() {
    }

    protected void onCommandFailed() {
    }

    void updateControllerTxHistory(TransactionType transactionType, Object element) {
        getOperationalState().getDeviceInfo().addToControllerTx(transactionType, element);
    }
}
