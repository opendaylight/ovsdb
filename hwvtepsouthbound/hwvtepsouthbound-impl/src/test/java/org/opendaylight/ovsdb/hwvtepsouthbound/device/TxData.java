/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

public class TxData {

    private final Map<UUID, UUID> namedUuidsInCurrentTx = new HashMap<>();
    private final Map<UUID, Row> created = new HashMap<>();
    private final Map<UUID, Row> updated = new HashMap<>();
    private final Set<UUID> deleted = new HashSet<>();
    private final Map<UUID, String> currentTableNamesByUuid = new HashMap<>();

    public void addToCreatedData(UUID uuid, Row row) {
        created.put(uuid, row);
    }

    public void addToUpdatedData(UUID uuid, Row row) {
        updated.put(uuid, row);
    }

    public void addToDeletedData(UUID uuid) {
        deleted.add(uuid);
    }

    public void addTableNameForUuid(UUID uuid, String tableName) {
        currentTableNamesByUuid.put(uuid, tableName);
    }

    public void addActualUuidForNamedUuid(UUID named, UUID actual) {
        namedUuidsInCurrentTx.put(named, actual);
    }

    public UUID getActualUuidForNamedUuid(UUID named) {
        return namedUuidsInCurrentTx.get(named);
    }

    public String getTableNameForUuid(UUID uuid) {
        return currentTableNamesByUuid.get(uuid);
    }

    public Map<UUID, Row> getCreatedData() {
        return created;
    }

    public Set<UUID> getDeletedUuids() {
        return deleted;
    }

    public Map<UUID, Row> getUpdatedData() {
        return updated;
    }

    public Map<UUID, String> getCurrentTableNamesByUuid() {
        return currentTableNamesByUuid;
    }
}