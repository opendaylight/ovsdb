package org.opendaylight.ovsdb.sal.connection;

/**
 * Global Constants
 *
 */
public enum ConnectionConstants {
    ADDRESS("address"),
    PORT("port"),
    PROTOCOL("protocol"),
    USERNAME("username"),
    PASSWORD("password"),
    SECURITYKEY("securitykey");

    private ConnectionConstants(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}