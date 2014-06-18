/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.lib;
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
     */
    @TypedColumn(name="name", method=MethodType.GETTER)
    public Column<GenericTableSchema, String> getName();

    @TypedColumn(name="name", method=MethodType.SETTER)
    public void setName(String name);

   /*
    * Annotations are NOT added to the Status column on purpose to test the backup
    * funcationality on getter, setter, column name derivation etc.  TyperHelper.java.
    */
    public Column<GenericTableSchema, Map<String, String>> getStatus();
    public void setStatus(Map<String, String> status);

    /*
     * TypedColumn's name Annotation should override the method name based Column derivation.
     * The method name and TypedColumn name was kept different on purpose to test the name
     * resolution priority of TyperHelper.java
     */
    @TypedColumn(name="flood_vlans", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<Integer>> getFloodVlans();

    @TypedColumn(name="flood_vlans", method=MethodType.SETTER)
    public void setFloodVlans(Set<Integer> vlans);


    @TypedColumn(name="ports", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<UUID>> getPorts();

    @TypedColumn(name="ports", method=MethodType.SETTER)
    public void setPorts(Set<UUID> ports);


    @TypedColumn(name="mirrors", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<UUID>> getMirrors();

    @TypedColumn(name="mirrors", method=MethodType.SETTER)
    public void setMirrors(Set<UUID> mirrors);


    @TypedColumn(name="controller", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<UUID>> getController();

    @TypedColumn(name="controller", method=MethodType.SETTER)
    public void setController(Set<UUID> controller);


    @TypedColumn(name="datapath_id", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<String>> getDatapathId();

    @TypedColumn(name="datapath_id", method=MethodType.SETTER)
    public void setDatapathId(Set<String> datapathId);


    @TypedColumn(name="datapath_type", method=MethodType.GETTER)
    public Column<GenericTableSchema, String> getDatapathType();

    @TypedColumn(name="datapath_type", method=MethodType.SETTER)
    public void setDatapathType(String datapathType);


    @TypedColumn(name="fail_mode", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<String>> getFailMode();

    @TypedColumn(name="fail_mode", method=MethodType.SETTER)
    public void setFailMode(Set<String> failMode);


    @TypedColumn(name="sflow", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<UUID>> getSflow();

    @TypedColumn(name="sflow", method=MethodType.SETTER)
    public void setSflow(Set<UUID> sflow);


    @TypedColumn(name="netflow", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<UUID>> getNetflow();

    @TypedColumn(name="netflow", method=MethodType.SETTER)
    public void setNetflow(Set<UUID> netflow);


    @TypedColumn(name="flow_tables", method=MethodType.GETTER)
    public Column<GenericTableSchema, Map<Integer, UUID>> getFlowTables();

    @TypedColumn(name="flow_tables", method=MethodType.SETTER)
    public void setFlowTables(Map<Integer, UUID> flowTables);


    @TypedColumn(name="stp_enable", method=MethodType.GETTER)
    public Column<GenericTableSchema, Boolean> getStpEnable();

    @TypedColumn(name="stp_enable", method=MethodType.SETTER)
    public void setStpEnable(Boolean stp_enable);


    @TypedColumn(name="protocols", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<String>> getProtocols();

    @TypedColumn(name="protocols", method=MethodType.SETTER)
    public void setProtocols(Set<String> protocols);


    @TypedColumn(name="other_config", method=MethodType.GETTER)
    public Column<GenericTableSchema, Map<String, String>> getOtherConfig();

    @TypedColumn(name="other_config", method=MethodType.SETTER)
    public void setOtherConfig(Map<String, String> other_config);


    @TypedColumn(name="external_ids", method=MethodType.GETTER)
    public Column<GenericTableSchema, Map<String, String>> getExternalIds();

    @TypedColumn(name="external_ids", method=MethodType.SETTER)
    public void setExternalIds(Map<String, String> externalIds);


    @TypedColumn(name="ipfix", method=MethodType.GETTER)
    public Column<GenericTableSchema, Set<UUID>> getIpfix();

    @TypedColumn(name="ipfix", method=MethodType.SETTER)
    public void setIpfix(Set<UUID> ipfix);
}
