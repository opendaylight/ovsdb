/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.EntryObject;

/**
 * Utility class to retrieve the unmet dependencies (config/operational) of the given object.
 */
public abstract class UnMetDependencyGetter<T extends EntryObject<?, ?, ?>> {

    private final ConfigDependencyGetter configDependencyGetter = new ConfigDependencyGetter();
    private final InTransitDependencyGetter inTransitDependencyGetter = new InTransitDependencyGetter();

    /**
     * Returns the iids this data depends upon
     * which are already intransit in the previous transaction if any.
     *
     * @param opState The operatonal state
     * @param data The data object
     * @return The depenencies
     */
    public Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier>> getInTransitDependencies(
            HwvtepOperationalState opState, T data) {
        return inTransitDependencyGetter.retrieveUnMetDependencies(opState, opState.getDeviceInfo(), data);
    }

    /**
     * Returns the iids this data depends upon
     * which are not yet present in the config data store if any.
     *
     * @param opState The operatonal state
     * @param data The data object
     * @return the      depenencies
     */
    public Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier>> getUnMetConfigDependencies(
            HwvtepOperationalState opState, T data) {
        return configDependencyGetter.retrieveUnMetDependencies(opState, opState.getDeviceInfo(), data);
    }

    abstract class DependencyGetter {

        Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier>> retrieveUnMetDependencies(
                HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo, T data) {

            Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier>> result = new HashMap<>();
            Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier<?>>> allKeys = new HashMap<>();
            allKeys.put(LogicalSwitches.class, getLogicalSwitchDependencies(data));
            allKeys.put(TerminationPoint.class, getTerminationPointDependencies(data));

            for (var entry : allKeys.entrySet()) {
                Class<? extends EntryObject<?, ?, ?>> cls = entry.getKey();
                List<DataObjectIdentifier<? extends DataObject>> keysToCheck = entry.getValue();
                for (DataObjectIdentifier<? extends DataObject> key : keysToCheck) {
                    if (!isDependencyMet(opState, deviceInfo, cls, key)) {
                        result = addToResultMap(result, cls, key);
                    }
                }
            }
            return result;
        }

        Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier>> addToResultMap(
                Map<Class<? extends EntryObject<?, ?, ?>>, List<DataObjectIdentifier>> result,
                Class<? extends EntryObject<?, ?, ?>> cls, DataObjectIdentifier<? extends DataObject> key) {
            if (null == result) {
                result = new HashMap<>();
            }
            if (!result.containsKey(cls)) {
                result.put(cls, new ArrayList<>());
            }
            result.get(cls).add(key);
            return result;
        }

        abstract boolean isDependencyMet(HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo,
                Class<? extends EntryObject<?, ?, ?>> cls, DataObjectIdentifier<? extends DataObject> key);
    }

    class ConfigDependencyGetter extends DependencyGetter {
        @Override
        boolean isDependencyMet(HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo,
                                Class<? extends EntryObject<?, ?, ?>> cls,
                                DataObjectIdentifier<? extends DataObject> key) {
            return deviceInfo.isConfigDataAvailable(cls, key) || isConfigDataAvailable(opState, cls, key);
        }

        boolean isConfigDataAvailable(HwvtepOperationalState opState,
                                      Class<? extends EntryObject<?, ?, ?>> cls,
                                      DataObjectIdentifier<? extends DataObject> key) {
            DataBroker db = opState.getConnectionInstance().getDataBroker();
            Optional data = HwvtepSouthboundUtil.readNode(db, LogicalDatastoreType.CONFIGURATION, key);
            if (data.isPresent()) {
                opState.getDeviceInfo().updateConfigData(cls, key, data.orElseThrow());
                return true;
            }
            return false;
        }
    }

    class InTransitDependencyGetter extends DependencyGetter {
        @Override
        boolean isDependencyMet(HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo,
                Class<? extends EntryObject<?, ?, ?>> cls, DataObjectIdentifier<? extends DataObject> key) {
            return opState.isKeyPartOfCurrentTx(cls, key) || !deviceInfo.isKeyInTransit(cls, key);
        }
    }

    public abstract List<DataObjectIdentifier<?>> getLogicalSwitchDependencies(T data);

    public abstract List<DataObjectIdentifier<?>> getTerminationPointDependencies(T data);
}
