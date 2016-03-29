/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;

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
    private Map<UUID, LogicalSwitch> logicalSwitches = null;

    public HwvtepDeviceInfo() {
        this.logicalSwitches = new HashMap<>();
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
}
