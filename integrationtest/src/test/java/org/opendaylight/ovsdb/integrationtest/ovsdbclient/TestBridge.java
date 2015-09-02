/*
 * Copyright (C) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.integrationtest.ovsdbclient;
import java.util.Map;
import java.util.Set;

import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodType;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedColumn;
import org.opendaylight.ovsdb.lib.schema.typed.TypedTable;

/**
 * Statically Typed Bridge Table as defined in ovs-vswitchd.conf.db
 */

/*
 * Interface name was set to TestBridge on purpose to test the @TypeTable annotation
 * functionality of TyperHelper.java
 */
@TypedTable(name="Bridge", database="Open_vSwitch")
public interface TestBridge extends TypedBaseTable {
    /*
     * Its a good practice to set the @TypedColumn to these Statically typed Tables & Columns.
     * Implementations can choose to use GETDATA or GETCOLUMN or both to get the data.
     * But GETCOLUMN gives more info on ColumnSchema.
     * The following "name" column is decorated with both GETDATA and GETCOLUMN and the corresponding test
     * will test both the options.
     */
    @TypedColumn(name="name", method=MethodType.GETDATA)
    String getName();

    @TypedColumn(name="name", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, String> getNameColumn();

    @TypedColumn(name="name", method=MethodType.SETDATA)
    void setName(String name);

   /*
    * Annotations are NOT added to the Status column on purpose to test the backup
    * functionality on getter, setter, column name derivation etc.  TyperHelper.java.
    */
   Column<GenericTableSchema, Map<String, String>> getStatusColumn();
    void setStatus(Map<String, String> status);

    /*
     * TypedColumn's name Annotation should override the method name based Column derivation.
     * The method name and TypedColumn name was kept different on purpose to test the name
     * resolution priority of TyperHelper.java
     */
    @TypedColumn(name="flood_vlans", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<Integer>> getFloodVlansColumn();

    @TypedColumn(name="flood_vlans", method=MethodType.SETDATA)
    void setFloodVlans(Set<Integer> vlans);


    @TypedColumn(name="ports", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<UUID>> getPortsColumn();

    @TypedColumn(name="ports", method=MethodType.SETDATA)
    void setPorts(Set<UUID> ports);


    @TypedColumn(name="mirrors", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<UUID>> getMirrorsColumn();

    @TypedColumn(name="mirrors", method=MethodType.SETDATA)
    void setMirrors(Set<UUID> mirrors);


    @TypedColumn(name="controller", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<UUID>> getControllerColumn();

    @TypedColumn(name="controller", method=MethodType.SETDATA)
    void setController(Set<UUID> controller);


    @TypedColumn(name="datapath_id", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<String>> getDatapathIdColumn();

    @TypedColumn(name="datapath_id", method=MethodType.SETDATA)
    void setDatapathId(Set<String> datapathId);


    @TypedColumn(name="datapath_type", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, String> getDatapathTypeColumn();

    @TypedColumn(name="datapath_type", method=MethodType.SETDATA)
    void setDatapathType(String datapathType);


    @TypedColumn(name="fail_mode", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<String>> getFailModeColumn();

    @TypedColumn(name="fail_mode", method=MethodType.SETDATA)
    void setFailMode(Set<String> failMode);


    @TypedColumn(name="sflow", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<UUID>> getSflowColumn();

    @TypedColumn(name="sflow", method=MethodType.SETDATA)
    void setSflow(Set<UUID> sflow);


    @TypedColumn(name="netflow", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<UUID>> getNetflowColumn();

    @TypedColumn(name="netflow", method=MethodType.SETDATA)
    void setNetflow(Set<UUID> netflow);


    @TypedColumn(name="flow_tables", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Map<Integer, UUID>> getFlowTablesColumn();

    @TypedColumn(name="flow_tables", method=MethodType.SETDATA)
    void setFlowTables(Map<Integer, UUID> flowTables);


    @TypedColumn(name="stp_enable", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Boolean> getStpEnableColumn();

    @TypedColumn(name="stp_enable", method=MethodType.SETDATA)
    void setStpEnable(Boolean stp_enable);


    @TypedColumn(name="protocols", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<String>> getProtocolsColumn();

    @TypedColumn(name="protocols", method=MethodType.SETDATA)
    void setProtocols(Set<String> protocols);


    @TypedColumn(name="other_config", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Map<String, String>> getOtherConfigColumn();

    @TypedColumn(name="other_config", method=MethodType.SETDATA)
    void setOtherConfig(Map<String, String> other_config);


    @TypedColumn(name="external_ids", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Map<String, String>> getExternalIdsColumn();

    @TypedColumn(name="external_ids", method=MethodType.SETDATA)
    void setExternalIds(Map<String, String> externalIds);


    @TypedColumn(name="ipfix", method=MethodType.GETCOLUMN)
    Column<GenericTableSchema, Set<UUID>> getIpfixColumn();

    @TypedColumn(name="ipfix", method=MethodType.SETDATA)
    void setIpfix(Set<UUID> ipfix);
}
