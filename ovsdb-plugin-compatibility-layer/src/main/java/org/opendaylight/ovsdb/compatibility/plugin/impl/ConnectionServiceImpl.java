/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.ConnectionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionServiceImpl implements OvsdbConnectionService{
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionServiceImpl.class);

    org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService pluginOvsdbConnectionService;

    public void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     */
    void stopping() {
    }

    public void setOvsdbConnectionService(org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService pluginOvsdbConnectionService){
        this.pluginOvsdbConnectionService = pluginOvsdbConnectionService;
    }

    public void unsetOvsdbConnectionService(org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService pluginOvsdbConnectionService){
        if(this.pluginOvsdbConnectionService != null)
            this.pluginOvsdbConnectionService = null;
    }
    @Override
    public Connection getConnection(Node node) {
        return pluginOvsdbConnectionService.getConnection(node);
    }

    @Override
    public List<Node> getNodes() {
        return pluginOvsdbConnectionService.getNodes();
    }

    @Override
    public Node connect(String identifier, Map<ConnectionConstants, String> params) {
        return pluginOvsdbConnectionService.connect(identifier, params);
    }

}
