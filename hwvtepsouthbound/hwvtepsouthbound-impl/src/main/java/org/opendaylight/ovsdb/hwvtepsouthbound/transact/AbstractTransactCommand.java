/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import com.google.common.collect.Lists;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepTableReader;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
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
    protected volatile HwvtepOperationalState hwvtepOperationalState = null;
    protected volatile TransactionBuilder deviceTransaction = null;
    private Collection<DataTreeModification<Node>> changes;
    Set<MdsalUpdate<T>> updates = new HashSet<>();

    protected AbstractTransactCommand() {
        // NO OP
    }

    public AbstractTransactCommand(HwvtepOperationalState state, Collection<DataTreeModification<Node>> changes) {
        this.hwvtepOperationalState = state;
        this.changes = changes;
    }

    public HwvtepOperationalState getOperationalState() {
        return hwvtepOperationalState;
    }

    public DataBroker getDataBroker() {
        return getOperationalState().getDataBroker();
    }

    public Collection<DataTreeModification<Node>> getChanges() {
        return changes;
    }

    void updateCurrentTxDeleteData(Class<? extends Identifiable> cls, InstanceIdentifier key, T data) {
        hwvtepOperationalState.updateCurrentTxDeleteData(cls, key);
        markKeyAsInTransit(cls, key);
        addToUpdates(key, data);
    }

    void updateCurrentTxData(Class<? extends Identifiable> cls, InstanceIdentifier key, UUID uuid, T data) {
        hwvtepOperationalState.updateCurrentTxData(cls, key, uuid);
        markKeyAsInTransit(cls, key);
        addToUpdates(key, data);
    }

    void addToUpdates(InstanceIdentifier key, T data) {
        T oldData = null;
        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (getConfigData((Class<? extends Identifiable>) classType, key) != null) {
            oldData = (T) getConfigData((Class<? extends Identifiable>) classType, key).getData();
        }
        updates.add(new MdsalUpdate<T>(key, data, oldData));
    }

    void processDependencies(final UnMetDependencyGetter<T> unMetDependencyGetter,
            final TransactionBuilder transaction,
            final InstanceIdentifier<Node> nodeIid,
            final InstanceIdentifier key,
            final T data, final Object... extraData) {

        HwvtepDeviceInfo deviceInfo = hwvtepOperationalState.getDeviceInfo();
        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType) type).getActualTypeArguments()[0];
        Map inTransitDependencies = Collections.emptyMap();
        Map confingDependencies = Collections.emptyMap();

        if (isDeleteCmd()) {
            if (deviceInfo.isKeyInTransit((Class<? extends Identifiable>) classType, key)) {
                inTransitDependencies = new HashMap<>();
                inTransitDependencies.put((Class<? extends Identifiable>) classType, Lists.newArrayList(key));
            }
        } else {
            inTransitDependencies = unMetDependencyGetter.getInTransitDependencies(hwvtepOperationalState, data);
            confingDependencies = unMetDependencyGetter.getUnMetConfigDependencies(hwvtepOperationalState, data);
            //we can skip the config termination point dependency as we can create them in device as part of this tx
            confingDependencies.remove(TerminationPoint.class);

            //If this key itself is in transit wait for the response of this key itself
            if (deviceInfo.isKeyInTransit((Class<? extends Identifiable>) classType, key)
                    || deviceInfo.isKeyInDependencyQueue(key)) {
                inTransitDependencies.put((Class<? extends Identifiable>) classType, Lists.newArrayList(key));
            }
        }
        LOG.info("Update received for key: {} txId: {}", key, getOperationalState().getTransactionId());
        if (HwvtepSouthboundUtil.isEmptyMap(confingDependencies)
                && HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {
            doDeviceTransaction(transaction, nodeIid, data, key, extraData);
            if (isDeleteCmd()) {
                getDeviceInfo().clearConfigData((Class<? extends Identifiable>) classType, key);
            } else {
                getDeviceInfo().updateConfigData((Class<? extends Identifiable>) classType, key, data);
            }
        }

        if (!HwvtepSouthboundUtil.isEmptyMap(confingDependencies)) {
            DependentJob<T> configWaitingJob = new DependentJob.ConfigWaitingJob(
                    key, data, confingDependencies) {
                AbstractTransactCommand clone = getClone();

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder) {
                    clone.hwvtepOperationalState = operationalState;
                    HwvtepDeviceInfo.DeviceData deviceData =
                            getDeviceInfo().getConfigData((Class<? extends Identifiable>)getClassType(), key);
                    T latest = data;
                    if (deviceData != null && deviceData.getData() != null) {
                        latest = (T) deviceData.getData();
                        clone.onConfigUpdate(transactionBuilder, nodeIid, latest, key, extraData);
                    } else if (isDeleteCmd()) {
                        clone.onConfigUpdate(transactionBuilder, nodeIid, latest, key, extraData);
                    }
                }

                @Override
                public void onFailure() {
                    clone.onFailure(transaction);
                }

                @Override
                public void onSuccess() {
                    clone.onSuccess(transaction);
                }
            };
            LOG.info("Update Adding to config wait queue for key: {} txId: {}",
                    key, getOperationalState().getTransactionId());
            addJobToQueue(configWaitingJob);
            return;
        }
        final long transactionId = hwvtepOperationalState.getTransactionId();
        if (!HwvtepSouthboundUtil.isEmptyMap(inTransitDependencies)) {

            DependentJob<T> opWaitingJob = new DependentJob.OpWaitingJob(
                    key, data, inTransitDependencies, transactionId) {
                AbstractTransactCommand clone = getClone();

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder) {
                    clone.hwvtepOperationalState = operationalState;
                    HwvtepDeviceInfo.DeviceData deviceData = getDeviceInfo()
                            .getConfigData((Class<? extends Identifiable>)getClassType(), key);
                    T latest = data;
                    if (deviceData != null && deviceData.getData() != null) {
                        latest = (T) deviceData.getData();
                        clone.onConfigUpdate(transactionBuilder, nodeIid, latest, key, extraData);
                    } else if (isDeleteCmd()) {
                        clone.onConfigUpdate(transactionBuilder, nodeIid, latest, key, extraData);
                    }
                }

                @Override
                public void onFailure() {
                    clone.onFailure(transaction);
                }

                @Override
                public void onSuccess() {
                    clone.onSuccess(transaction);
                }
            };
            LOG.info("Update Adding to op wait queue for key: {} txId: {}", key, transactionId);
            addJobToQueue(opWaitingJob);
            return;
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

    @NonNull
    protected Map<InstanceIdentifier<Node>, List<T>> extractRemoved(
            Collection<DataTreeModification<Node>> modification, Class<T> class1) {
        Map<InstanceIdentifier<Node>, List<T>> result = new HashMap<>();
        if (modification != null && !modification.isEmpty()) {
            for (DataTreeModification<Node> change : modification) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                if (!Objects.equals(hwvtepOperationalState.getConnectionInstance().getInstanceIdentifier(), key)) {
                    continue;
                }
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

    @NonNull
    protected Map<InstanceIdentifier<Node>, List<T>> extractUpdated(
            Collection<DataTreeModification<Node>> modification, Class<T> class1) {
        Map<InstanceIdentifier<Node>, List<T>> result = new HashMap<>();
        if (modification != null && !modification.isEmpty()) {
            for (DataTreeModification<Node> change : modification) {
                InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                if (!Objects.equals(hwvtepOperationalState.getConnectionInstance().getInstanceIdentifier(), key)) {
                    continue;
                }
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
            return Collections.EMPTY_LIST;
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
            return Collections.EMPTY_LIST;
        }
        if (HwvtepSouthboundUtil.isEmpty(list2)) {
            return HwvtepSouthboundUtil.isEmpty(list1) ? Collections.EMPTY_LIST : list1;
        }

        Map<Object, T> map1 = list1.stream().collect(Collectors.toMap(ele -> ele.key(), ele -> ele));
        Map<Object, T> map2 = list2.stream().collect(Collectors.toMap(ele -> ele.key(), ele -> ele));
        map1.entrySet().forEach(entry1 -> {
            T val2 = map2.remove(entry1.getKey());
            if (compareKeyOnly) {
                if (val2 == null) {
                    result.add(entry1.getValue());
                }
            } else {
                if (val2 == null) {
                    result.add(entry1.getValue());
                    return;
                }
                if (!areEqual(entry1.getValue(), val2)) {
                    result.add(entry1.getValue());
                }
            }
        });
        return result;
    }


    protected Type getClassType() {
        Type type = getClass().getGenericSuperclass();
        Type classType = ((ParameterizedType) type).getActualTypeArguments()[0];
        return classType;
    }

    protected boolean areEqual(T obj1, T obj2) {
        return obj1.key().equals(obj2.key());
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

    protected boolean isDeleteCmd() {
        return false;
    }

    protected HwvtepDeviceInfo getDeviceInfo() {
        return getOperationalState().getDeviceInfo();
    }

    protected TransactionBuilder getDeviceTransaction() {
        return deviceTransaction;
    }

    @Override
    public void onSuccess(TransactionBuilder deviceTx) {
        onCommandSucceeded();
    }

    @Override
    public void onFailure(TransactionBuilder deviceTx) {
        onCommandFailed();
    }

    protected void onCommandSucceeded() {
    }

    protected void onCommandFailed() {
    }

    void updateControllerTxHistory(TransactionType transactionType, Object element) {
        getOperationalState().getDeviceInfo().addToControllerTx(transactionType, element);
    }

    public <T> HwvtepDeviceInfo.DeviceData fetchDeviceData(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        HwvtepDeviceInfo.DeviceData deviceData  = getDeviceOpData(cls, key);
        if (deviceData == null) {
            LOG.debug("Could not find data for key {}", key);
            java.util.Optional<TypedBaseTable> optional =
                    getTableReader().getHwvtepTableEntryUUID(cls, key, null);
            if (optional.isPresent()) {
                LOG.debug("Found the data for key from device {} ", key);
                getDeviceInfo().updateDeviceOperData(cls, key, optional.get().getUuid(), (T)optional.get());
                return getDeviceOpData(cls, key);
            } else {
                LOG.info("Could not Find the data for key from device {} ", key);
            }
        }
        return deviceData;
    }

    protected String getKeyStr(InstanceIdentifier iid) {
        return iid.toString();
    }

    public <K extends Identifiable> void addJobToQueue(DependentJob<K> job) {
        hwvtepOperationalState.getDeviceInfo().putKeyInDependencyQueue(job.getKey());
        hwvtepOperationalState.getDeviceInfo().addJobToQueue(job);
    }

    public void markKeyAsInTransit(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        hwvtepOperationalState.getDeviceInfo().markKeyAsInTransit(cls, key);
    }

    public HwvtepDeviceInfo.DeviceData getDeviceOpData(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        return getOperationalState().getDeviceInfo().getDeviceOperData(cls, key);
    }

    public void clearConfigData(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        hwvtepOperationalState.getDeviceInfo().clearConfigData(cls, key);
    }

    public HwvtepDeviceInfo.DeviceData getConfigData(Class<? extends Identifiable> cls, InstanceIdentifier key) {
        return hwvtepOperationalState.getDeviceInfo().getConfigData(cls, key);
    }

    public void updateConfigData(Class<? extends Identifiable> cls, InstanceIdentifier key, Object data) {
        hwvtepOperationalState.getDeviceInfo().updateConfigData(cls, key, data);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public AbstractTransactCommand getClone() {
        try {
            return (AbstractTransactCommand) getClass().getConstructor(HwvtepOperationalState.class, Collection.class)
                    .newInstance(hwvtepOperationalState, changes);
        } catch (Throwable e) {
            LOG.error("Failed to clone the cmd ", e);
        }
        return this;
    }

    public HwvtepTableReader getTableReader() {
        return getOperationalState().getConnectionInstance().getHwvtepTableReader();
    }

    public HwvtepConnectionInstance getConnectionInstance() {
        return hwvtepOperationalState.getConnectionInstance();
    }

    public HwvtepOperationalState newOperState() {
        return new HwvtepOperationalState(getConnectionInstance());
    }
}
