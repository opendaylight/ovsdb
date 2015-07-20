/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Physical_Locator", database="hardware_vtep", fromVersion="1.0.0")
public interface PhysicalLocator extends TypedBaseTable<GenericTableSchema> {
    @TypedColumn(name="encapsulation_type", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getEncapsulationTypeColumn();

    @TypedColumn(name="encapsulation_type", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setEncapsulationType(String encapsulationType);

    @TypedColumn(name="dst_ip", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getDstIpColumn();

    @TypedColumn(name="dst_ip", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setDstIp(String dstIp);

    @TypedColumn(name="bfd", method=MethodType.GETCOLUMN, fromVersion="1.0.0", untilVersion="1.2.0")
    Column<GenericTableSchema, Map<String, String>> getBfdColumn();

    @TypedColumn(name="bfd", method=MethodType.SETDATA, fromVersion="1.0.0", untilVersion="1.2.0")
    void setBfd(Map<String, String> bfd);

    @TypedColumn(name="bfd_status", method=MethodType.GETCOLUMN, fromVersion="1.0.0", untilVersion="1.2.0")
    Column<GenericTableSchema, Map<String, String>> getBfdStatusColumn();

    @TypedColumn(name="bfd_status", method=MethodType.SETDATA, fromVersion="1.0.0", untilVersion="1.2.0")
    void setBfdStatus(Map<String, String> bfdStatus);
}