/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import java.util.Map;
import java.util.Set;

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

@TypedTable(name="IPFIX", database="Open_vSwitch")
public interface IPFIX extends TypedBaseTable {

    @TypedColumn(name="targets", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getTargetsColumn();
    @TypedColumn(name="targets", method=MethodType.SETDATA)
    public void setTargets(Set<String> targets);

    @TypedColumn(name="sampling", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getSamplingColumn();
    @TypedColumn(name="sampling", method=MethodType.SETDATA)
    public void setSampling(Set<Integer> sampling);

    @TypedColumn(name="obs_domain_id", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getObsDomainIdColumn();
    @TypedColumn(name="obs_domain_id", method=MethodType.SETDATA)
    public void setObsDomainId(Set<Integer> obs_domain_id);

    @TypedColumn(name="obs_point_id", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getObsOointIdColumn();
    @TypedColumn(name="obs_point_id", method=MethodType.SETDATA)
    public void setObsPointId(Set<Integer> obsPointId);

    @TypedColumn(name="cache_active_timeout", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getCacheActiveTimeoutColumn();
    @TypedColumn(name="cache_active_timeout", method=MethodType.SETDATA)
    public void setCacheActiveTimeout(Set<Integer> cacheActiveTimeout);

    @TypedColumn(name="cache_max_ﬂows", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<Integer>> getCacheMaxFowsColumn();
    @TypedColumn(name="cache_max_ﬂows", method=MethodType.SETDATA)
    public void setCacheMaxFows(Set<Integer> cacheMaxFows);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds);
}
