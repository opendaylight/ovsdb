/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolver;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.NetvirtProvidersProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 *
 * @author Anil Vishnoi (avishnoi@Brocade.com)
 *
 */
public class GatewayMacResolverService extends AbstractServiceInstance
                                        implements ConfigInterface, GatewayMacResolver,PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayMacResolverService.class);
    private static final short TABEL_FOR_ARP_FLOW = 0;
    private static final String ARP_REPLY_TO_CONTROLLER_FLOW_NAME = "GatewayArpReplyRouter";
    private static final int ARP_REPLY_TO_CONTROLLER_FLOW_PRIORITY = 10000;
    private static final Instruction SEND_TO_CONTROLLER_INSTRUCTION;
    private final ArpSender arpSender;
    private final SalFlowService flowService;
    private final AtomicLong flowCookie = new AtomicLong();
    private final ConcurrentHashMap<Ipv4Address, ArpResolverMetadata> arpRemoveFlowInputAndL3EpKeyById =
            new ConcurrentHashMap<Ipv4Address, ArpResolverMetadata>();
    private final int ARP_WATCH_BROTHERS = 10;
    private final int WAIT_CYCLES = 3;
    private final int PER_CYCLE_WAIT_DURATION = 1000;
    private final ListeningExecutorService arpWatcherWall = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ARP_WATCH_BROTHERS));

    static {
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(
                ImmutableList.of(ArpFlowFactory.createSendToControllerAction(0))).build();
        SEND_TO_CONTROLLER_INSTRUCTION = new InstructionBuilder().setOrder(0)
            .setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build())
            .build();
    }

    public GatewayMacResolverService(){
        super(Service.GATEWAY_RESOLVER);
        ProviderContext providerContext = NetvirtProvidersProvider.getProviderContext();
        checkNotNull(providerContext);
        PacketProcessingService packetProcessingService = providerContext.getRpcService(PacketProcessingService.class);
        if (packetProcessingService != null) {
            LOG.debug("{} was found.", PacketProcessingService.class.getSimpleName());
            this.arpSender = new ArpSender(packetProcessingService);
        } else {
            LOG.error("Missing service {}", PacketProcessingService.class.getSimpleName());
            this.arpSender = null;
        }
        flowService = providerContext.getRpcService(SalFlowService.class);
    }

    /**
     * Method do following actions:
     * 1. Install flow to direct ARP response packet to controller
     * 2. Send ARP request packet out on all port of the given External network bridge.
     * 3. Cache the flow that need to be removed once ARP resolution is done.
     * 4. Return listenable future so that user can add callback to get the MacAddress
     * @param externalNetworkBridge Broadcast ARP request packet on this bridge
     * NOTE:Periodic refresh is not supported yet.
     * @param gatewayIp  Resolve MAC address of this Gateway Ip
     * @return Future<MacAddress> Future object
     */
    @Override
    public ListenableFuture<MacAddress> resolveMacAddress( final Long externalNetworkBridgeDpid, final Ipv4Address gatewayIp, final Boolean periodicRefresh){
        if(arpRemoveFlowInputAndL3EpKeyById.containsKey(gatewayIp)){
            if(arpRemoveFlowInputAndL3EpKeyById.get(gatewayIp).getGatewayMacAddress() != null){
                return arpWatcherWall.submit(new Callable<MacAddress>(){

                    @Override
                    public MacAddress call() throws Exception {
                        return arpRemoveFlowInputAndL3EpKeyById.get(gatewayIp).getGatewayMacAddress();
                    }
                });
            }
        }else{
            arpRemoveFlowInputAndL3EpKeyById.put(gatewayIp,
                    new ArpResolverMetadata(null, gatewayIp,periodicRefresh));
        }

        final ArpMessageAddress senderAddress = createArpSenderAddress();
        if (senderAddress == null) {
            LOG.warn("Cannot create sender address to send ARP reuqest for gateway {}", gatewayIp);
            return null;
        }

        final String nodeName = OPENFLOW + externalNetworkBridgeDpid;

        final Node externalNetworkBridge = getOpenFlowNode(nodeName);
        if(externalNetworkBridge == null){
            LOG.error("MAC address for gateway {} can not be resolved, because external bridge {} "
                    + "is not connected to controller.",gatewayIp.getValue(),externalNetworkBridgeDpid );
            return null;
        }
        //Build arp reply router flow
        final Flow arpReplyToControllerFlow = createArpReplyToControllerFlow(senderAddress, gatewayIp);


        final InstanceIdentifier<Node> nodeIid = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, externalNetworkBridge.getKey())
                .build();
        final InstanceIdentifier<Flow> flowIid = createFlowIid(arpReplyToControllerFlow, nodeIid);
        final NodeRef nodeRef = new NodeRef(nodeIid);

        //Install flow
        Future<RpcResult<AddFlowOutput>> addFlowResult = flowService.addFlow(new AddFlowInputBuilder(
                arpReplyToControllerFlow).setFlowRef(new FlowRef(flowIid)).setNode(nodeRef).build());
        //wait for flow installation
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(addFlowResult),
                new FutureCallback<RpcResult<AddFlowOutput>>() {

            @Override
            public void onSuccess(RpcResult<AddFlowOutput> result) {
                if (!result.isSuccessful()) {
                    LOG.warn("Flow to route ARP Reply to Controller is not installed successfully : {} \nErrors: {}", flowIid,result.getErrors());
                    return;
                }
                LOG.debug("Flow to route ARP Reply to Controller installed successfully : {}", flowIid);

                //cache metadata
                arpRemoveFlowInputAndL3EpKeyById.get(gatewayIp).setFlowToRemove(
                        new RemoveFlowInputBuilder(arpReplyToControllerFlow).setNode(nodeRef).build());

                //Broadcast ARP request packets
                for (NodeConnector egressNc : externalNetworkBridge.getNodeConnector()) {
                    KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> egressNcIid = nodeIid.child(
                            NodeConnector.class, new NodeConnectorKey(egressNc.getId()));
                    ListenableFuture<RpcResult<Void>> futureSendArpResult = arpSender.sendArp(
                            senderAddress, gatewayIp, egressNcIid);
                    Futures.addCallback(futureSendArpResult, logResult(gatewayIp, egressNcIid));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("ARP Reply to Controller flow was not created: {}", flowIid, t);
            }
            }
        );
        //Wait for MacAddress population in cache
        return waitForMacAddress(gatewayIp);
    }

    private ListenableFuture<MacAddress> waitForMacAddress(final Ipv4Address gatewayIp){

        return arpWatcherWall.submit(new Callable<MacAddress>(){

            @Override
            public MacAddress call() throws Exception {
                for(int cycle = 0;cycle < WAIT_CYCLES;cycle++){
                    //Sleep before checking mac address, so meanwhile ARP request packets
                    // will be broadcasted on the bridge.
                    Thread.sleep(PER_CYCLE_WAIT_DURATION);
                    ArpResolverMetadata arpResolverMetadata = arpRemoveFlowInputAndL3EpKeyById.get(gatewayIp);
                    if(arpResolverMetadata != null && arpResolverMetadata.getGatewayMacAddress() != null){
                        if(!arpResolverMetadata.isPeriodicRefresh()){
                            return arpRemoveFlowInputAndL3EpKeyById.remove(gatewayIp).getGatewayMacAddress();
                        }
                        return arpResolverMetadata.getGatewayMacAddress();
                    }
                }
                return null;
            }
        });
    }
    private static @Nullable Ipv4Address getIPv4Addresses(IpAddress ipAddress) {
        if (ipAddress.getIpv4Address() == null) {
            return null;
        }
        return ipAddress.getIpv4Address();
    }

    private @Nullable ArpMessageAddress createArpSenderAddress( ) {
        ArpMessageAddress arpPacketSourceAddresses= null;
        try {
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                    ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            arpPacketSourceAddresses = new ArpMessageAddress(ArpUtils.bytesToMac(iface.getHardwareAddress()),new Ipv4Address(inetAddr.getHostAddress()));
                            LOG.info("Use host IP {} /MAC {} address as ARP request packet's source ip/mac address.",
                                    arpPacketSourceAddresses.getProtocolAddress().getValue(),arpPacketSourceAddresses.getHardwareAddress().getValue());
                            break;
                        }
                    }
                }
                LOG.error("Not able to retrieve local host IP/MAC address");
            }
        } catch (Exception e) {
            LOG.warn("Exception while fetching local host ip address ",e);
        }
        return arpPacketSourceAddresses;
    }

    private Flow createArpReplyToControllerFlow(final ArpMessageAddress senderAddress, final Ipv4Address ipForRequestedMac) {
        checkNotNull(senderAddress);
        checkNotNull(ipForRequestedMac);
        FlowBuilder arpFlow = new FlowBuilder().setTableId(TABEL_FOR_ARP_FLOW)
            .setFlowName(ARP_REPLY_TO_CONTROLLER_FLOW_NAME)
            .setPriority(ARP_REPLY_TO_CONTROLLER_FLOW_PRIORITY)
            .setBufferId(OFConstants.OFP_NO_BUFFER)
            .setIdleTimeout(0)
            .setHardTimeout(0)
            .setCookie(new FlowCookie(BigInteger.valueOf(flowCookie.incrementAndGet())))
            .setFlags(new FlowModFlags(false, false, false, false, false));

        EthernetMatch ethernetMatch = ArpFlowFactory.createEthernetMatch(senderAddress.getHardwareAddress());
        ArpMatch arpMatch = ArpFlowFactory.createArpMatch(senderAddress, ipForRequestedMac);
        Match match = new MatchBuilder().setEthernetMatch(ethernetMatch).setLayer3Match(arpMatch).build();
        arpFlow.setMatch(match);
        arpFlow.setInstructions(new InstructionsBuilder().setInstruction(
                ImmutableList.of(SEND_TO_CONTROLLER_INSTRUCTION)).build());
        arpFlow.setId(createFlowId(senderAddress, ipForRequestedMac));
        return arpFlow.build();
    }

    private FlowId createFlowId(ArpMessageAddress senderAddress, Ipv4Address ipForRequestedMac) {
        StringBuilder sb = new StringBuilder();
        sb.append(ARP_REPLY_TO_CONTROLLER_FLOW_NAME);
        sb.append("|").append(ipForRequestedMac.getValue());
        return new FlowId(sb.toString());
    }

    private static InstanceIdentifier<Flow> createFlowIid(Flow flow, InstanceIdentifier<Node> nodeIid) {
        return nodeIid.builder()
            .augmentation(FlowCapableNode.class)
            .child(Table.class, new TableKey(flow.getTableId()))
            .child(Flow.class, new FlowKey(flow.getId()))
            .build();
    }

    private FutureCallback<RpcResult<Void>> logResult(final Ipv4Address tpa,
            final KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> egressNcIid) {
        return new FutureCallback<RpcResult<Void>>() {

            @Override
            public void onSuccess(RpcResult<Void> result) {
                LOG.debug("ARP Request for IP {} was sent from {}.", tpa.getValue(), egressNcIid);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("ARP Request for IP {} was NOT sent from {}.", tpa.getValue(), egressNcIid);
            }
        };
    }

    @Override
    public void onPacketReceived(PacketReceived potentialArp) {
        Arp arp = null;
        try {
            arp = ArpResolverUtils.getArpFrom(potentialArp);
        } catch (Exception e) {
            LOG.trace(
                    "Failed to decode potential ARP packet. This could occur when other than ARP packet was received.",
                    e);
            return;
        }
        if (arp.getOperation() != ArpOperation.REPLY.intValue()) {
            LOG.trace("Packet is not ARP REPLY packet.");
            return;
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("ARP REPLY received - {}", ArpUtils.getArpToStringFormat(arp));
        }
        NodeKey nodeKey = potentialArp.getIngress().getValue().firstKeyOf(Node.class, NodeKey.class);
        if (nodeKey == null) {
            LOG.info("Unknown source node of ARP packet: {}", potentialArp);
            return;
        }
        Ipv4Address gatewayIpAddress = ArpUtils.bytesToIp(arp.getSenderProtocolAddress());
        MacAddress gatewayMacAddress = ArpUtils.bytesToMac(arp.getSenderHardwareAddress());
        ArpResolverMetadata removeFlowInputAndL3EpKey = arpRemoveFlowInputAndL3EpKeyById.get(gatewayIpAddress);
        if(removeFlowInputAndL3EpKey != null){
            removeFlowInputAndL3EpKey.setGatewayMacAddress(gatewayMacAddress);
            flowService.removeFlow(removeFlowInputAndL3EpKey.getFlowToRemove());
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext,
            ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(GatewayMacResolver.class.getName()), this);

    }

    @Override
    public void setDependencies(Object impl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void stopPeriodicReferesh(Ipv4Address gatewayIp) {
        arpRemoveFlowInputAndL3EpKeyById.remove(gatewayIp);
    }

}
