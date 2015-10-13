/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NetvirtSfcAclListener;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.sfc.sfc_ovs.provider.SfcOvsUtil;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.AccessListEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev141010.access.lists.access.list.access.list.entries.access.list.entry.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.classifier.rev150105.classifiers.classifier.sffs.Sff;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for Netvirt SFC
 * @author Arun Yerra
 */
public class NetvirtSfcOF13Provider implements INetvirtSfcOF13Provider{
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcOF13Provider.class);
    private static final int DEFAULT_FLOW_PRIORITY = 32768;
    //private final DataBroker dataService;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile Southbound southbound;
    private MdsalUtils dbutils;
    private PipelineOrchestrator orchestrator;

    /**
     * {@link NetvirtSfcOF13Provider} constructor.
     * @param bundleContext Bundle Context, used to access NetVirt Pipeline orchestrator.
     * @param dataBroker MdSal {@link DataBroker}
     */
    public NetvirtSfcOF13Provider(final DataBroker dataBroker, BundleContext bundleContext) {
        Preconditions.checkNotNull(dataBroker, "Input dataBroker cannot be NULL!");
        Preconditions.checkNotNull(bundleContext, "Input bundleContext cannot be NULL!");

        //this.dataService = dataBroker;
        dbutils = new MdsalUtils(dataBroker);
        orchestrator = bundleContext.getService(bundleContext.getServiceReference(PipelineOrchestrator.class));
        this.setDependencies(bundleContext);
    }

    @Override
    public void addClassifierRules(Sff sff, AccessList acl) {
        Preconditions.checkNotNull(sff, "Input service function forwarder cannot be NULL!");
        Preconditions.checkNotNull(acl, "Input accesslist cannot be NULL!");

        // Validate if any service function forwarder exists by the name, using SFC provider APIs.
        ServiceFunctionForwarder serviceForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarderExecutor(sff.getName());
        if (serviceForwarder == null) {
            LOG.debug("Service Function Forwarder = {} not yet configured. Skip processing !!", sff.getName());
            return;
        }

        // If a service function forwarder exists, then get the corresponding OVS Bridge details and Openflow NodeId.
        // If OVS Bridge augmentation is configured, the following API returns NULL.
        String datapathId = SfcOvsUtil.getOpenFlowNodeIdForSff(serviceForwarder);
        if (datapathId == null) {
            LOG.debug("Service Function Forwarder = {} is not augemented with "
                    + "OVS Bridge Information. Skip processing!!", sff.getName());
        }
        // If openflow Node Id is NULL, get all the bridge nodes using southbound apis and fetch
        // SFF with matching name. From this bridge name, get the openflow data path ID.
        if (datapathId == null) {
            Node node = null;
            final List<Node> nodes = nodeCacheManager.getBridgeNodes();
            if (nodes.isEmpty()) {
                LOG.debug("Noop with Classifier Creation on SFF={}. No Bridges configured YET!!", sff.getName());
            } else {
                for (Node dstNode : nodes) {
                    LOG.debug("Processing Node={}, sff={}", dstNode.getNodeId().getValue(), sff.getName());
                    if (dstNode.getNodeId().getValue().equalsIgnoreCase(sff.getName())) {
                        LOG.debug("Found matching OVSDB Bridge Name!!= {}", dstNode.getNodeId().getValue());
                        node = dstNode;
                        break;
                    }
                }
            }
        }

        LOG.debug("Processing the Classifier rules on Node={}", datapathId);
        if (datapathId != null) {
            // Program the OF flow on the corresponding open flow node.
            Iterator<AccessListEntry> itr = acl.getAccessListEntries().getAccessListEntry().iterator();
            while (itr.hasNext()) {
                AccessListEntry entry = itr.next();
                programOFRules(entry, datapathId, true);
            }
        }
    }

    private void programOFRules(AccessListEntry entry, String datapathId, boolean write) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(Constants.OPENFLOW_NODE_PREFIX + datapathId));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        //Create the match using match builder, by parsing the Accesslist Entry Match.
        MatchBuilder matchBuilder = null;
        matchBuilder = buildMatch(entry.getRuleName(), entry.getMatches(), datapathId);

        InstructionsBuilder isb = null;
        isb = buildActions(entry.getRuleName(), entry.getActions(), datapathId);

        String flowId = "NETVIRT_SFC_FLOW" + "_" + entry.getRuleName();

        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        flowBuilder.setInstructions(isb.build());

        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private InstructionsBuilder buildActions(String ruleName, Actions actions, String datapathId) {
        InstructionBuilder ib = new InstructionBuilder();

        if (actions.getPacketHandling() instanceof Deny) {
            InstructionUtils.createDropInstructions(ib);
        } else if (actions.getPacketHandling() instanceof Permit) {
            //Permit actPermit = (Permit) actions.getPacketHandling();
        } else {
            InstructionUtils.createDropInstructions(ib);
        }

        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();
        instructions.add(ib.build());

        // Call the InstructionBuilder Methods Containing Actions
        ib = this.getMutablePipelineInstructionBuilder();
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        InstructionsBuilder isb = new InstructionsBuilder();
        isb.setInstruction(instructions);
        return isb;
    }

    private MatchBuilder buildMatch(String ruleName, Matches matches, String dpId) {
        MatchBuilder matchBuilder = new MatchBuilder();

        if (matches.getAceType() instanceof AceIp) {
            AceIp aceIp = (AceIp)matches.getAceType();
            if (aceIp.getAceIpVersion() instanceof AceIpv4) {
                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                MatchUtils.createSrcL3IPv4Match(matchBuilder, aceIpv4.getSourceIpv4Address());
                MatchUtils.createDstL3IPv4Match(matchBuilder, aceIpv4.getDestinationIpv4Address());
                MatchUtils.createIpProtocolMatch(matchBuilder, aceIp.getIpProtocol());
                MatchUtils.addLayer4Match(matchBuilder, aceIp.getIpProtocol().intValue(),
                        aceIp.getSourcePortRange().getLowerPort().getValue().intValue(),
                        aceIp.getDestinationPortRange().getLowerPort().getValue().intValue());
            }
        } else if (matches.getAceType() instanceof AceEth) {
            AceEth aceEth = (AceEth) matches.getAceType();
            MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(aceEth.getSourceMacAddress().getValue()));
            MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(aceEth.getDestinationMacAddress().getValue()),
                    new MacAddress(aceEth.getDestinationMacAddressMask().getValue()));
        }

        //MatchUtils.createInPortMatch(matchBuilder, Long.getLong(dpId), Long.getLong(matches.getInputInterface()));
        return matchBuilder;
    }

    @Override
    public void removeClassifierRules(Sff sff, AccessList acl) {
        // TODO Auto-generated method stub

    }


    protected void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        LOG.debug("writeFlow: flowBuilder: {}, nodeBuilder: {}",
                flowBuilder.build(), nodeBuilder.build());
        dbutils.merge(LogicalDatastoreType.CONFIGURATION, createNodePath(nodeBuilder), nodeBuilder.build());
        dbutils.put(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder), flowBuilder.build());
    }

    protected void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        dbutils.delete(LogicalDatastoreType.CONFIGURATION, createFlowPath(flowBuilder, nodeBuilder));
    }

    private String getDpid(Node node) {
        long dpid = southbound.getDataPathId(node);
        if (dpid == 0) {
            LOG.warn("getDpid: DPID could not be found for node: {}", node.getNodeId().getValue());
        }
        return String.valueOf(dpid);
    }

    private static InstanceIdentifier<Flow> createFlowPath(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey()).build();
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
        createNodePath(NodeBuilder nodeBuilder) {
        return InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        nodeBuilder.getKey()).build();
    }

    private short getTable() {
        return Service.INGRESS_ACL.getTable();
    }

    private final InstructionBuilder getMutablePipelineInstructionBuilder() {
        Service nextService = orchestrator.getNextServiceInPipeline(Service.INGRESS_ACL);
        if (nextService != null) {
            return InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), nextService.getTable());
        } else {
            return InstructionUtils.createDropInstructions(new InstructionBuilder());
        }
    }

    private void setDependencies(BundleContext bundleContext) {
        //super.setDependencies(bundleContext.getServiceReference(INetvirtSfcOF13Provider.class.getName()), this);
        nodeCacheManager = (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        southbound = (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }
}
