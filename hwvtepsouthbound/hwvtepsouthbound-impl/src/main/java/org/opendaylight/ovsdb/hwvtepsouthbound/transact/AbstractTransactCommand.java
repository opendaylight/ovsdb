/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Lists;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractTransactCommand<T extends DataObject> implements TransactCommand {

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

    void updateCurrentTxDeleteData(InstanceIdentifier key, T data) {
        Class<? extends DataObject> cls = data.getClass();
        operationalState.updateCurrentTxDeleteData(cls, key);
        operationalState.getDeviceInfo().clearConfigData(cls, key);
    }

    void updateCurrentTxData(Class<? extends DataObject> cls, InstanceIdentifier key, UUID uuid, Object data) {
        operationalState.updateCurrentTxData(cls, key, uuid);
        operationalState.getDeviceInfo().markKeyAsInTransit(cls, key);
        operationalState.getDeviceInfo().updateConfigData(cls, key, data);
    }

    void processDependencies(UnMetDependencyGetter<T> unMetDependencyGetter,
                             TransactionBuilder transaction,
                             final InstanceIdentifier<Node> nodeIid,
                             final InstanceIdentifier<T> key,
                             final T data, final Object... extraData) {

        HwvtepDeviceInfo deviceInfo = operationalState.getDeviceInfo();
        Map inTransitDependencies = unMetDependencyGetter.getInTransitDependencies(operationalState, data);
        Map confingDependencies = unMetDependencyGetter.getUnMetConfigDependencies(operationalState, data);
        //we can skip the config termination point dependency
        confingDependencies.remove(TerminationPoint.class);

        //If this key itself is in transit wait for the response of this key itself
        if (deviceInfo.isKeyInTransit(data.getClass(), key)) {
            inTransitDependencies.put(data.getClass(), Lists.newArrayList(key));
        }

        if (confingDependencies.isEmpty() && inTransitDependencies.isEmpty()) {
            doDeviceTransaction(transaction, nodeIid, data, key, extraData);
            //TODO put proper uuid
            updateCurrentTxData(data.getClass(), key, new UUID("uuid"), data);
        }
        if (!confingDependencies.isEmpty()) {
            DependentJob<T> configWaitingJob = new DependentJob.ConfigWaitingJob(
                    key, data, confingDependencies) {

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder) {
                    AbstractTransactCommand.this.operationalState = operationalState;
                    onConfigUpdate(transactionBuilder, nodeIid, data);
                }
            };
            deviceInfo.addJobToQueue(configWaitingJob);
        }
        if (inTransitDependencies.size() > 0) {

            DependentJob<T> opWaitingJob = new DependentJob.OpWaitingJob(
                    key, data, inTransitDependencies) {

                @Override
                public void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder) {
                    AbstractTransactCommand.this.operationalState = operationalState;
                    onConfigUpdate(transactionBuilder, nodeIid, data, extraData);
                }
            };
            deviceInfo.addJobToQueue(opWaitingJob);
        }
    }

    protected void doDeviceTransaction(TransactionBuilder transaction,InstanceIdentifier<Node> nodeIid, T data,
                                       Object... extraData) {
        //NO OP default
    }

    protected void onConfigUpdate(TransactionBuilder transaction, InstanceIdentifier<Node> nodeIid, T data, Object... extraData) {
        //NO OP default
    }
}
