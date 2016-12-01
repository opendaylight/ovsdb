/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;
import java.util.Map;

public abstract class DependentJob<T extends DataObject> {

    private final long expiryTime;
    private final InstanceIdentifier key;
    private final T data;
    private final Map<Class<? extends DataObject>, List<InstanceIdentifier>> dependencies;

    DependentJob(InstanceIdentifier key,
                           T data, Map<Class<? extends DataObject>, List<InstanceIdentifier>> dependencies) {
        this.expiryTime = System.currentTimeMillis() + HwvtepSouthboundConstants.WAITING_JOB_EXPIRY_TIME_MILLIS;
        this.key = key;
        this.data = data;
        this.dependencies = dependencies;
    }

    /**
     * This call back method gets called when all its dependencies are resolved
     * @param operationalState   new current operational state
     * @param transactionBuilder transaction builder to create device transaction
     */
    protected abstract void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder);

    /**
     * This method is to check if all the given dependency of this job or not
     * @param deviceInfo   The device info of tis job
     * @param cls          dependency type to be checked for
     * @param iid          instance identifier to be checked for
     * @return true if the dependency is met
     */
    protected abstract boolean isDependencyMet(HwvtepDeviceInfo deviceInfo, Class<? extends DataObject> cls,
                                               InstanceIdentifier iid);

    boolean isExpired(long currentTime) {
        return currentTime > expiryTime;
    }

    /**
     * This method checks if all the dependencies of this job or met or not
     * @param deviceInfo The device info of this job
     * @return true if all the dependencies are met
     */
    boolean areDependenciesMet(HwvtepDeviceInfo deviceInfo) {
        for (Class<? extends DataObject> cls : dependencies.keySet()) {
            for (InstanceIdentifier iid : dependencies.get(cls)) {
                if (!isDependencyMet(deviceInfo, cls, iid)) {
                    return false;
                }
            }
        }
        return true;
    }

    public InstanceIdentifier getKey() {
        return key;
    }

    public T getData() {
        return data;
    }

    public abstract static class ConfigWaitingJob<T extends DataObject> extends DependentJob {

        public ConfigWaitingJob(InstanceIdentifier key, T data, Map dependencies) {
            super(key, data, dependencies);
        }

        @Override
        protected boolean isDependencyMet(HwvtepDeviceInfo deviceInfo, Class cls, InstanceIdentifier iid) {
            return deviceInfo.isConfigDataAvailable(cls, iid);
        }
    }

    public abstract static class OpWaitingJob<T extends DataObject> extends DependentJob {

        public OpWaitingJob(InstanceIdentifier key, T data, Map dependencies) {
            super(key, data, dependencies);
        }

        @Override
        protected boolean isDependencyMet(HwvtepDeviceInfo deviceInfo, Class cls, InstanceIdentifier iid) {
            HwvtepDeviceInfo.DeviceData deviceData = deviceInfo.getDeviceOpData(cls, iid);
            return deviceData == null || deviceData.getStatus() != HwvtepDeviceInfo.DeviceDataStatus.IN_TRANSIT;
        }
    }
}
