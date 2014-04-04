/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.lib.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ColumnSchema {

    String name;

    @JsonProperty("type")
    private OvsdbType type;

    @JsonProperty("ephemeral")
    private Boolean ephemeral;

    @JsonProperty("mutable")
    private Boolean mutable;

    public OvsdbType getType() {
        return type;
    }

    public Boolean getEphemeral() {
        return ephemeral;
    }

    public Boolean getMutable() {
        return mutable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return "ColumnType [type=" + type + ", ephemeral=" + ephemeral
                + ", mutable=" + mutable + "]";
    }

}
