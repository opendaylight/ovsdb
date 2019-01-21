/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.impl;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionServiceConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionServiceConfigurator.class);

    private static final String JSON_RPC_DECODER_MAX_FRAME_LENGTH_PARAM = "json-rpc-decoder-max-frame-length";
    private static final String USE_SSL_PARAM = "use-ssl";
    private static final String OVSDB_RPC_TASK_TIMEOUT_PARAM = "ovsdb-rpc-task-timeout";
    private static final String OVSDB_LISTENER_PORT_PARAM = "ovsdb-listener-port";
    private final OvsdbConnectionService ovsdbconnection;

    public OvsdbConnectionServiceConfigurator(OvsdbConnectionService ovsdbconnection) {
        this.ovsdbconnection = ovsdbconnection;
    }

    public void setOvsdbRpcTaskTimeout(int timeout) {
        ovsdbconnection.setOvsdbRpcTaskTimeout(timeout);
    }

    public void setUseSsl(boolean flag) {
        ovsdbconnection.setUseSsl(flag);
    }

    public void setJsonRpcDecoderMaxFrameLength(int maxFrameLength) {
        ovsdbconnection.setJsonRpcDecoderMaxFrameLength(maxFrameLength);
    }

    public void setOvsdbListenerIp(String ip) {
        ovsdbconnection.setOvsdbListenerIp(ip);
    }

    public void setOvsdbListenerPort(int portNumber) {
        ovsdbconnection.setOvsdbListenerPort(portNumber);
    }

    public void updateConfigParameter(Map<String, Object> configParameters) {
        if (configParameters != null && !configParameters.isEmpty()) {
            LOG.debug("Config parameters received : {}", configParameters.entrySet());
            for (Map.Entry<String, Object> paramEntry : configParameters.entrySet()) {
                if (paramEntry.getKey().equalsIgnoreCase(OVSDB_RPC_TASK_TIMEOUT_PARAM)) {
                    ovsdbconnection.setOvsdbRpcTaskTimeout(Integer.parseInt((String) paramEntry.getValue()));
                } else if (paramEntry.getKey().equalsIgnoreCase(USE_SSL_PARAM)) {
                    ovsdbconnection.setUseSsl(Boolean.parseBoolean(paramEntry.getValue().toString()));
                }

            }
        }
    }
}

