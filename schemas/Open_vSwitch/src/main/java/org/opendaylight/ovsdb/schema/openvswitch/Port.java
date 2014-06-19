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

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;


/*
 * Reusing the existing Table definitions and many of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */

@TypedTable(name="Port", database="Open_vSwitch")
public interface Port extends TypedBaseTable {
    @TypedColumn(name="name", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getNameColumn() ;
    @TypedColumn(name="name", method=MethodType.SETDATA)
    public void setName(String name) ;

    @TypedColumn(name="tag", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<BigInteger>> getTagColumn() ;
    @TypedColumn(name="tag", method=MethodType.SETDATA)
    public void setTag(Set<BigInteger> tag) ;

    @TypedColumn(name="trunks", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<BigInteger>> getTrunksColumn() ;
    @TypedColumn(name="trunks", method=MethodType.SETDATA)
    public void setTrunks(Set<BigInteger> trunks) ;

    @TypedColumn(name="interfaces", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getInterfacesColumn() ;
    @TypedColumn(name="interfaces", method=MethodType.SETDATA)
    public void setInterfaces(Set<UUID> interfaces) ;

    @TypedColumn(name="mac", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getMacColumn() ;
    @TypedColumn(name="mac", method=MethodType.SETDATA)
    public void setMac(Set<String> mac) ;

    @TypedColumn(name="qos", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getQosColumn() ;
    @TypedColumn(name="qos", method=MethodType.SETDATA)
    public void setQos(Set<UUID> qos) ;

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn() ;
    @TypedColumn(name="other_config", method=MethodType.SETDATA)
    public void setOtherConfig(Map<String, String> otherConfig) ;

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds) ;
}