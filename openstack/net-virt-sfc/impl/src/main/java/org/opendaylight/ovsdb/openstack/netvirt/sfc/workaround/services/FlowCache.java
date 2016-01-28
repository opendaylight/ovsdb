/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.workaround.services;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCache {
    private static final Logger LOG = LoggerFactory.getLogger(FlowCache.class);
    private Map<String, Map<Integer, InstanceIdentifier<Flow>>> flowCache = new HashMap<>();

    public void addFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder, String rspName, int flowId) {
        Map<Integer, InstanceIdentifier<Flow>> flowMap = flowCache.get(rspName);
        if (flowMap == null) {
            LOG.info("addFlow: adding new flowMap for {}({})", rspName, flowId);
            flowMap = new HashMap<>();
        }
        InstanceIdentifier<Flow> path = FlowUtils.createFlowPath(flowBuilder, nodeBuilder);
        flowMap.put(flowId, path);
        flowCache.put(rspName, flowMap);
        LOG.info("addFlow: added {}({}) {} to cache size {} - {}", rspName, flowId, path,
                flowCache.size(), flowCache);
    }

    public void removeFlow(String rspName, int flowId) {
        Map<Integer, InstanceIdentifier<Flow>> flowMap = flowCache.get(rspName);
        if (flowMap != null) {
            flowMap.remove(flowId);
            if (flowMap.isEmpty()) {
                flowCache.remove(rspName);
                LOG.info("removeFlow: removed flowMap {}({}) from cache size {} - {}", rspName, flowId,
                        flowCache.size(), flowCache);
            } else {
                flowCache.put(rspName, flowMap);
            }
        }
        LOG.info("removeFlow: removed {}({}) from cache size {} - {}", rspName, flowId, flowCache.size(), flowCache);
    }

    public Map<Integer, InstanceIdentifier<Flow>> getFlows(String rspName) {
        return flowCache.get(rspName);
    }
}
