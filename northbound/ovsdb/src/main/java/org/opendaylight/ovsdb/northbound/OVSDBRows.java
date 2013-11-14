package org.opendaylight.ovsdb.northbound;

import java.util.Map;

import org.opendaylight.ovsdb.lib.table.internal.Table;

public class OVSDBRows {
    Map<String, Table<?>> rows;

    public OVSDBRows(Map<String, Table<?>> rows) {
        super();
        this.rows = rows;
    }

    public Map<String, Table<?>> getRows() {
        return rows;
    }

    public void setRows(Map<String, Table<?>> rows) {
        this.rows = rows;
    }
}
