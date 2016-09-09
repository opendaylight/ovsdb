/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

import java.util.Map;

/**
 * This class is a typed interface to the SSL Table
 */
@TypedTable (name="SSL", database="Open_vSwitch", fromVersion="1.0.0")
public interface SSL extends TypedBaseTable {

    @TypedColumn (name="ca_cert", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<String> getCaCertColumn() ;

    @TypedColumn (name="ca_cert", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setCaCert(String caCert) ;

    @TypedColumn (name="external_ids", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<Map<String, String>> getExternalIdsColumn() ;

    @TypedColumn (name="external_ids", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setExternalIds(Map<String, String> externalIds) ;

    @TypedColumn (name="bootstrap_ca_cert", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<Boolean> getBootstrapCaCertColumn() ;

    @TypedColumn (name="bootstrap_ca_cert", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setBootstrapCaCert(Boolean bootstrapCaCert) ;

    @TypedColumn (name="certificate", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<String> getCertificateColumn() ;

    @TypedColumn (name="certificate", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setCertificate(String certificate) ;

    @TypedColumn (name="private_key", method= MethodType.GETCOLUMN, fromVersion="1.0.0")
    Column<String> getPrivateKeyColumn() ;

    @TypedColumn (name="private_key", method= MethodType.SETDATA, fromVersion="1.0.0")
    void setPrivateKey(String private_key) ;
}