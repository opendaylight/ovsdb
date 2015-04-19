/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

/**
 * Constants for SouthboundIT
 */
public final class SouthboundITConstants {
    private SouthboundITConstants() {
        throw new AssertionError("This class should not be instantiated.");
    }

    public static final String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";
    public static final String CUSTOM_PROPERTIES = "etc/custom.properties";
    public static final String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    public static final String SERVER_PORT = "ovsdbserver.port";
    public static final String SERVER_EXTRAS = "ovsdbserver.extras";
    public static final String CONNECTION_TYPE = "ovsdbserver.connection";
    public static final String CONNECTION_TYPE_ACTIVE = "active";
    public static final String CONNECTION_TYPE_PASSIVE = "passive";
    public static final int CONNECTION_INIT_TIMEOUT = 10000;
    public static final String DEFAULT_SERVER_IPADDRESS = "127.0.0.1";
    public static final String DEFAULT_SERVER_PORT = "6640";
    public static final String DEFAULT_SERVER_EXTRAS = "false";
    public static final String BRIDGE_NAME = "brtest";
}
