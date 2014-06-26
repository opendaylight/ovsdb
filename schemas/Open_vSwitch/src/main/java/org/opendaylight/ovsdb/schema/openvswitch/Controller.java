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

@TypedTable(name="Controller", database="Open_vSwitch")
public interface Controller extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="target", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, String> getTargetColumn();

    @TypedColumn(name="target", method=MethodType.SETDATA)
    public void setTarget(String target);

    @TypedColumn(name="controller_burst_limit", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Integer> getBurstLimitColumn();

    @TypedColumn(name="controller_burst_limit", method=MethodType.SETDATA)
    public void setBurstLimit(Integer burstLimit);

    @TypedColumn(name="controller_rate_limit", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Integer> getRateLimitColumn();

    @TypedColumn(name="controller_rate_limit", method=MethodType.SETDATA)
    public void setRateLimit(Integer burstLimit);
}