/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

public class MonitorSelect {

    private final boolean initial;
    private final boolean insert;
    private final boolean delete;
    private final boolean modify;

    public MonitorSelect(boolean initial, boolean insert, boolean delete, boolean modify) {
        this.initial = initial;
        this.insert = insert;
        this.delete = delete;
        this.modify = modify;
    }

    public boolean isInitial() {
        return initial;
    }

    public boolean isInsert() {
        return insert;
    }

    public boolean isDelete() {
        return delete;
    }

    public boolean isModify() {
        return modify;
    }

}
