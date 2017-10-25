/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

public class DeviceData {

    private final Map<UUID, Row> allRowsByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, String> tableNameForUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> incomingRefsForUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> outgoingRefsForUuid = new ConcurrentHashMap<>();

    public Row getRowFromUuid(UUID uuid) {
        return allRowsByUuid.get(uuid);
    }

    public Set<UUID> getIncomingRefsForUuid(UUID uuid) {
        return incomingRefsForUuid.get(uuid);
    }

    public String getTableNameForUuid(UUID uuid) {
        return tableNameForUuid.get(uuid);
    }

    private boolean isPhysicalLocatorRow(UUID uuid) {
        return Objects.equals(Predicates.PHYSICAL_LOCATOR, tableNameForUuid.get(uuid));
    }

    private boolean isPhysicalLocatorSetRow(UUID uuid) {
        return Objects.equals(Predicates.PHYSICAL_LOCATOR_SET, tableNameForUuid.get(uuid));
    }

    private boolean hasEmptyInboundRefsFor(UUID uuid) {
        return incomingRefsForUuid.get(uuid) == null || incomingRefsForUuid.get(uuid).isEmpty();
    }

    public boolean isValidRow(UUID uuid) {
        return allRowsByUuid.containsKey(uuid);
    }

    public synchronized void commit(TxData txData) {
        allRowsByUuid.putAll(txData.getCreatedData());
        allRowsByUuid.putAll(txData.getUpdatedData());
        tableNameForUuid.putAll(txData.getCurrentTableNamesByUuid());
        txData.getDeletedUuids().forEach(deleted -> {
            allRowsByUuid.remove(deleted);
            tableNameForUuid.remove(deleted);
        });
        updateReferences();
    }

    public synchronized void clearLocatorSets() {
        boolean updatedLocatorSets = false;
        Iterator<Map.Entry<UUID, Row>> iterator = allRowsByUuid.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<UUID, Row> entry = iterator.next();
            if (isPhysicalLocatorSetRow(entry.getKey()) && hasEmptyInboundRefsFor(entry.getKey())) {
                iterator.remove();
                tableNameForUuid.remove(entry.getKey());
                updatedLocatorSets = true;
            }
        }
        if (updatedLocatorSets) {
            updateReferences();
        }
    }

    public synchronized Map<UUID, Row> clearLocators() {
        Map<UUID, Row> deletedLocators = new HashMap<>();
        Iterator<Map.Entry<UUID, Row>> iterator = allRowsByUuid.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<UUID, Row> entry = iterator.next();
            if (isPhysicalLocatorRow(entry.getKey()) && hasEmptyInboundRefsFor(entry.getKey())) {
                deletedLocators.put(entry.getKey(), entry.getValue());
                iterator.remove();
                tableNameForUuid.remove(entry.getKey());
            }
        }
        return deletedLocators;
    }

    private void updateReferences() {
        incomingRefsForUuid.clear();
        outgoingRefsForUuid.clear();
        List<Row> rows = new ArrayList<>(allRowsByUuid.values());
        rows.stream().forEach((row) -> {
            UUID srcUuid = TableUtil.getUuidOfRow(row);
            outgoingRefsForUuid.putIfAbsent(srcUuid, new HashSet());
            row.getColumns()
                    .stream()
                    .filter(Predicates.IS_NOT_UUID_COLUMN)
                    .forEach(column -> {
                        TableUtil.getOutGoingRefs((Column)column)
                                .forEach(dstUuid -> {
                                    incomingRefsForUuid.putIfAbsent(dstUuid, new HashSet());
                                    incomingRefsForUuid.get(dstUuid).add(srcUuid);
                                    outgoingRefsForUuid.get(srcUuid).add(dstUuid);
                                });
                    });
        });
    }

    public Row getRow(String tableName, String columnName, Object columnValue) {
        Iterator<Map.Entry<UUID, Row>> it = allRowsByUuid.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Row> entry = it.next();
            Iterator<Column> i2 = entry.getValue().getColumns().iterator();
            while (i2.hasNext()) {
                Column  column = i2.next();
                if (Objects.equals(columnName, column.getSchema().getName())) {
                    UUID uuid = entry.getKey();
                    if (Objects.equals(tableName, tableNameForUuid.get(uuid))) {
                        if (Objects.equals(column.getData(), columnValue)) {
                            return entry.getValue();
                        }
                    }
                }
            }
        }
        return null;
    }
}
