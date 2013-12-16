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
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Interface extends Table<Interface> {

    public static Name<Interface> NAME = new Name<Interface>("Interface") {};

    private String name;
    private OvsDBMap<String, String> options;
    private String type;
    private OvsDBSet<BigInteger> ofport;
    private OvsDBSet<String> mac;
    private OvsDBMap<String, Integer> statistics;
    private OvsDBMap<String, String> status;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OvsDBMap<String, String> getOptions() {
        return options;
    }

    public void setOptions(OvsDBMap<String, String> options) {
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public OvsDBSet<BigInteger> getOfport() {
        return ofport;
    }

    public void setOfport(OvsDBSet<BigInteger> ofport) {
        this.ofport = ofport;
    }

    public OvsDBSet<String> getMac() {
        return mac;
    }

    public void setMac(OvsDBSet<String> mac) {
        this.mac = mac;
    }

    public OvsDBMap<String, Integer> getStatistics() {
        return statistics;
    }

    public void setStatistics(OvsDBMap<String, Integer> statistics) {
        this.statistics = statistics;
    }

    public OvsDBMap<String, String> getStatus() {
        return status;
    }

    public void setStatus(OvsDBMap<String, String> status) {
        this.status = status;
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
    public Name<Interface> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Interface [name=" + name + ", options=" + options + ", type="
                + type + ", ofport=" + ofport + ", mac=" + mac
                + ", statistics=" + statistics + ", status=" + status
                + ", other_config=" + other_config + ", external_ids="
                + external_ids + "]";
    }
}
