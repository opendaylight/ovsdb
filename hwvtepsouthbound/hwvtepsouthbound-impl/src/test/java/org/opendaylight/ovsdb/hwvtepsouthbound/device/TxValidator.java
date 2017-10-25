/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.device;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

public class TxValidator {

    public void validate(TxData currentTxData, DeviceData deviceData) throws IllegalStateException {
        if (!containsvalidUuidRefs(currentTxData, deviceData)) {
            throw new IllegalStateException("Invalid uuid references found");
        }
        if (hasLiveReferencesForDeletedData(currentTxData, deviceData)) {
            throw new IllegalStateException("Still some references are left for deleted uuid ");
        }
    }

    private boolean hasLiveReferencesForDeletedData(TxData currentTxData, DeviceData deviceData) {
        return false;//TODO
    }

    private boolean containsvalidUuidRefs(TxData currentTxData, DeviceData deviceData) {
        List<Row> rowsCreatedOrUpdated = new ArrayList<>(currentTxData.getCreatedData().values());
        rowsCreatedOrUpdated.addAll(currentTxData.getUpdatedData().values());
        return rowsCreatedOrUpdated
                .stream()
                .allMatch((row) -> isValidRow(row, currentTxData, deviceData));
    }

    private boolean isValidRow(Row row, TxData currentTxData, DeviceData deviceData) {
        return false;//TODO
    }
}
