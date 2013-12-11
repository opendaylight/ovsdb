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
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Queue extends Table<Queue> {

    public static final Name<Queue> NAME = new Name<Queue>("Queue") {};

    private OvsDBSet<Integer> dscp;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public Queue() {
    }

    public OvsDBSet<Integer> getDscp() {
        return dscp;
    }

    public void setDscp(OvsDBSet<Integer> dscp) {
        this.dscp = dscp;
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
    public Name<Queue> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Queue [dscp=" + dscp + ", other_config=" + other_config
                + ", external_ids=" + external_ids + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Queue> {
        dscp,
        other_config,
        external_ids}
}
