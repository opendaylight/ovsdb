package org.opendaylight.ovsdb.neutron;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NodeConfiguration {

    private static final int MAX_VLAN = 4096;
    private java.util.Queue<Integer> internalVlans = new LinkedList<>();
    private ConcurrentMap<String, Integer> tenantVlanMap = new ConcurrentHashMap<>();

    public NodeConfiguration() {
        for (int i = 1; i < MAX_VLAN; i++) {
            internalVlans.add(i);
        }
    }

    public Queue<Integer> getInternalVlans() {
        return internalVlans;
    }

    public ConcurrentMap<String, Integer> getTenantVlanMap() {
        return tenantVlanMap;
    }

}
