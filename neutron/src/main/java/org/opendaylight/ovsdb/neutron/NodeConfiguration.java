/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Replace tenantVlanMap with a per Node cache
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

public class NodeConfiguration {
    private static final int MAX_VLAN = 4096;
    private java.util.Queue<Integer> internalVlans = new LinkedList<>();
    private ConcurrentMap<String, Integer> tenantVlanMap = new ConcurrentHashMap<>();

    public NodeConfiguration() {
        for (int i = 1; i < MAX_VLAN ; i++) {
            internalVlans.add(i);
        }
    }

    public int assignInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) return mappedVlan;
        mappedVlan = internalVlans.poll();
        if (mappedVlan != null) tenantVlanMap.put(networkId, mappedVlan);
        return mappedVlan;
    }

    public void internalVlanInUse (int vlan) {
        internalVlans.remove(vlan);
    }

    public int getInternalVlan (String networkId) {
        Integer vlan = tenantVlanMap.get(networkId);
        if (vlan == null) return 0;
        return vlan.intValue();
    }

}
