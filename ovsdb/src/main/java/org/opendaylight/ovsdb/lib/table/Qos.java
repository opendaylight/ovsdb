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


import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Qos extends Table<Qos> {

    public static final Name<Qos> NAME = new Name<Qos>("QoS") {};

    private OvsDBMap<Integer, UUID> queues;
    private String type;
    private OvsDBMap<String, String> other_config;
    private OvsDBMap<String, String> external_ids;

    public Qos() {
    }

    public OvsDBMap<Integer, UUID> getQueues() {
        return queues;
    }

    public void setQueues(OvsDBMap<Integer, UUID> queues) {
        this.queues = queues;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
    public Name<Qos> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "Qos [queues=" + queues + ", type=" + type + ", other_config="
                + other_config + ", external_ids=" + external_ids + "]";
    }

    public enum Column implements org.opendaylight.ovsdb.lib.table.internal.Column<Qos> {
        queues,
        type,
        other_config,
        external_ids}
}
