/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.neutron.provider;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

public interface NetworkProvider {

    public static final int LLDP_PRIORITY = 1000;
    public static final int NORMAL_PRIORITY = 0;

    public boolean hasPerTenantTunneling();
    public Status handleInterfaceUpdate(String tunnelType, String tunnelKey);
    public Status handleInterfaceUpdate(NeutronNetwork network, Node source, Interface intf);
    public Status handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node source, Interface intf, boolean isLastInstanceOnNode);
    /*
     * Initialize the Flow rules given the OVSDB node.
     * This method provides a set of common functionalities to initialize the Flow rules of an OVSDB node
     * that are Openflow Version specific. Hence we have this method in addition to the following
     * Openflow Node specific initialization method.
     */
    public void initializeFlowRules(Node node);

    /*
     * Initialize the Flow rules given the Openflow node
     */
    public void initializeOFFlowRules(Node openflowNode);
}
