/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Maps;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;

public class TableUpdate {
    private Map<UUID, RowUpdate> rows;

    public Map<UUID, RowUpdate> getRows() {
        return rows;
    }

    public class RowUpdate {
        private UUID uuid;
        private Row oldRow;
        private Row newRow;

        public RowUpdate(UUID uuid, Row oldRow, Row newRow) {
            this.uuid = uuid;
            this.oldRow = oldRow;
            this.newRow = newRow;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public Row getOld() {
            return oldRow;
        }

        public void setOld(Row old) {
            this.oldRow = old;
        }

        public Row getNew() {
            return newRow;
        }

        @SuppressWarnings("checkstyle:HiddenField")
        public void setNew(Row newRow) {
            this.newRow = newRow;
        }

        @Override
        public String toString() {
            return "RowUpdate [uuid=" + uuid + ", oldRow=" + oldRow + ", newRow=" + newRow
                    + "]";
        }
    }

    public TableUpdate() {
        super();
        rows = Maps.newHashMap();
    }

    public void addRow(UUID uuid, Row oldRow, Row newRow) {
        rows.put(uuid, new RowUpdate(uuid, oldRow, newRow));
    }

    public Row getOld(UUID uuid) {
        RowUpdate rowUpdate = rows.get(uuid);
        if (rowUpdate == null) {
            return null;
        }
        return rowUpdate.getOld();
    }

    public Row getNew(UUID uuid) {
        RowUpdate rowUpdate = rows.get(uuid);
        if (rowUpdate == null) {
            return null;
        }
        return rowUpdate.getNew();
    }

    @Override
    public String toString() {
        return "TableUpdate [" + rows + "]";
    }
}
