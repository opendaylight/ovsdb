/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import java.util.Map;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.google.common.collect.Maps;

public class TableUpdate<E extends TableSchema<E>> {
    private Map<UUID, RowUpdate<E>> rows;

    public Map<UUID, RowUpdate<E>> getRows() {
        return rows;
    }

    public class RowUpdate<E extends TableSchema<E>> {
        private UUID uuid;
        private Row<E> oldRow;
        private Row<E> newRow;

        public RowUpdate(UUID uuid, Row<E> oldRow, Row<E> newRow) {
            this.uuid = uuid;
            this.oldRow = oldRow;
            this.newRow = newRow;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public Row<E> getOld() {
            return oldRow;
        }

        public void setOld(Row<E> oldRow) {
            this.oldRow = oldRow;
        }

        public Row<E> getNew() {
            return newRow;
        }

        public void setNew(Row<E> newRow) {
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

    public void addRow(UUID uuid, Row<E> oldRow, Row<E> newRow) {
        rows.put(uuid, new RowUpdate<E>(uuid, oldRow, newRow));
    }

    public Row<E> getOld(UUID uuid) {
        RowUpdate<E> rowUpdate = rows.get(uuid);
        if (rowUpdate == null) {
            return null;
        }
        return rowUpdate.getOld();
    }

    public Row<E> getNew(UUID uuid) {
        RowUpdate<E> rowUpdate = rows.get(uuid);
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
