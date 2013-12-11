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

import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NetFlow  extends Table<NetFlow> {

    public static final Name<NetFlow> NAME = new Name<NetFlow>("NetFlow") {};
    private OvsDBSet<String> targets;

    public OvsDBSet<String> getTargets() {
        return targets;
    }

    public void setTargets(OvsDBSet<String> targets) {
        this.targets = targets;
    }

    @Override
    @JsonIgnore
    public Name<NetFlow> getTableName() {
        return NAME;
    }

    @Override
    public String toString() {
        return "NetFlow [targets=" + targets + "]";
    }
}
