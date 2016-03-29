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

public class HwvtepDeviceState {
    private Map<UUID, LogicalSwitch> logicalSwitches = null;

    public HwvtepDeviceState() {
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
