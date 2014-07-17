/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Dave Tucker
 */
package org.opendaylight.ovsdb.neutron;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Queue;
import java.util.concurrent.ConcurrentMap;

public class NodeConfiguration {
    private java.util.Queue<Integer> internalVlans = Lists.newLinkedList();
    private ConcurrentMap<String, Integer> tenantVlanMap = Maps.newConcurrentMap();

    public NodeConfiguration() {
        for (int i = 1; i < Constants.MAX_VLAN; i++) {
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
