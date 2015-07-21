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

@TypedTable(name="Arp_Sources_Local", database="hardware_vtep", fromVersion="1.2.0")
public interface ArpSourcesLocal extends TypedBaseTable<GenericTableSchema> {
    @TypedColumn(name="src_mac", method=MethodType.GETCOLUMN, fromVersion="1.2.0")
    Column<GenericTableSchema, String> getSrcMacColumn();

    @TypedColumn(name="src_mac", method=MethodType.GETDATA, fromVersion="1.2.0")
    String getSrcMac();

    @TypedColumn(name="src_mac", method=MethodType.SETDATA, fromVersion="1.2.0")
    void setSrcMac(String srcMac);

    @TypedColumn(name="locator", method=MethodType.GETCOLUMN, fromVersion="1.2.0")
    Column<GenericTableSchema, UUID> getLocatorColumn();

    @TypedColumn(name="locator", method=MethodType.SETDATA, fromVersion="1.2.0")
    void setLocator(UUID locator);
}
