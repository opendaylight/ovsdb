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
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;


/*
 * Reusing the existing Table definitions and a few columns are not defined here
 * TODO : Fill up the missing Columns and include Supported DB Version
 */

@TypedTable(name="Open_vSwitch", database="Open_vSwitch")
public interface OpenVSwitch extends TypedBaseTable {
    @TypedColumn(name="bridges", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getBridgesColumn() ;
    @TypedColumn(name="bridges", method=MethodType.SETDATA)
    public void setBridges(Set<UUID> bridges) ;

    @TypedColumn(name="curr_cfg", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Integer> getCurr_cfgColumn() ;
    @TypedColumn(name="curr_cfg", method=MethodType.SETDATA)
    public void setCurr_cfg(Integer curr_cfg) ;

    @TypedColumn(name="db_version", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getDbVersionColumn() ;
    @TypedColumn(name="db_version", method=MethodType.SETDATA)
    public void setDbVersion(Set<String> dbVersion) ;

    @TypedColumn(name="manager_options", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getManagerOptionsColumn() ;
    @TypedColumn(name="manager_options", method=MethodType.SETDATA)
    public void setManagerOptions(Set<UUID> managerOptions) ;

    @TypedColumn(name="status", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getStatusColumn() ;
    @TypedColumn(name="status", method=MethodType.SETDATA)
    public void setStatus(Map<String, String> status) ;

    @TypedColumn(name="next_cfg", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Integer> getNextCfgColumn() ;
    @TypedColumn(name="next_cfg", method=MethodType.SETDATA)
    public void setNextCfg(Integer nextCfg) ;

    @TypedColumn(name="ovs_version", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getOvsVersionColumn() ;
    @TypedColumn(name="ovs_version", method=MethodType.SETDATA)
    public void setOvsVersion(Set<String> ovsVersion) ;

    @TypedColumn(name="ssl", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<UUID>> getSslColumn() ;
    @TypedColumn(name="ssl", method=MethodType.SETDATA)
    public void setSsl(Set<UUID> ssl) ;

    @TypedColumn(name="system_type", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getSystemTypeColumn() ;
    @TypedColumn(name="system_type", method=MethodType.SETDATA)
    public void setSystemType(Set<String> systemType) ;

    @TypedColumn(name="system_version", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Set<String>> getSystemVersionColumn() ;
    @TypedColumn(name="system_version", method=MethodType.SETDATA)
    public void setSystemVersion(Set<String> systemVersion) ;

    @TypedColumn(name="capabilities", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, UUID>> getCapabilitiesColumn() ;
    @TypedColumn(name="capabilities", method=MethodType.SETDATA)
    public void setCapabilities(Map<String, UUID> capabilities) ;

    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn() ;
    @TypedColumn(name="other_config", method=MethodType.SETDATA)
    public void setOtherConfig(Map<String, String> otherConfig) ;

    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn() ;
    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    public void setExternalIds(Map<String, String> externalIds) ;

    @TypedColumn(name="statistics", method=MethodType.GETCOLUMN)
    public Column<GenericTableSchema, Map<String, Integer>> getStatisticsColumn() ;
    @TypedColumn(name="statistics", method=MethodType.SETDATA)
    public void setStatistics(Map<String, Integer> statistics) ;
}