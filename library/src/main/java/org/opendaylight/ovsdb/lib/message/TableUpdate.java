/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib.message;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

public class TableUpdate<E extends TableSchema<E>> {
    private UUID uuid;
    private Row<E> old;
    private Row<E> new_;

    public TableUpdate(UUID uuid) {
        super();
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Row<E> getOld() {
        return old;
    }

    public void setOld(Row<E> old) {
        this.old = old;
    }

    public Row<E> getNew() {
        return new_;
    }

    public void setNew(Row<E> new_) {
        this.new_ = new_;
    }

    @Override
    public String toString() {
        return "TableUpdate [uuid=" + uuid + ", old=" + old + ", new_=" + new_
                + "]";
    }
}
