/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.it;

/**
 * Constants for SouthboundIT.
 */
public interface SouthboundITConstants {
    String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";
    String CUSTOM_PROPERTIES = "etc/custom.properties";
    String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    String SERVER_PORT = "ovsdbserver.port";
    String CONNECTION_TYPE = "ovsdbserver.connection";
    String CONNECTION_TYPE_ACTIVE = "active";
    String CONNECTION_TYPE_PASSIVE = "passive";
    int CONNECTION_INIT_TIMEOUT = 10000;
    String DEFAULT_SERVER_IPADDRESS = "127.0.0.1";
    String DEFAULT_SERVER_PORT = "6640";
    String DEFAULT_OPENFLOW_PORT = "6653";
    String BRIDGE_NAME = "brtest";
    String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
}
