/*
 * Copyright (c) 2016 Inocybe Technologies and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * This class is a typed interface to the AutoAttach Table
 */
@TypedTable(name="AutoAttach", database="Open_vSwitch", fromVersion="7.11.2")
public interface AutoAttach extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="system_name", method=MethodType.GETCOLUMN, fromVersion="7.11.2")
    Column<GenericTableSchema, String> getSystemNameColumn();
    @TypedColumn(name="system_name", method=MethodType.SETDATA, fromVersion="7.11.2")
    void setSystemName(String systemName);

    @TypedColumn(name="system_description", method=MethodType.GETCOLUMN, fromVersion="7.11.2")
    Column<GenericTableSchema, String> getSystemDescriptionColumn();
    @TypedColumn(name="system_description", method=MethodType.SETDATA, fromVersion="7.11.2")
    void setSystemDescription(String systemDescription);

    @TypedColumn(name="mappings", method=MethodType.GETCOLUMN, fromVersion="7.11.2")
    Column<GenericTableSchema, Map<Long, Long>> getMappingsColumn();
    @TypedColumn(name="mappings", method=MethodType.SETDATA, fromVersion="7.11.2")
    void setMappings(Map<Long, Long> mappings);

    // FIXME: To be uncommented when Open vSwitch supports external_ids column
//    @TypedColumn (name="external_ids", method= MethodType.GETCOLUMN, fromVersion="7.11.2")
//    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
//    @TypedColumn (name="external_ids", method= MethodType.SETDATA, fromVersion="7.11.2")
//    void setExternalIds(Map<String, String> externalIds);

}
