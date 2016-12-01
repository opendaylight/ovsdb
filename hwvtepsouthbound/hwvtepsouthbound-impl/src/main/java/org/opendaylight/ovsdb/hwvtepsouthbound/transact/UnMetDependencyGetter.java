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
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to retrieve the unmet dependencies (config/operational) of the given object
 * @param <T>
 */
public abstract class UnMetDependencyGetter<T extends Identifiable> {

    private final ConfigDependencyGetter configDependencyGetter = new ConfigDependencyGetter();
    private final InTransitDependencyGetter inTransitDependencyGetter = new InTransitDependencyGetter();

    /**
     * Returns the iids this data depends upon
     * which are already intransit in the previous transaction if any
     * @param opState   The operatonal state
     * @param data      The data object
     * @return          The depenencies
     */
    public Map<Class<? extends Identifiable>, List<InstanceIdentifier>> getInTransitDependencies(
            HwvtepOperationalState opState, T data) {
        return inTransitDependencyGetter.retrieveUnMetDependencies(opState, opState.getDeviceInfo(), data);
    }

    /**
     * Returns the iids this data depends upon
     * which are not yet present in the config data store if any
     * @param opState   The operatonal state
     * @param data      The data object
     * @return the      depenencies
     */
    public Map<Class<? extends Identifiable>, List<InstanceIdentifier>> getUnMetConfigDependencies(
            HwvtepOperationalState opState, T data) {
        return configDependencyGetter.retrieveUnMetDependencies(opState, opState.getDeviceInfo(), data);
    }

    abstract class DependencyGetter {

        Map<Class<? extends Identifiable>, List<InstanceIdentifier>> retrieveUnMetDependencies(
                HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo, T data) {

            Map<Class<? extends Identifiable>, List<InstanceIdentifier>> result = new HashMap<>();
            Map<Class<? extends Identifiable>, List<InstanceIdentifier<?>>> allKeys = new HashMap<>();
            allKeys.put(LogicalSwitches.class, getLogicalSwitchDependencies(data));
            allKeys.put(TerminationPoint.class, getTerminationPointDependencies(data));

            for (Class<? extends Identifiable> cls : allKeys.keySet()) {
                List<InstanceIdentifier<? extends DataObject>> keysToCheck = allKeys.get(cls);
                for (InstanceIdentifier<? extends DataObject> key : keysToCheck) {
                    if (!isDependencyMet(opState, deviceInfo, cls, key)) {
                        result = addToResultMap(result, cls, key);
                    }
                }
            }
            return result;
        }

        Map<Class<? extends Identifiable>, List<InstanceIdentifier>> addToResultMap(
                Map<Class<? extends Identifiable>, List<InstanceIdentifier>> result,
                Class<? extends Identifiable> cls, InstanceIdentifier<? extends DataObject> key) {
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
                                         Class<? extends Identifiable> cls, InstanceIdentifier<? extends DataObject> key);
    }

    class ConfigDependencyGetter extends DependencyGetter {
        boolean isDependencyMet(HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo,
                                Class<? extends Identifiable> cls, InstanceIdentifier<? extends DataObject> key) {
            return deviceInfo.isConfigDataAvailable(cls, key) || isConfigDataAvailable(opState, key);
        }

        boolean isConfigDataAvailable(HwvtepOperationalState opState, InstanceIdentifier<? extends DataObject> key) {
            DataBroker db = opState.getConnectionInstance().getDataBroker();
            MdsalUtils mdsalUtils = new MdsalUtils(db);
            return mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, key) != null;
        }
    }

    class InTransitDependencyGetter extends DependencyGetter {
        boolean isDependencyMet(HwvtepOperationalState opState, HwvtepDeviceInfo deviceInfo,
                                Class<? extends Identifiable> cls, InstanceIdentifier<? extends DataObject> key) {
            return opState.isKeyPartOfCurrentTx(cls, key) || !deviceInfo.isKeyInTransit(cls, key);
        }
    }

    List<InstanceIdentifier<?>> getLogicalSwitchDependencies(T data) {
        return Collections.EMPTY_LIST;
    }

    List<InstanceIdentifier<?>> getTerminationPointDependencies(T data) {
        return Collections.EMPTY_LIST;
    }
}