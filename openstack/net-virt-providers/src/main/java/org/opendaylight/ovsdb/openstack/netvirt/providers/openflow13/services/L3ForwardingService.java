/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class L3ForwardingService extends AbstractServiceInstance implements L3ForwardingProvider, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(L3ForwardingService.class);

    public L3ForwardingService() {
        super(Service.L3_FORWARDING);
    }

    public L3ForwardingService(Service service) {
        super(service);
    }

    @Override
    public Status programForwardingTableEntry(Long dpid, String segmentationId, InetAddress ipAddress,
                                              String macAddress, Action action) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = OF13Provider.createNodeBuilder(nodeName);

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        InstructionBuilder ib = new InstructionBuilder();

        if (ipAddress instanceof Inet6Address) {
            // WORKAROUND: For now ipv6 is not supported
            // TODO: implement ipv6 case
            LOG.debug("ipv6 address is not implemented yet. dpid {} segmentationId {} ipAddress {} macAddress {} Action {}",
                      dpid, segmentationId, ipAddress, macAddress, action);
            return new Status(StatusCode.NOTIMPLEMENTED);
        }

        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createDstL3IPv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(ipAddress.getHostAddress()));

        // Set Dest Mac address
        InstructionUtils.createDlDstInstructions(ib, new MacAddress(macAddress));
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Goto Next Table
        ib = getMutablePipelineInstructionBuilder();
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setInstructions(isb.setInstruction(instructions).build());

        String flowId = "L3Forwarding_" + segmentationId + "_" + ipAddress.getHostAddress();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setPriority(1024);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (action.equals(Action.ADD)) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(L3ForwardingProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
