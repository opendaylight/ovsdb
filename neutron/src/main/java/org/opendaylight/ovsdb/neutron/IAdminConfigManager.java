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
import java.net.InetAddress;

public interface IAdminConfigManager {
    public String getIntegrationBridgeName();
    public void setIntegrationBridgeName(String integrationBridgeName);
    public String getTunnelBridgeName();
    public void setTunnelBridgeName(String tunnelBridgeName);
    public String getExternalBridgeName();
    public void setExternalBridgeName (String externalBridgeName);
    public String getPatchToIntegration();
    public void setPatchToIntegration(String patchToIntegration);
    public String getPatchToTunnel();
    public void setPatchToTunnel(String patchToTunnel);
    public InetAddress getTunnelEndPoint(Node node);
    public boolean isInterested (String tableName);
}