package org.opendaylight.ovsdb.lib.notation;

public enum Mutator {
    SUM("+="),
    DIFFERENCE("-="),
    PRODUCT("*="),
    QUOTIENT("/="),
    REMAINDER("%="),
    INSERT("insert"),
    DELETE("delete");

    private Mutator(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
