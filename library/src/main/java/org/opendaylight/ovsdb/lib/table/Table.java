/*
 * Copyright (C) 2013 Ebay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.opendaylight.ovsdb.lib.table;

public abstract class Table<E extends Table> {
    public abstract Name<E> getTableName();
    @Override
    public abstract String toString();
    public Column<E> getColumns() {
        return null;
    }

    public static abstract class Name<E extends Table> {
        String name;

        protected Name(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Table:" + name;
        }
    }
}
