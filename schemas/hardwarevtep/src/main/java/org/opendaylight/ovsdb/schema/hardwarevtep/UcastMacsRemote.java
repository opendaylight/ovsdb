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

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Ucast_Macs_Remote", database="hardware_vtep", fromVersion="1.0.0")
public interface UcastMacsRemote extends TypedBaseTable<GenericTableSchema> {
    @TypedColumn(name="MAC", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getMacColumn();

    @TypedColumn(name="MAC", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getMac();

    @TypedColumn(name="MAC", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setMac(String mac);


    @TypedColumn(name="logical_switch", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, UUID> getLogicalSwitchColumn();

    @TypedColumn(name="logical_switch", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setLogicalSwitch(UUID logicalSwitch);

    @TypedColumn(name="locator", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, UUID> getLocatorColumn();

    @TypedColumn(name="locator", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setLocator(UUID locator);


    @TypedColumn(name="ipaddr", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getIpAddrColumn();

    @TypedColumn(name="ipaddr", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getIpAddr();

    @TypedColumn(name="ipaddr", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setIpAddress(String ipAddr);
}
