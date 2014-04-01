/*******************************************************************************
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Initial Commit of IInternalNetworkManager
 *******************************************************************************/
package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.Bridge;

public interface IInternalNetworkManager {
    public String getInternalBridgeUUID (Node node, String bridgeName);
    public boolean isInternalNetworkNeutronReady(Node node);
    public boolean isInternalNetworkOverlayReady(Node node);
    public boolean isInternalNetworkTunnelReady (Node node);
    public boolean isInternalNetworkVlanReady (Node node, NeutronNetwork network);
    public boolean isPortOnBridge (Node node, Bridge bridge, String portName);
    public void createIntegrationBridge (Node node) throws Exception;
    public boolean createNetNetwork (Node node, NeutronNetwork network) throws Exception;
    public boolean checkAndCreateNetwork (Node node, NeutronNetwork network);
    public void prepareInternalNetwork(Node node);
}