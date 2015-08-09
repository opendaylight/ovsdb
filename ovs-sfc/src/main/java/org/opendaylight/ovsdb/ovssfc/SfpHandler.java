/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.access.list.AccessListEntries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SfpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SfpHandler.class);
    private OvsSfcProvider ovsSfcProvider = OvsSfcProvider.getOvsSfcProvider();
    private int vlan;

    public int getVlan () {
        return vlan;
    }

    public void setVlan (int vlan) {
        this.vlan = vlan;
    }

    void processSfp (SfcEvent.Action action, ServiceFunctionPath serviceFunctionPath) {
        LOGGER.trace("\nOVSSFC Enter: {}, action: {}\n   sfp: {}",
                Thread.currentThread().getStackTrace()[1],
                action.toString(),
                serviceFunctionPath.toString());

        switch (action) {
            case CREATE:
            case UPDATE:
                sfpUpdate(serviceFunctionPath);
                break;
            case DELETE:
                break;
            default:
                break;
        }

        LOGGER.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }

    void processSfps (SfcEvent.Action action, ServiceFunctionPaths serviceFunctionPaths) {
        LOGGER.trace("\nOVSSFC Enter: {}, action: {}\n   sfps: {}",
                Thread.currentThread().getStackTrace()[1],
                action.toString(),
                serviceFunctionPaths.toString());

        switch (action) {
        case CREATE:
        case UPDATE:
            break;
        case DELETE:
            break;
        default:
            break;
        }

        LOGGER.trace("\nOVSSFC Exit: {}", Thread.currentThread().getStackTrace()[1]);
    }

    /*
     * Get the ingress ssf. This sff will take the ingress acl flows.
     * Get the acl.
     * Get the system-id from the sff to find the ovs node.
     * Program flows.
     *
     */
    private void sfpUpdate (ServiceFunctionPath serviceFunctionPath) {
        LOGGER.trace("\nOVSSFC {}\n Building SFP {}",
                Thread.currentThread().getStackTrace()[1],
                serviceFunctionPath.getName());

        // TODO: replace with correct getAccessList when the restonf issue is fixed.
        //ovsSfcProvider.aclUtils.getAccessList(serviceFunctionPath.getName());
        AccessListEntries accessListEntries = null;
        LOGGER.trace("\n   acl: {}", accessListEntries);
        // TODO: code to convert acl into flows

        String serviceFunctionForwarderName;
        ServiceFunctionForwarder serviceFunctionForwarder = null;
        Short startingIndex = serviceFunctionPath.getStartingIndex();
        List<ServicePathHop> servicePathHopList = serviceFunctionPath.getServicePathHop();
        for (ServicePathHop servicePathHop : servicePathHopList) {
            LOGGER.trace("\n   sph: {}", servicePathHop);

            serviceFunctionForwarderName = servicePathHop.getServiceFunctionForwarder();
            serviceFunctionForwarder = ovsSfcProvider.sffUtils.readServiceFunctionForwarder(serviceFunctionForwarderName);
            if (serviceFunctionForwarder != null) {
                String systemId = ovsSfcProvider.sffUtils.getSystemId(serviceFunctionForwarder);
                Node ovsNode = ovsSfcProvider.ovsUtils.getNodeFromSystemId(systemId);
                if (ovsNode != null) {
                    Long dpid = ovsSfcProvider.ovsUtils.getDpid(ovsNode, serviceFunctionForwarderName);
                    if (dpid.equals(0)) {
                        LOGGER.warn("cannot find dpid for {}", serviceFunctionForwarderName);
                        continue;
                    }
                    if (servicePathHop.getServiceIndex().equals(startingIndex)) {
                        ovsSfcProvider.flows.initializeFlowRules(dpid);
                        // Add ingress classifier rule
                        ovsSfcProvider.flows.writeIngressAcl(dpid, 0L, 0, true);
                        // Add vlan t0 rule
                    }
                    // Add t30 classifier reg rule
                    // Add t31 nextHop rule
                }
            }
        }
    }

}
