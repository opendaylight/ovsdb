package org.opendaylight.ovsdb.internal;

public enum Encapsulation {

    VXLAN("vxlan"), GRE("gre"), CAPWAP("capwap");

    private final String value;

    private Encapsulation(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
