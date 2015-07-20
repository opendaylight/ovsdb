/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * This class is a typed interface to the Flow_Sample_Collector_Set Table
 */
@TypedTable(name="Flow_Sample_Collector_Set", database="Open_vSwitch", fromVersion="7.1.0")
public interface FlowSampleCollectorSet extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="id", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Long> getIdColumn();

    @TypedColumn(name="id", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setId(Long id);

    @TypedColumn(name="bridge", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, UUID> getBridgeColumn();

    @TypedColumn(name="bridge", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setBridge(UUID bridge);

    @TypedColumn(name="ipfix", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, UUID> getIpfixColumn();

    @TypedColumn(name="ipfix", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setIpfix(UUID ipfix);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setExternalIds(Map<String, String> externalIds);
}
