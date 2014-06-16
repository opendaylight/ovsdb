/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.sal.core.Node;

import java.net.InetAddress;
import java.util.List;

public interface IAdminConfigManager {
    public String getIntegrationBridgeName();
    public void setIntegrationBridgeName(String integrationBridgeName);
    public String getNetworkBridgeName();
    public void setNetworkBridgeName(String networkBridgeName);
    public String getPatchToNetwork();
    public void setPatchToNetwork(String patchToNetwork);
    public String getExternalBridgeName();
    public void setExternalBridgeName (String externalBridgeName);
    public String getPatchToIntegration();
    public void setPatchToIntegration(String patchToIntegration);
    public String getPhysicalInterfaceName (Node node, String physicalNetwork);
    public List<String> getAllPhysicalInterfaceNames(Node node);
    public InetAddress getTunnelEndPoint(Node node);
    public boolean isInterested (String tableName);
}