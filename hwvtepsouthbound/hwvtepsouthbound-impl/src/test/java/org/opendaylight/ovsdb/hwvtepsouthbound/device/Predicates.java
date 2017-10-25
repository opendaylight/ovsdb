/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.device;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

public class Predicates {

    public static final String PHYSICAL_LOCATOR = "Physical_Locator";
    public static final String PHYSICAL_LOCATOR_SET = "Physical_Locator_Set";
    public static final String UUID_COLUMN_NAME = "_uuid";

    public static final Predicate<Column> IS_UUID_COLUMN = (column) -> column.getSchema().getName().equals(UUID_COLUMN_NAME);
    public static final Predicate<Column> IS_NOT_UUID_COLUMN = IS_UUID_COLUMN.negate();

    public static final Predicate<Collection> IS_UUID_COLLECTION = (collection) -> {
        return !collection.isEmpty() && collection.iterator().next() instanceof UUID;
    };
    public static final Predicate<Collection> IS_NOT_UUID_COLLECTION = IS_UUID_COLLECTION.negate();

    public static final Predicate<Map> IS_UUID_MAP = (map) -> {
        return !map.isEmpty() && map.values().iterator().next() instanceof UUID;
    };
    public static final Predicate<Map> IS_NOT_UUID_MAP = IS_UUID_MAP.negate();

    public static final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> GET_OLD_ROW_FOR_CREATE
            = (deviceData, entry) -> null;
    public static final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> GET_NEW_ROW_FOR_CREATE
            = (deviceData, entry) -> entry.getValue();
    public static final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> GET_OLD_ROW_FOR_UPDATE
            = (deviceData, entry) -> deviceData.getRowFromUuid(entry.getKey());
    public static final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> GET_NEW_ROW_FOR_UPDATE
            = (deviceData, entry) -> entry.getValue();
    public static final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> GET_OLD_ROW_FOR_DELETE
            = (deviceData, entry) -> deviceData.getRowFromUuid(entry.getKey());
    public static final BiFunction<DeviceData, Map.Entry<UUID, Row>, Row> GET_NEW_ROW_FOR_DELETE
            = (deviceData, entry) -> null;

    public static final Function<List<Condition>, UUID> GET_WHERE_UUID = (conditions) -> {
        return new UUID(conditions.iterator().next().getValue().toString());
    };
}
