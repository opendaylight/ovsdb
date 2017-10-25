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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.schema.BaseType;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.ColumnType;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;

public class TableUtil {

    public static Row createRow(final Operation operation,
                                final UUID uuid,
                                final Map<String, Object> newRow,
                                final Row oldRow) {
        Row updatedRow = new Row();
        if (oldRow != null) {
            for (Object obj : oldRow.getColumns()) {
                Column column = (Column) obj;
                updatedRow.addColumn(column.getSchema().getName(), column);
            }
        }
        Set<Map.Entry<String, Object>> entrySet = newRow.entrySet();
        entrySet.forEach( (entry) -> {
            String columnName = entry.getKey();
            Column<GenericTableSchema, Object> column = createColumn(operation, entry);
            updatedRow.addColumn(columnName, column);
        });
        updatedRow.addColumn(Predicates.UUID_COLUMN_NAME, new Column(new ColumnSchema(Predicates.UUID_COLUMN_NAME,
                new ColumnType.AtomicColumnType(new BaseType.UuidBaseType())), uuid));
        return updatedRow;
    }

    public static Column<GenericTableSchema, Object> createColumn(final Operation update,
                                                                  final Map.Entry<String, Object> entry) {
        String columnName = entry.getKey();
        GenericTableSchema tableSchema = (GenericTableSchema) update.getTableSchema();
        ColumnSchema<GenericTableSchema, Object> columnSchema =
                TyperUtils.getColumnSchema(tableSchema, columnName, (Class<Object>) entry.getValue().getClass());
        return new Column<>(columnSchema, entry.getValue());
    }

    public static Stream<UUID> getOutGoingRefs(final Column column) {
        List<UUID> refUuids = new ArrayList<>();
        Collection refs = new ArrayList();
        if (column.getData() instanceof UUID) {
            refUuids.add((UUID) column.getData());
        } else if (column.getData() instanceof Collection) {
            refs = (Collection) column.getData();

        } else if (column.getData() instanceof Map) {
            refs = ((Map)column.getData()).values();
        }
        if (refs != null) {
            refs.forEach( (ref) -> {
                if (ref instanceof UUID) {
                    refUuids.add((UUID) ref);
                }
            });
        }
        return refUuids.stream();
    }

    public static void replaceNamedUuidRefs(final Collection<Row> rows,
                                            final TxData txData) {
        rows.stream().forEach((row) -> {
            row.getColumns().stream()
                .filter(Predicates.IS_NOT_UUID_COLUMN)
                .forEach((column1) -> {
                    Column column = (Column) column1;

                    if (column.getData() instanceof UUID) {
                        UUID newUuid = getUuidfromNamedUuid(column.getData(), txData);
                        column.setData(newUuid);

                    } else if (column.getData() instanceof Collection) {
                        replaceUuidCollection(column, txData);

                    } else if (column.getData() instanceof Map) {
                        replaceUuidMap(column, txData);
                    }
                });
        });
    }

    public static void replaceUuidMap(final Column column,
                                      final TxData txData) {
        if (Predicates.IS_NOT_UUID_MAP.test((Map) column.getData())) {
            return;
        }
        Set<Map.Entry> columnData = ((Map)column.getData()).entrySet();
        Map newMap = new HashMap<>();
        columnData.forEach((entry) -> {
            UUID newUuid = getUuidfromNamedUuid(entry.getValue(), txData);
            newMap.put(entry.getKey(), newUuid);
        });
        column.setData(newMap);
    }

    public static void replaceUuidCollection(final Column column,
                                             final TxData txData) {
        if (Predicates.IS_NOT_UUID_COLLECTION.test((Collection)column.getData())) {
            return;
        }

        List<UUID> newData = new ArrayList<>((Collection)column.getData());
        Collection originalCollection = (Collection)column.getData();
        originalCollection.clear();
        newData.forEach(referred -> {
            UUID newUuid = getUuidfromNamedUuid(referred, txData);
            originalCollection.add(newUuid);
        });
    }

    public static UUID getUuidfromNamedUuid(final Object namedUuid,
                                            final TxData txData) {
        if (txData.getActualUuidForNamedUuid((UUID)namedUuid) != null) {
            return txData.getActualUuidForNamedUuid((UUID) namedUuid);
        }
        return (UUID)namedUuid;
    }

    public static UUID getUuidOfRow(final Row newRow) {
        for (Object columnObj : newRow.getColumns()) {
            Column column = (Column) columnObj;
            if (Predicates.IS_UUID_COLUMN.test(column)) {
                return (UUID) column.getData();
            }
        }
        return null;
    }

    public static TableUpdates createTableUpdate(final TxData txData,
                                                 final DeviceData deviceData) {

        Map<String, TableUpdate> tableUpdateMap = new HashMap<>();
        txData.getCreatedData().entrySet().forEach(entry -> {
            createTableUpdate(entry, tableUpdateMap, txData, deviceData,
                    Predicates.GET_OLD_ROW_FOR_CREATE, Predicates.GET_NEW_ROW_FOR_CREATE);
        });
        txData.getUpdatedData().entrySet().forEach(entry -> {
            createTableUpdate(entry, tableUpdateMap, txData, deviceData,
                    Predicates.GET_OLD_ROW_FOR_UPDATE, Predicates.GET_NEW_ROW_FOR_UPDATE);
        });
        txData.getDeletedUuids().forEach(deleted -> {
            createTableUpdate(new ImmutablePair<UUID, Row>(deleted, null), tableUpdateMap, txData, deviceData,
                    Predicates.GET_OLD_ROW_FOR_DELETE, Predicates.GET_NEW_ROW_FOR_DELETE);
        });
        return new TableUpdates(tableUpdateMap);
    }

    public static void createTableUpdate(final Map.Entry<UUID, Row> entry,
                                         final Map<String, TableUpdate> tableUpdateMap,
                                         final TxData txData,
                                         final DeviceData deviceData,
                                         final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> oldRowGetter,
                                         final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> newRowGetter) {
        UUID uuid = entry.getKey();
        Row oldRow = oldRowGetter.apply(deviceData, entry);
        Row newRow = newRowGetter.apply(deviceData, entry);
        String tableName = txData.getTableNameForUuid(uuid);
        tableName = tableName == null  ? deviceData.getTableNameForUuid(uuid) : tableName;
        tableUpdateMap.putIfAbsent(tableName, new TableUpdate());
        tableUpdateMap.get(tableName).addRow(uuid, oldRow, newRow);
    }
}
