/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;

public abstract class OvsdbTestBase {
    private final static String identifier = "TEST";

    public Node getTestConnection() throws IOException {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class,
                this);
        Map<ConnectionConstants, String> params = new HashMap<ConnectionConstants, String>();
        Properties props = System.getProperties();
        params.put(ConnectionConstants.ADDRESS,
                props.getProperty("ovsdbserver.ipaddress"));
        params.put(ConnectionConstants.PORT,
                props.getProperty("ovsdbserver.port", "6640"));

        Node node = connectionService.connect(identifier, params);
        if (node == null) {
            throw new IOException("Failed to connect to the ovsdb server");
        }
        return node;
    }
}
