/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.message;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class TableUpdate<E extends TableSchema<E>> {
    private final Map<UUID, RowUpdate<E>> rows = new HashMap<>();

    public Map<UUID, RowUpdate<E>> getRows() {
        return rows;
    }

    public static class RowUpdate<E extends TableSchema<E>> {
        private final UUID uuid;
        private Row<E> oldRow;
        private Row<E> newRow;

        public RowUpdate(final UUID uuid, final Row<E> oldRow, final Row<E> newRow) {
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

        public void setOld(final Row<E> old) {
            this.oldRow = old;
        }

        public Row<E> getNew() {
            return newRow;
        }

        @SuppressWarnings("checkstyle:HiddenField")
        public void setNew(final Row<E> newRow) {
            this.newRow = newRow;
        }

        @Override
        public String toString() {
            return "RowUpdate [uuid=" + uuid + ", oldRow=" + oldRow + ", newRow=" + newRow
                    + "]";
        }
    }

    public void addRow(final UUID uuid, final Row<E> oldRow, final Row<E> newRow) {
        rows.put(uuid, new RowUpdate<>(uuid, oldRow, newRow));
    }

    public Row<E> getOld(final UUID uuid) {
        RowUpdate<E> rowUpdate = rows.get(uuid);
        return rowUpdate != null ? rowUpdate.getOld() : null;
    }

    public Row<E> getNew(final UUID uuid) {
        RowUpdate<E> rowUpdate = rows.get(uuid);
        return rowUpdate != null ? rowUpdate.getNew() : null;
    }

    @Override
    public String toString() {
        return "TableUpdate [" + rows + "]";
    }
}
