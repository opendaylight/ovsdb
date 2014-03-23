/*******************************************************************************
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Initial Commit of IAdminConfigManager
 *******************************************************************************/
package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.sal.core.Node;

public interface IInternalNetworkManager {
    public String getInternalBridgeUUID (Node node, String bridgeName);
    public boolean isInternalNetworkNeutronReady(Node node);
    public boolean isInternalNetworkOverlayReady(Node node);
    public void createInternalNetworkForOverlay(Node node) throws Exception;
    public void createInternalNetworkForNeutron(Node node) throws Exception;
    public void prepareInternalNetwork(Node node);
}