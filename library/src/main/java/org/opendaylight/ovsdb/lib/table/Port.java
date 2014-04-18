/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Madhu Venugopal, Evan Zeller
 */
package org.opendaylight.ovsdb.lib.table;

import java.math.BigInteger;

import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Port extends Table<Port> {

    public static final Name<Port> NAME = new Name<Port>("Port") {};

    private String name;
    private OvsDBSet<BigInteger> tag;
    private OvsDBSet<BigInteger> trunks;
    private OvsDBSet<UUID> interfaces;
    private OvsDBSet<String> mac;
    private OvsDBSet<UUID> qos;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public Port() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OvsDBSet<BigInteger> getTag() {
        return tag;
    }

    public void setTag(OvsDBSet<BigInteger> tag) {
        this.tag = tag;
    }

    public OvsDBSet<BigInteger> getTrunks() {
        return trunks;
    }

    public void setTrunks(OvsDBSet<BigInteger> trunks) {
        this.trunks = trunks;
    }

    public OvsDBSet<UUID> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(OvsDBSet<UUID> interfaces) {
        this.interfaces = interfaces;
    }

    public OvsDBSet<String> getMac() {
        return mac;
    }

    public void setMac(OvsDBSet<String> mac) {
        this.mac = mac;
    }

    public OvsDBSet<UUID> getQos() {
        return qos;
    }

    public void setQos(OvsDBSet<UUID> qos) {
        this.qos = qos;
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
    @JsonIgnore
    public Name<Port> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Port [name=" + name + ", tag=" + tag + ", trunks=" + trunks
                + ", interfaces=" + interfaces + ", mac=" + mac + ", qos="
                + qos + ", other_config=" + other_config + ", external_ids="
                + external_ids + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.Column<Port> {
        interfaces,
        name,
        tag,
        trunks,
        mac,
        qos,
        statistics,
        other_config,
        external_ids}
}
