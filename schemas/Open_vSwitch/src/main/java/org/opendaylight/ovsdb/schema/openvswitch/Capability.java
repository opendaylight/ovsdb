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

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * This class is a typed interface to the Capability table
 * Note: This table is deprecated as of schema version 6.7.0
 */
@TypedTable(name="Capability", database="Open_vSwitch", fromVersion="1.0.0", untilVersion="6.7.0")
public interface Capability extends TypedBaseTable {
    @TypedColumn(name="details", method=MethodType.GETCOLUMN, fromVersion="1.0.0", untilVersion="6.7.0")
    public Column<GenericTableSchema, Map<String, String>> getDetailsColumn();

    @TypedColumn(name="details", method=MethodType.SETDATA, fromVersion="1.0.0", untilVersion="6.7.0")
    public void setDetails(Map<String, String> details);
}
