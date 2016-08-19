/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HAStateHelper {

    static Logger LOG = LoggerFactory.getLogger(HAStateHelper.class);

    static HAStateHandler nonHaStateHandler = new HAStateHandler() {
        @Override
        public void handle(HAContext config, ReadWriteTransaction tx) {
        }
    };

    static Map<HAState, HAStateHandler> stateHandlers = new HashMap<>();

    static {

        stateHandlers.put(HAState.D1Connected, new D1ConnectedHandler());
        stateHandlers.put(HAState.D1ReConnected, new D1ReConnectedHandler());
        stateHandlers.put(HAState.D2Connected, new D2ConnectedHandler());
        stateHandlers.put(HAState.D2Reconnected, new D2ReConnectedHandler());
        stateHandlers.put(HAState.D1Disconnected, new D1DisConnectedHandler());

        stateHandlers.put(HAState.NonHA, nonHaStateHandler);
    }

    public static HAStateHandler getStateHandler(HAContext haContext) throws Exception {
        HAStateHandler haStateHandler = stateHandlers.get(haContext.getHaState());
        if (haStateHandler == null) {
            return nonHaStateHandler;
        }
        return haStateHandler;
    }
}
