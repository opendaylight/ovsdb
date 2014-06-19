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
/*
 * Reusing the existing Table definitions and many of columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */
@TypedTable(name="SSL", database="Open_vSwitch")
public interface SSL extends TypedBaseTable {
    @TypedColumn(name="ca_cert", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getCaCertColumn() ;
    @TypedColumn(name="ca_cert", method=MethodType.SETDATA)
    public void setCaCert(String caCert) ;

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds) ;

    @TypedColumn(name="bootstrap_ca_cert", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Boolean> getBootstrapCaCertColumn() ;
    @TypedColumn(name="bootstrap_ca_cert", method=MethodType.SETDATA)
    public void setBootstrapCaCert(Boolean bootstrapCaCert) ;

    @TypedColumn(name="certificate", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getCertificateColumn() ;
    @TypedColumn(name="certificate", method=MethodType.SETDATA)
    public void setCertificate(String certificate) ;

    @TypedColumn(name="private_key", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getPrivateKeyColumn() ;
    @TypedColumn(name="private_key", method=MethodType.SETDATA)
    public void setPrivateKey(String private_key) ;
}
