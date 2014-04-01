/*******************************************************************************
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Initial Commit of ITenantNetworkManager
 *******************************************************************************/
package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.Interface;

public interface ITenantNetworkManager {
    public static final String EXTERNAL_ID_VM_ID = "vm-id";
    public static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";
    public static final String EXTERNAL_ID_VM_MAC = "attached-mac";
    public int getInternalVlan(Node node, String networkId);
    public void reclaimTenantNetworkInternalVlan(Node node, String portUUID, NeutronNetwork network);
    public void networkCreated (String networkId);
    public int networkCreated (Node node, String networkId);
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId);
    public String getNetworkIdForSegmentationId (String segmentationId);
    public NeutronNetwork getTenantNetworkForInterface (Interface intf);
    public void programTenantNetworkInternalVlan(Node node, String portUUID, NeutronNetwork network);
    public void networkDeleted(String id);
}