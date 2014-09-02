/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;

import com.google.common.collect.Lists;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;

public class ArpResponderService extends AbstractServiceInstance implements ArpProvider {
    public ArpResponderService() {
        super(Service.ARP_RESPONDER);
    }

    public ArpResponderService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    @Override
    public Status programStaticArpEntry(Node node, Long dpid, String segmentationId, String macAddress,
                                        InetAddress ipAddress, Action action) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = OF13Provider.createNodeBuilder(nodeName);

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        InstructionBuilder ib = new InstructionBuilder();

        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(Constants.ARP_ETHERTYPE));

        // Move Eth Src to Eth Dst
        InstructionUtils.applyActionIns(ActionUtils.nxMoveEthSrcToEthDstAction());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Set Eth Src
        InstructionUtils.createDlSrcInstructions(ib, new MacAddress(macAddress));
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Set ARP OP
        InstructionUtils.applyActionIns(ActionUtils.nxLoadArpOpAction(BigInteger.valueOf(0x02L)));
        ib.setOrder(2);
        ib.setKey(new InstructionKey(2));
        instructions.add(ib.build());

        // Move ARP SHA to ARP THA
        InstructionUtils.applyActionIns(ActionUtils.nxMoveArpShaToArpThaAction());
        ib.setOrder(3);
        ib.setKey(new InstructionKey(3));
        instructions.add(ib.build());

        // Move ARP SPA to ARP TPA
        InstructionUtils.applyActionIns(ActionUtils.nxMoveArpSpaToArpTpaAction());
        ib.setOrder(4);
        ib.setKey(new InstructionKey(4));
        instructions.add(ib.build());

        // Load Mac to ARP SHA
        InstructionUtils.applyActionIns(ActionUtils.nxLoadArpShaAction(new BigInteger(macAddress)));
        ib.setOrder(5);
        ib.setKey(new InstructionKey(5));
        instructions.add(ib.build());

        // Load IP to ARP SPA
        InstructionUtils.applyActionIns(ActionUtils.nxLoadArpSpaAction(ipAddress.getHostAddress()));
        ib.setOrder(6);
        ib.setKey(new InstructionKey(6));
        instructions.add(ib.build());

        // Output of InPort
        InstructionUtils.applyActionIns(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":INPORT")));
        ib.setOrder(7);
        ib.setKey(new InstructionKey(7));
        instructions.add(ib.build());

        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setInstructions(isb.setInstruction(instructions).build());

        String flowId = "ArpResponder_" + ipAddress.getHostAddress();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setPriority(1024);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);

        if (action.equals(Action.ADD)) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }
}
