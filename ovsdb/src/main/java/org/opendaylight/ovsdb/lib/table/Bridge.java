/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.table;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Bridge extends Table<Bridge> {
    public static final Name<Bridge> NAME = new Name<Bridge>("Bridge"){};
    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Bridge>{controller, fail_mode, name, ports}

    private String name;
    private OvsDBSet<UUID> ports;
    private OvsDBSet<UUID> controller;
    private OvsDBSet<String> datapath_id;
    private String datapath_type;
    private OvsDBSet<String> fail_mode;
    private OvsDBMap<String, String> status;
    private Boolean stp_enable;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public Bridge() {
    }

    @Override
    @JsonIgnore
    public Name<Bridge> getTableName() {
        return NAME;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OvsDBSet<UUID> getPorts() {
        return ports;
    }

    public void setPorts(OvsDBSet<UUID> ports) {
        this.ports = ports;
    }

    public OvsDBSet<UUID> getController() {
        return controller;
    }

    public void setController(OvsDBSet<UUID> controller) {
        this.controller = controller;
    }

    public OvsDBSet<String> getDatapath_id() {
        return datapath_id;
    }

    public void setDatapath_id(OvsDBSet<String> datapath_id) {
        this.datapath_id = datapath_id;
    }

    public String getDatapath_type() {
        return datapath_type;
    }

    public void setDatapath_type(String datapath_type) {
        this.datapath_type = datapath_type;
    }

    public OvsDBSet<String> getFail_mode() {
        return fail_mode;
    }

    public void setFail_mode(OvsDBSet<String> fail_mode) {
        this.fail_mode = fail_mode;
    }

    public OvsDBMap<String, String> getStatus() {
        return status;
    }

    public void setStatus(OvsDBMap<String, String> status) {
        this.status = status;
    }

    public Boolean getStp_enable() {
        return stp_enable;
    }

    public void setStp_enable(Boolean stp_enable) {
        this.stp_enable = stp_enable;
    }

    public OvsDBMap<String, String> getOther_config() {
        return other_config;
    }

    public void setOther_config(OvsDBMap<String, String> other_config) {
        this.other_config = other_config;
    }

    public OvsDBMap<String, String> getExternal_ids() {
        return external_ids;
    }

    public void setExternal_ids(OvsDBMap<String, String> external_ids) {
        this.external_ids = external_ids;
    }

    @Override
    public String toString() {
        return "Bridge [name=" + name + ", ports=" + ports + ", controller="
                + controller + ", datapath_id=" + datapath_id
                + ", datapath_type=" + datapath_type + ", fail_mode="
                + fail_mode + ", status=" + status + ", stp_enable="
                + stp_enable + ", other_config=" + other_config
                + ", external_ids=" + external_ids + "]";
    }
}
