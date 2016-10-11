/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="Mcast_Macs_Local", database="hardware_vtep", fromVersion="1.0.0")
public interface McastMacsLocal extends TypedBaseTable<GenericTableSchema> {
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

    @TypedColumn(name="locator_set", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, UUID> getLocatorSetColumn();

    @TypedColumn(name="locator_set", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setLocatorSet(UUID locatorSet);


    @TypedColumn(name="ipaddr", method=MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<GenericTableSchema, String> getIpAddrColumn();

    @TypedColumn(name="ipaddr", method=MethodType.GETDATA, fromVersion="1.0.0")
    String getIpAddr();

    @TypedColumn(name="ipaddr", method=MethodType.SETDATA, fromVersion="1.0.0")
    void setIpAddress(String ipAddr);
}
