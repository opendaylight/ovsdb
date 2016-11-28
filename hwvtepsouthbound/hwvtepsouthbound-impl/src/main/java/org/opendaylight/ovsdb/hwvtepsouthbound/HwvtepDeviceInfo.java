/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.clearData;
import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.getData;
import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.updateData;

/*
 * HwvtepDeviceInfo is used to store some of the table entries received
 * in updates from a Hwvtep device. There will be one instance of this per
 * Hwvtep device connected. Table entries are stored in a map keyed by
 * uuids of respective rows.
 *
 * Purpose of this class is to provide data present in tables which
 * were updated in a previous transaction and are not available in
 * current updatedRows. This allows us to handle updates for Tables
 * which reference other tables and need information in those tables
 * to add data to Operational data store.
 *
 * e.g. Mac-entries in data store use logical-switch-ref as one of the
 * keys. Mac-entry updates from switch rarely contain Logical_Switch
 * table entries. To add mac-entries we need table entries from
 * Logical_Switch table which were created in an earlier update.
 *
 */
public class HwvtepDeviceInfo {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepDeviceInfo.class);

    public static enum DeviceDataStatus {
        IN_TRANSIT,
        UNAVAILABLE,
        AVAILABLE
    }

    static class DeviceData {
        InstanceIdentifier key;
        UUID uuid;
        Object data;
        DeviceDataStatus status;

        public DeviceData(InstanceIdentifier key, UUID uuid, Object data, DeviceDataStatus status) {
            this.data = data;
            this.key = key;
            this.status = status;
            this.uuid = uuid;
        }
    }

    //TODO remove this
    private Map<UUID, LogicalSwitch> logicalSwitches = null;
    private Map<UUID, PhysicalSwitch> physicalSwitches = null;
    private Map<UUID, PhysicalLocator> physicalLocators = null;
    private Map<UUID, UUID> mapTunnelToPhysicalSwitch = null;

    private HwvtepConnectionInstance connectionInstance;

    private Map<Class<? extends DataObject>, Map<InstanceIdentifier, DeviceData>> configKeyVsData = new ConcurrentHashMap<>();
    private Map<Class<? extends DataObject>, Map<InstanceIdentifier, DeviceData>> opKeyVsData = new ConcurrentHashMap<>();
    private Map<Class<? extends DataObject>, Map<UUID, Object>> uuidVsData = new ConcurrentHashMap<>();

    public HwvtepDeviceInfo(HwvtepConnectionInstance hwvtepConnectionInstance) {
        this.connectionInstance = hwvtepConnectionInstance;
        this.logicalSwitches = new HashMap<>();
        this.physicalSwitches = new HashMap<>();
        this.physicalLocators = new HashMap<>();
        this.mapTunnelToPhysicalSwitch = new HashMap<>();
    }

    public void putLogicalSwitch(UUID uuid, LogicalSwitch lSwitch) {
        logicalSwitches.put(uuid, lSwitch);
    }

    public LogicalSwitch getLogicalSwitch(UUID uuid) {
        return logicalSwitches.get(uuid);
    }

    public LogicalSwitch removeLogicalSwitch(UUID uuid) {
        return logicalSwitches.remove(uuid);
    }

    public Map<UUID, LogicalSwitch> getLogicalSwitches() {
        return logicalSwitches;
    }

    public void putPhysicalSwitch(UUID uuid, PhysicalSwitch pSwitch) {
        physicalSwitches.put(uuid, pSwitch);
    }

    public PhysicalSwitch getPhysicalSwitch(UUID uuid) {
        return physicalSwitches.get(uuid);
    }

    public PhysicalSwitch removePhysicalSwitch(UUID uuid) {
        return physicalSwitches.remove(uuid);
    }

    public Map<UUID, PhysicalSwitch> getPhysicalSwitches() {
        return physicalSwitches;
    }

    public void putPhysicalLocator(UUID uuid, PhysicalLocator pLocator) {
        physicalLocators.put(uuid, pLocator);
    }

    public PhysicalLocator getPhysicalLocator(UUID uuid) {
        return physicalLocators.get(uuid);
    }

    public PhysicalLocator removePhysicalLocator(UUID uuid) {
        return physicalLocators.remove(uuid);
    }

    public Map<UUID, PhysicalLocator> getPhysicalLocators() {
        return physicalLocators;
    }

    public void putPhysicalSwitchForTunnel(UUID uuid, UUID psUUID) {
        mapTunnelToPhysicalSwitch.put(uuid, psUUID);
    }

    public PhysicalSwitch getPhysicalSwitchForTunnel(UUID uuid) {
        return physicalSwitches.get(mapTunnelToPhysicalSwitch.get(uuid));
    }

    public void removePhysicalSwitchForTunnel(UUID uuid) {
        mapTunnelToPhysicalSwitch.remove(uuid);
    }

    public Map<UUID, UUID> getPhysicalSwitchesForTunnels() {
        return mapTunnelToPhysicalSwitch;
    }

    public boolean isKeyInTransit(Class<? extends DataObject> cls, InstanceIdentifier key) {
        DeviceData deviceData = getData(opKeyVsData, cls, key);
        return deviceData != null && DeviceDataStatus.IN_TRANSIT == deviceData.status;
    }

    public boolean isConfigDataAvailable(Class<? extends DataObject> cls, InstanceIdentifier key) {
        return getData(configKeyVsData, cls, key) != null;
    }

    public void updateConfigData(Class<? extends DataObject> cls, InstanceIdentifier key, Object data) {
        updateData(configKeyVsData, cls, key,
                new DeviceData(key, null, data, DeviceDataStatus.AVAILABLE));
    }

    public void clearConfigData(Class<? extends DataObject> cls, InstanceIdentifier key) {
        clearData(configKeyVsData, cls, key);
    }

    public void markKeyAsInTransit(Class<? extends DataObject> cls, InstanceIdentifier key) {
        updateData(opKeyVsData, cls, key,
                new DeviceData(key, null, null, DeviceDataStatus.IN_TRANSIT));
    }

    public void updateDeviceOpData(Class<? extends DataObject> cls, InstanceIdentifier key, UUID uuid, Object data) {
        updateData(opKeyVsData, cls, key,
                new DeviceData(key, uuid, data, DeviceDataStatus.AVAILABLE));
    }

    public void clearDeviceOpData(Class<? extends DataObject> cls, InstanceIdentifier key) {
        clearData(opKeyVsData, cls, key);
    }

    public Object getDeviceOpData(Class<? extends DataObject> cls, UUID uuid) {
        return getData(uuidVsData, cls, uuid);
    }

    public UUID getUUID(Class<? extends DataObject> cls, InstanceIdentifier key) {
        DeviceData data = getData(opKeyVsData, cls, key);
        if (data != null) {
            return data.uuid;
        }
        return null;
    }
}
