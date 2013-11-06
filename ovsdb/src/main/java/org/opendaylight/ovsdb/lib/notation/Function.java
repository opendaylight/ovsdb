package org.opendaylight.ovsdb.lib.notation;

public enum Function {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">="),
    GREATER_THAN_OR_EQUALS(">="),
    INCLUDES("includes"),
    EXCLUDES("excludes");

    private Function(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}
