/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.hardwarevtep;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

@TypedTable(name="ACL", database="hardware_vtep", fromVersion="1.4.0")
public interface ACL extends TypedBaseTable {
    @TypedColumn(name="acl_name", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<String> getAclNameColumn();

    @TypedColumn(name="acl_name", method=MethodType.GETDATA, fromVersion="1.4.0")
    String getAclName();

    @TypedColumn(name="acl_name", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAclName(String aclName);


    @TypedColumn(name="acl_entries", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<UUID> getAclEntriesColumn();

    @TypedColumn(name="acl_entries", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAclEntry(UUID aclEntry);


    @TypedColumn(name="acl_fault_status", method=MethodType.GETCOLUMN, fromVersion="1.4.0")
    Column<Map<String, String>> getAclFaultStatusColumn();

    @TypedColumn(name="acl_fault_status", method=MethodType.SETDATA, fromVersion="1.4.0")
    void setAclFaultStatus(Map<String, String> aclFaultStatus);
}
