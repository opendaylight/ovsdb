/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OvsIFlowInstructionClient {
    static final Logger logger = LoggerFactory.getLogger(OvsIFlowInstructionClient.class);
    private static final Pattern NODETYPE = Pattern.compile("openflow:");
    private Integer tcpSrcPort; private String uri;
    private Long etherType; private BigInteger tunnelID;
    private String attachedMac; private MacAddress srcMac;
    private MacAddress dstMac; private VlanId vlanId;
    private Ipv4Prefix prefix; private Integer tos;
    private Short nwTtl; private Short ipProtocol;
    private Integer tcpDstPort; private Integer udpDstPort;
    private Integer udpSrcPort; private Boolean ofpDropAction;
    private Boolean packetInLLDP; private Short goToTableID;
    private Boolean ofpNormal; private Long ofpPort;
    private Boolean ofpFlood; private Boolean ofpLocal;
    private Boolean ofpController; private Long ofpOutputPort;
    private Long dpid; private NodeId nodeId;
    private Boolean popVlanID; private FlowBuilder flowBuilder;
    private OvsIFlowInstructionClient ovsFlowInstruction;
    private List<Instruction> existingInstructions;
    private Boolean popVlanID() { return this.popVlanID;}
    public Short goToTableID() {return this.goToTableID;}
    public BigInteger tunnelID() {return this.tunnelID;}
    public List<Instruction> existingInstructions() {return this.existingInstructions;}
    public Integer tcpSrcPort() {return this.tcpSrcPort;}
    public Long etherType() {return this.etherType;}
    public String attachedMac() {return this.attachedMac;}
    public String uri() {return this.uri;}
    public MacAddress srcMac() {return this.srcMac;}
    public MacAddress dstMac() {return this.dstMac;}
    public VlanId vlanId() {return this.vlanId;}
    public Ipv4Prefix prefix() {return this.prefix;}
    public Integer tos() {return this.tos;}
    public Short nwTtl() {return this.nwTtl;}
    public Short ipProtocol() {return this.ipProtocol;}
    public Integer tcpDstPort() {return this.tcpDstPort;}
    public Integer udpDstPort() {return this.udpDstPort;}
    public Integer udpSrcPort() {return this.udpSrcPort;}
    public Boolean ofpDropAction() {return this.ofpDropAction;}
    public Boolean packetInLLDP() {return this.packetInLLDP;}
    public Boolean ofpNormal() {return this.ofpNormal;}
    public Long ofpPort() {return this.ofpPort;}
    public Boolean ofpFlood() {return this.ofpFlood;}
    public Boolean ofpLocal() {return this.ofpLocal;}
    public Boolean ofpController() {return this.ofpController;}
    public Long ofpOutputPort() {return this.ofpOutputPort;}
    public Long dpid() {return this.dpid;}
    public NodeId nodeId() {return this.nodeId;}
    public FlowBuilder flowBuilder() {return this.flowBuilder;}
    public OvsIFlowInstructionClient goToTableID(final Short goToTableID)
    {this.goToTableID = goToTableID;return this;}

    public OvsIFlowInstructionClient existingInstructions(
            final List<Instruction> existingInstructions) {
        this.existingInstructions = existingInstructions;
        return this;
    }
    public OvsIFlowInstructionClient ovsFlowInstruction() {
        return this.ovsFlowInstruction;
    }
    public OvsIFlowInstructionClient tcpSrcPort(final Integer tcpSrcPort) {
        this.tcpSrcPort = tcpSrcPort;
        return this;
    }
    public OvsIFlowInstructionClient etherType(final Long etherType) {
        this.etherType = etherType;
        return this;
    }
    public OvsIFlowInstructionClient attachedMac(final String attachedMac) {
        this.attachedMac = attachedMac;
        return this;
    }
    public OvsIFlowInstructionClient uri(final String uri) {
        this.uri = uri;
        return this;
    }
    public OvsIFlowInstructionClient srcMac(
            final MacAddress srcMac) {
        this.srcMac = srcMac;
        return this;
    }
    public OvsIFlowInstructionClient dstMac(
            final MacAddress dstMac) {
        this.dstMac = dstMac;
        return this;
    }
    public OvsIFlowInstructionClient vlanId(
            final VlanId vlanId) {
        this.vlanId = vlanId;
        return this;
    }
    public OvsIFlowInstructionClient prefix(
            final Ipv4Prefix prefix) {
        this.prefix = prefix;
        return this;
    }
    public OvsIFlowInstructionClient tunnelID(final BigInteger tunnelID) {
        this.tunnelID = tunnelID;
        return this;
    }
    public OvsIFlowInstructionClient tos(final Integer tos) {
        this.tos = tos;
        return this;
    }
    public OvsIFlowInstructionClient nwTtl(final Short nwTtl) {
        this.nwTtl = nwTtl;
        return this;
    }
    public OvsIFlowInstructionClient ipProtocol(final Short ipProtocol) {
        this.ipProtocol = ipProtocol;
        return this;
    }
    public OvsIFlowInstructionClient tcpDstPort(final Integer tcpDstPort) {
        this.tcpDstPort = tcpDstPort;
        return this;
    }
    public OvsIFlowInstructionClient udpDstPort(final Integer udpDstPort) {
        this.udpDstPort = udpDstPort;
        return this;
    }
    public OvsIFlowInstructionClient udpSrcPort(final Integer udpSrcPort) {
        this.udpSrcPort = udpSrcPort;
        return this;
    }
    public OvsIFlowInstructionClient ofpDropAction(final Boolean ofpDropAction) {
        this.ofpDropAction = ofpDropAction;
        return this;
    }
    public OvsIFlowInstructionClient packetInLLDP(final Boolean packetInLLDP) {
        this.packetInLLDP = packetInLLDP;
        return this;
    }
    public OvsIFlowInstructionClient ofpNormal(final Boolean ofpNormal) {
        this.ofpNormal = ofpNormal;
        return this;
    }
    public OvsIFlowInstructionClient ofpPort(final Long ofpPort) {
        this.ofpPort = ofpPort;
        return this;
    }
    public OvsIFlowInstructionClient ofpFlood(final Boolean ofpFlood) {
        this.ofpFlood = ofpFlood;
        return this;
    }
    public OvsIFlowInstructionClient ofpLocal(final Boolean ofpLocal) {
        this.ofpLocal = ofpLocal;
        return this;
    }
    public OvsIFlowInstructionClient ofpController(final Boolean ofpController) {
        this.ofpController = ofpController;
        return this;
    }
    public OvsIFlowInstructionClient ofpOutputPort(final Long ofpOutputPort) {
        this.ofpOutputPort = ofpOutputPort;
        return this;
    }
    public OvsIFlowInstructionClient dpid(final Long dpid) {
        this.dpid = dpid;
        return this;
    }
    public OvsIFlowInstructionClient nodeId(
            final NodeId nodeId) {
        this.nodeId = nodeId;
        return this;
    }
    public OvsIFlowInstructionClient flowBuilder(
            final FlowBuilder flowBuilder) {
        this.flowBuilder = flowBuilder;
        return this;
    }
    public OvsIFlowInstructionClient ovsFlowInstruction(
            final OvsIFlowInstructionClient ovsFlowInstruction) {
        this.ovsFlowInstruction = ovsFlowInstruction;
        return this;
    }
    public OvsIFlowInstructionClient popVlanID(final Boolean popVlanID) {
        this.popVlanID = popVlanID;
        return this;
    }

    public void buildOtherProviderOpenFlowImplementationsHere() {
        // TODO Other Provider Instructions and Versions are simply new builders in this class
    }

    public FlowBuilder buildMDSalInstructions(FlowBuilder flowBuilder,
            OvsIFlowInstructionClient ovsFlowInstruction) throws NullPointerException {
        int instructionIndex = 0;
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();
        this.ovsFlowInstruction = ovsFlowInstruction;
        this.flowBuilder = flowBuilder;
        List<Instruction> instructions = new ArrayList<Instruction>();
        /* Pop VID Instruction */
        if (ovsFlowInstruction.popVlanID() != null) {
            OF13MdSalInstruction.createPopVlanInstructions(ib);
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Drop Instruction */
        if (ovsFlowInstruction.ofpDropAction() != null) {
            OF13MdSalInstruction.createDropInstructions(ib);
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Set VLAN ID */
        if (ovsFlowInstruction.vlanId() != null) {
            VlanId vid = ovsFlowInstruction.vlanId();
            OF13MdSalInstruction.createSetVlanInstructions(ib, vid);
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Set Tunnel ID */
        if (ovsFlowInstruction.tunnelID() != null) {
            OF13MdSalInstruction.createSetTunnelIdInstructions(ib, tunnelID);
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Add a GOTO table instruction */
        if (ovsFlowInstruction.goToTableID() != null) {
            OF13MdSalInstruction.createGotoTableInstructions(ib,
                    (ovsFlowInstruction.goToTableID()));
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Send to Reserved Port NORMAL (legacy forwarding) */
        if (ovsFlowInstruction.ofpNormal() != null) {
            OF13MdSalInstruction.createNormalInstructions(ib);
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Send to Reserved Port CONTROLLER (e.g. packet_in event) */
        if (ovsFlowInstruction.ofpController() != null) {
            OF13MdSalInstruction.createSendToControllerInstructions(ib);
            logger.info("2 Consumer ClientMatch Controller: {}", ovsFlowInstruction.ofpController());

            addInstruction(ib, instructionIndex++, instructions);
            logger.info("Consumer ClientMatch Controller: {}", instructions.toString());
        }
        /* Send to the Specified Output port */
        if (ovsFlowInstruction.ofpOutputPort() != null && ovsFlowInstruction.existingInstructions() != null) {
            OF13MdSalInstruction.addOutputPortInstructions
                    (ib, ovsFlowInstruction.dpid(), ovsFlowInstruction.ofpOutputPort(),
                            ovsFlowInstruction.existingInstructions());
            addInstruction(ib, instructionIndex++, instructions);
        }
        /* Send to the Specified Output port */
        if (ovsFlowInstruction.ofpOutputPort() != null  && (ovsFlowInstruction.existingInstructions() == null)) {
            OF13MdSalInstruction.createOutputPortInstructions
                    (ib, dpid, ovsFlowInstruction.ofpOutputPort());
            addInstruction(ib, instructionIndex++, instructions);
        }
        isb.setInstruction(instructions);
        flowBuilder.setInstructions(isb.build());
        return flowBuilder;
    }
    /* Add Instruction to the cumulative list */
    private void addInstruction(InstructionBuilder ib, int order, List<Instruction> instructions) {
        ib.setOrder(order);
        ib.setKey(new InstructionKey(order));
        instructions.add(ib.build());
    }

    private long parseDpid(final NodeId id) {
        final String nodeId = NODETYPE.matcher(id.getValue()).replaceAll("");
        BigInteger nodeIdBigInt = new BigInteger(nodeId);
        Long dpid = nodeIdBigInt.longValue();
        return dpid;
    }

    @Override
    public String toString() {
        return "OvsIFlowInstruction{" +
                "tcpSrcPort=" + tcpSrcPort +
                ", etherType=" + etherType +
                ", tunnelID=" + tunnelID +
                ", attachedMac='" + attachedMac + '\'' +
                ", uri='" + uri + '\'' +
                ", srcMac=" + srcMac +
                ", dstMac=" + dstMac +
                ", vlanId=" + vlanId +
                ", prefix=" + prefix +
                ", tos=" + tos +
                ", nwTtl=" + nwTtl +
                ", ipProtocol=" + ipProtocol +
                ", tcpDstPort=" + tcpDstPort +
                ", udpDstPort=" + udpDstPort +
                ", udpSrcPort=" + udpSrcPort +
                ", ofpDropAction=" + ofpDropAction +
                ", packetInLLDP=" + packetInLLDP +
                ", goToTableID=" + goToTableID +
                ", ofpNormal=" + ofpNormal +
                ", ofpPort=" + ofpPort +
                ", ofpFlood=" + ofpFlood +
                ", ofpLocal=" + ofpLocal +
                ", ofpController=" + ofpController +
                ", ofpOutputPort=" + ofpOutputPort +
                ", dpid=" + dpid +
                ", nodeId=" + nodeId +
                ", popVlanID=" + popVlanID +
                '}';
    }
}
