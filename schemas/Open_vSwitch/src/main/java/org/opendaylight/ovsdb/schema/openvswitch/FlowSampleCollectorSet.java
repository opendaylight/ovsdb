/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/*
 * Reusing the existing Table definitions and many of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */
@TypedTable(name="Flow_Sample_Collector_Set", database="Open_vSwitch")
public interface FlowSampleCollectorSet extends TypedBaseTable<GenericTableSchema> {
    @TypedColumn(name="id", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Integer> getIdColumn();
    @TypedColumn(name="id", method=MethodType.SETDATA)
    public void setId(Integer id);

    @TypedColumn(name="bridge", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Bridge> getBridgeColumn();
    @TypedColumn(name="bridge", method=MethodType.SETDATA)
    public void setBridge(Bridge bridge);

    @TypedColumn(name="ipfix", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, IPFIX> getIpfixColumn();
    @TypedColumn(name="ipfix", method=MethodType.SETDATA)
    public void setIpfix(IPFIX ipfix);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds);
}
