package org.opendaylight.ovsdb.lib.table.internal;

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
