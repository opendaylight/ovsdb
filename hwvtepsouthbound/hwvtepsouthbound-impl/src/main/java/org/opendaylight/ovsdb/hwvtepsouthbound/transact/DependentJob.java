/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DependentJob<T extends Identifiable> {

    private static final Logger LOG = LoggerFactory.getLogger(DependentJob.class);

    private static final Predicate<HwvtepDeviceInfo.DeviceData> DATA_INTRANSIT
            = (controllerData) -> controllerData != null && controllerData.isInTransitState();

    private static final Predicate<HwvtepDeviceInfo.DeviceData> DATA_INTRANSIT_EXPIRED
            = (controllerData) -> controllerData != null && controllerData.isInTransitState()
            && controllerData.isIntransitTimeExpired();

    //expecting the device to create the data
    private static final BiPredicate<HwvtepDeviceInfo.DeviceData, Optional<TypedBaseTable>> INTRANSIT_DATA_CREATED
            = (controllerData, deviceData) -> controllerData.getUuid() == null && deviceData.isPresent();

    private static final BiPredicate<HwvtepDeviceInfo.DeviceData, Optional<TypedBaseTable>> INTRANSIT_DATA_NOT_CREATED
            = (controllerData, deviceData) -> controllerData.getUuid() == null && !deviceData.isPresent();

    //expecting the device to delete the data
    private static final BiPredicate<HwvtepDeviceInfo.DeviceData, Optional<TypedBaseTable>> INTRANSIT_DATA_DELETED
            = (controllerData, deviceData) -> controllerData.getUuid() != null && !deviceData.isPresent();

    private static final BiPredicate<HwvtepDeviceInfo.DeviceData, Optional<TypedBaseTable>> INTRANSIT_DATA_NOT_DELETED
            = (controllerData, deviceData) -> controllerData.getUuid() != null && deviceData.isPresent();

    private final long expiryTime;
    private final InstanceIdentifier key;
    private final T data;
    private final Map<Class<? extends DataObject>, List<InstanceIdentifier>> dependencies;
    private final long transactionId;

    DependentJob(InstanceIdentifier key,
                           T data, Map<Class<? extends DataObject>, List<InstanceIdentifier>> dependencies,
                 long transactionId) {
        this.expiryTime = System.currentTimeMillis() + HwvtepSouthboundConstants.WAITING_JOB_EXPIRY_TIME_MILLIS;
        this.key = key;
        this.data = data;
        this.dependencies = dependencies;
        this.transactionId = transactionId;
    }

    /**
     * This call back method gets called when all its dependencies are resolved.
     *
     * @param operationalState   new current operational state
     * @param transactionBuilder transaction builder to create device transaction
     */
    protected abstract void onDependencyResolved(HwvtepOperationalState operationalState,
                                                 TransactionBuilder transactionBuilder);

    /**
     * This method is to check if all the given dependency of this job or not.
     *
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
     * This method checks if all the dependencies of this job or met or not.
     *
     * @param deviceInfo The device info of this job
     * @return true if all the dependencies are met
     */
    boolean areDependenciesMet(HwvtepDeviceInfo deviceInfo) {
        for (Entry<Class<? extends DataObject>, List<InstanceIdentifier>> entry : dependencies.entrySet()) {
            Class<? extends DataObject> cls = entry.getKey();
            for (InstanceIdentifier<?> iid : entry.getValue()) {
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

    public long getTransactionId() {
        return transactionId;
    }

    public Map<Class<? extends DataObject>, List<InstanceIdentifier>> getDependencies() {
        return dependencies;
    }

    public T getData() {
        return data;
    }

    public boolean isConfigWaitingJob() {
        return true;
    }

    public void onFailure() {
    }

    public void onSuccess() {
    }

    public abstract static class ConfigWaitingJob<T extends Identifiable> extends DependentJob<T> {

        public ConfigWaitingJob(InstanceIdentifier key, T data,
                Map<Class<? extends DataObject>, List<InstanceIdentifier>> dependencies) {
            super(key, data, dependencies, 0);
        }

        @Override
        protected boolean isDependencyMet(HwvtepDeviceInfo deviceInfo, Class cls, InstanceIdentifier iid) {
            return deviceInfo.isConfigDataAvailable(cls, iid);
        }

        public boolean isConfigWaitingJob() {
            return true;
        }
    }

    public abstract static class OpWaitingJob<T extends Identifiable> extends DependentJob<T> {

        public OpWaitingJob(InstanceIdentifier key, T data,
                Map<Class<? extends DataObject>, List<InstanceIdentifier>> dependencies,
                            long transactionId) {
            super(key, data, dependencies, transactionId);
        }

        @Override
        protected boolean isDependencyMet(HwvtepDeviceInfo deviceInfo, Class cls, InstanceIdentifier iid) {
            boolean depenencyMet = true;
            HwvtepDeviceInfo.DeviceData controllerData = deviceInfo.getDeviceOperData(cls, iid);

            if (DATA_INTRANSIT_EXPIRED.test(controllerData)) {
                LOG.info("Intransit state expired for key: {} --- dependency {}", iid, getKey());

                //either the device acted on the selected iid/uuid and sent the updated event or it did not
                //here we are querying the device directly to get the latest status on the iid
                Optional<TypedBaseTable> latestDeviceStatus = deviceInfo.getConnectionInstance()
                        .getHwvtepTableReader().getHwvtepTableEntryUUID(cls, iid, controllerData.getUuid());

                TypedBaseTable latestDeviceData = latestDeviceStatus.orElse(null);

                if (INTRANSIT_DATA_CREATED.test(controllerData, latestDeviceStatus)) {
                    LOG.info("Intransit expired key is actually created but update is missed/delayed {}", iid);
                    deviceInfo.updateDeviceOperData(cls, iid, latestDeviceData.getUuid(), latestDeviceData);

                } else if (INTRANSIT_DATA_NOT_CREATED.test(controllerData, latestDeviceStatus)) {
                    LOG.info("Intransit expired key is actually not created but update is missed/delayed {}", iid);
                    deviceInfo.clearDeviceOperData(cls, iid);

                } else if (INTRANSIT_DATA_DELETED.test(controllerData, latestDeviceStatus)) {
                    //also deleted from device
                    LOG.info("Intransit expired key is actually deleted but update is missed/delayed {}", iid);
                    deviceInfo.clearDeviceOperData(cls, iid);

                } else if (INTRANSIT_DATA_NOT_DELETED.test(controllerData, latestDeviceStatus)) {
                    //not deleted from device we will reuse existing uuid
                    LOG.info("Intransit expired key is actually not deleted but update is missed/delayed {}", iid);
                    deviceInfo.updateDeviceOperData(cls, iid, latestDeviceData.getUuid(), latestDeviceData);
                }
            } else if (DATA_INTRANSIT.test(controllerData)) {
                //device status is still in transit
                depenencyMet = false;
            }
            return depenencyMet;
        }

        @Override
        public boolean isConfigWaitingJob() {
            return false;
        }
    }
}
