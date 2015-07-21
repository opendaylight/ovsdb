/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * This class is a typed interface to the NetFlow Table
 */
@TypedTable(name="NetFlow", database="Open_vSwitch", fromVersion="1.0.0")
public interface NetFlow extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="targets", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<String>> getTargetsColumn();

    @TypedColumn(name="targets", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setTargets(Set<String> targets);

    @TypedColumn(name="active_timeout", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getActiveTimeoutColumn();

    @TypedColumn(name="active_timeout", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setActiveTimeout(Long activeTimeout);

    @TypedColumn(name="engine_type", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getEngineTypeColumn();

    @TypedColumn(name="engine_type", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setEngineType(Set<Long> engineType);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds);

    @TypedColumn(name="active_timeout", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getActivityTimeoutColumn();

    @TypedColumn(name="active_timeout", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setActivityTimeout(Set<Long> activityTimeout);

    @TypedColumn(name="add_id_to_interface", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Boolean>> getAddIdToInterfaceColumn();

    @TypedColumn(name="add_id_to_interface", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setAddIdToInterface(Boolean addIdToInterface);

    @TypedColumn(name="engine_id", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, Set<Long>> getEngineIdColumn();

    @TypedColumn(name="engine_id", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setEngineId(Set<Long> engineId);

}
