/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
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

/**
 * This class is a typed interface to the IPFIX Table
 */
@TypedTable(name="IPFIX", database="Open_vSwitch", fromVersion="7.1.0")
public interface IPFIX extends TypedBaseTable<GenericTableSchema> {

    @TypedColumn(name="targets", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Set<String>> getTargetsColumn();
    @TypedColumn(name="targets", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setTargets(Set<String> targets);

    @TypedColumn(name="sampling", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Set<Long>> getSamplingColumn();
    @TypedColumn(name="sampling", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setSampling(Set<Long> sampling);

    @TypedColumn(name="obs_domain_id", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Set<Long>> getObsDomainIdColumn();
    @TypedColumn(name="obs_domain_id", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setObsDomainId(Set<Long> obs_domain_id);

    @TypedColumn(name="obs_point_id", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Set<Long>> getObsPointIdColumn();
    @TypedColumn(name="obs_point_id", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setObsPointId(Set<Long> obsPointId);

    @TypedColumn(name="cache_active_timeout", method=MethodType.GETCOLUMN, fromVersion="7.3.0")
    Column<GenericTableSchema, Set<Long>> getCacheActiveTimeoutColumn();
    @TypedColumn(name="cache_active_timeout", method=MethodType.SETDATA, fromVersion="7.3.0")
    void setCacheActiveTimeout(Set<Long> cacheActiveTimeout);

    @TypedColumn(name="cache_max_flows", method=MethodType.GETCOLUMN, fromVersion="7.3.0")
    Column<GenericTableSchema, Set<Long>> getCacheMaxFlowsColumn();
    @TypedColumn(name="cache_max_flows", method=MethodType.SETDATA, fromVersion="7.3.0")
    void setCacheMaxFlows(Set<Long> cacheMaxFlows);

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN, fromVersion="7.1.0")
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();
    @TypedColumn(name="external_ids", method=MethodType.SETDATA, fromVersion="7.1.0")
    void setExternalIds(Map<String, String> externalIds);
}