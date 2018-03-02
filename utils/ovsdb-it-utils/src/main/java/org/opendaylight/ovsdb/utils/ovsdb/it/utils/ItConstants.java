/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.ovsdb.it.utils;

/**
 * Constants for SouthboundIT.
 */
public interface ItConstants {
    String ORG_OPS4J_PAX_LOGGING_CFG = "etc/org.ops4j.pax.logging.cfg";
    String CUSTOM_PROPERTIES = "etc/custom.properties";
    String SERVER_IPADDRESS = "ovsdbserver.ipaddress";
    String SERVER_PORT = "ovsdbserver.port";
    String CONTROLLER_IPADDRESS = "ovsdb.controller.address";
    String USERSPACE_ENABLED = "ovsdb.userspace.enabled";
    String SERVER_EXTRAS = "ovsdbserver.extras";
    String CONNECTION_TYPE = "ovsdbserver.connection";
    String CONNECTION_TYPE_ACTIVE = "active";
    String CONNECTION_TYPE_PASSIVE = "passive";
    int CONNECTION_INIT_TIMEOUT = 10000;
    String DEFAULT_SERVER_IPADDRESS = "127.0.0.1";
    String DEFAULT_SERVER_PORT = "6640";
    String DEFAULT_OPENFLOW_PORT = "6653";
    String DEFAULT_SERVER_EXTRAS = "false";
    String BRIDGE_NAME = "brtest";
    String PORT_NAME = "porttest";
    String INTEGRATION_BRIDGE_NAME = "br-int";
    String OPENFLOW_CONNECTION_PROTOCOL = "tcp";
    String NETVIRT_TOPOLOGY_ID = "netvirt:1";
    String DOCKER_COMPOSE_FILE_NAME = "docker.compose.file";
    String DOCKER_RUN = "docker.run";
    String DOCKER_VENV_WS = "docker.vEnvWs";
    String DOCKER_WAIT_FOR_PING_SECS = "docker.ping.wait.secs";
}
