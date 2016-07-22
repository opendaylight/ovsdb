/*
 * Copyright (c) 2016 Red Hat and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.message;

public class MonitorSelectBuilder {

    private boolean initial;
    private boolean insert;
    private boolean delete;
    private boolean modify;

    public boolean isInitial() {
        return initial;
    }

    public MonitorSelectBuilder setInitial(boolean initial) {
        this.initial = initial;
        return this;
    }

    public boolean isInsert() {
        return insert;
    }

    public MonitorSelectBuilder setInsert(boolean insert) {
        this.insert = insert;
        return this;
    }

    public boolean isDelete() {
        return delete;
    }

    public MonitorSelectBuilder setDelete(boolean delete) {
        this.delete = delete;
        return this;
    }

    public boolean isModify() {
        return modify;
    }

    public MonitorSelectBuilder setModify(boolean modify) {
        this.modify = modify;
        return this;
    }

    public MonitorSelect build() {
        return new MonitorSelect(initial, insert, delete, modify);
    }
}
