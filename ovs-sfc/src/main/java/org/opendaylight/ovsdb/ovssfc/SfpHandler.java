/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfpHandler {
    private static final Logger logger = LoggerFactory.getLogger(SfpHandler.class);
    private int vlan;

    public int getVlan () {
        return vlan;
    }

    public void setVlan (int vlan) {
        this.vlan = vlan;
    }

    void processSfp (SfcEvent.Action action, ServiceFunctionPath serviceFunctionPath) {
        logger.trace("\nOVSSFC Enter: {}, action: {}\n   sfp: {}",
                Thread.currentThread().getStackTrace()[1],
                action.toString(),
                serviceFunctionPath.toString());
    }

    void processSfps (SfcEvent.Action action, ServiceFunctionPaths serviceFunctionPaths) {
        logger.trace("\nOVSSFC Enter: {}, action: {}\n   sfps: {}",
                Thread.currentThread().getStackTrace()[1],
                action.toString(),
                serviceFunctionPaths.toString());

        switch (action) {
        case CREATE:
            break;
        case UPDATE:
            break;
        case DELETE:
            break;
        default:
            break;
        }

        logger.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }
}
