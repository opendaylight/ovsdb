/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolver;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolverListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.NetvirtProvidersProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
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

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *
 * @author Anil Vishnoi (avishnoi@Brocade.com)
 *
 */
public class GatewayMacResolverService extends AbstractServiceInstance
                                        implements ConfigInterface, GatewayMacResolver,PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayMacResolverService.class);
    private static final String ARP_REPLY_TO_CONTROLLER_FLOW_NAME = "GatewayArpReplyRouter";
    private static final int ARP_REPLY_TO_CONTROLLER_FLOW_PRIORITY = 10000;
    private static final Instruction SEND_TO_CONTROLLER_INSTRUCTION;
    private ArpSender arpSender;
    private SalFlowService flowService;
    private final AtomicLong flowCookie = new AtomicLong();
    private final ConcurrentMap<Ipv4Address, ArpResolverMetadata> gatewayToArpMetadataMap =
            new ConcurrentHashMap<>();
    private static final int ARP_WATCH_BROTHERS = 10;
    private static final int WAIT_CYCLES = 3;
    private static final int PER_CYCLE_WAIT_DURATION = 1000;
    private static final int REFRESH_INTERVAL = 10;
    private final ListeningExecutorService arpWatcherWall = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ARP_WATCH_BROTHERS));
    private final ScheduledExecutorService gatewayMacRefresherPool = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService refreshRequester = Executors.newSingleThreadScheduledExecutor();
    private AtomicBoolean initializationDone = new AtomicBoolean(false);
    private volatile ConfigurationService configurationService;
    private volatile NodeCacheManager nodeCacheManager;

    static {
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(
                ImmutableList.of(ArpFlowFactory.createSendToControllerAction(0))).build();
        SEND_TO_CONTROLLER_INSTRUCTION = new InstructionBuilder().setOrder(0)
            .setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build())
            .build();
    }

    public GatewayMacResolverService(){
        super(Service.GATEWAY_RESOLVER);
    }

    public GatewayMacResolverService(Service service){
        super(service);
    }

    private void init(){
        if(!initializationDone.get()){
            initializationDone.set(true);
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
            refreshRequester.scheduleWithFixedDelay(new Runnable(){

                @Override
                public void run() {
                    if (!gatewayToArpMetadataMap.isEmpty()){
                        for(final Entry<Ipv4Address, ArpResolverMetadata> gatewayToArpMetadataEntry : gatewayToArpMetadataMap.entrySet()){
                            final Ipv4Address gatewayIp = gatewayToArpMetadataEntry.getKey();
                            final ArpResolverMetadata gatewayMetaData =
                                    checkAndGetExternalBridgeDpid(gatewayToArpMetadataEntry.getValue());
                            gatewayMacRefresherPool.schedule(new Runnable(){

                                @Override
                                public void run() {

                                    final Node externalNetworkBridge = getExternalBridge(gatewayMetaData.getExternalNetworkBridgeDpid());
                                    if(externalNetworkBridge == null){
                                        LOG.error("MAC address for gateway {} can not be resolved, because external bridge {} "
                                                + "is not connected to controller.",
                                                gatewayIp.getValue(),
                                                gatewayMetaData.getExternalNetworkBridgeDpid() );
                                    } else {
                                        LOG.debug("Refresh Gateway Mac for gateway {} using source ip {} and mac {} for ARP request",
                                                gatewayIp.getValue(),gatewayMetaData.getArpRequestSourceIp().getValue(),gatewayMetaData.getArpRequestSourceMacAddress().getValue());

                                        sendGatewayArpRequest(externalNetworkBridge,gatewayIp,gatewayMetaData.getArpRequestSourceIp(), gatewayMetaData.getArpRequestSourceMacAddress());
                                    }
                                }
                            }, 1, TimeUnit.SECONDS);
                        }
                    }
                }
            }, REFRESH_INTERVAL, REFRESH_INTERVAL, TimeUnit.SECONDS);
        }
    }
    /**
     * Method do following actions:
     * 1. Install flow to direct ARP response packet to controller
     * 2. Send ARP request packet out on all port of the given External network bridge.
     * 3. Cache the flow that need to be removed once ARP resolution is done.
     * 4. Return listenable future so that user can add callback to get the MacAddress
     * @param externalNetworkBridgeDpid Broadcast ARP request packet on this bridge
     * @param gatewayIp IP address for which MAC need to be resolved
     * @param sourceIpAddress Source Ip address for the ARP request packet
     * @param sourceMacAddress Source Mac address for the ARP request packet
     * @param periodicRefresh Enable/Disable periodic refresh of the Gateway Mac address
     * @return Future object
     */
    @Override
    public ListenableFuture<MacAddress> resolveMacAddress(
            final GatewayMacResolverListener gatewayMacResolverListener, final Long externalNetworkBridgeDpid,
            final Boolean refreshExternalNetworkBridgeDpidIfNeeded,
            final Ipv4Address gatewayIp, final Ipv4Address sourceIpAddress, final MacAddress sourceMacAddress,
            final Boolean periodicRefresh){
        Preconditions.checkNotNull(refreshExternalNetworkBridgeDpidIfNeeded);
        Preconditions.checkNotNull(sourceIpAddress);
        Preconditions.checkNotNull(sourceMacAddress);
        Preconditions.checkNotNull(gatewayIp);

        LOG.info("Trigger Mac resolution for gateway {}, using source ip {} and mac {}",
                gatewayIp.getValue(),sourceIpAddress.getValue(),sourceMacAddress.getValue());

        init();
        if(gatewayToArpMetadataMap.containsKey(gatewayIp)){
            if(gatewayToArpMetadataMap.get(gatewayIp).getGatewayMacAddress() != null){
                return arpWatcherWall.submit(new Callable<MacAddress>(){

                    @Override
                    public MacAddress call() throws Exception {
                        return gatewayToArpMetadataMap.get(gatewayIp).getGatewayMacAddress();
                    }
                });
            }
        }else{
            gatewayToArpMetadataMap.put(gatewayIp,new ArpResolverMetadata(gatewayMacResolverListener,
                    externalNetworkBridgeDpid, refreshExternalNetworkBridgeDpidIfNeeded,
                    gatewayIp, sourceIpAddress, sourceMacAddress, periodicRefresh));
        }


        final Node externalNetworkBridge = getExternalBridge(externalNetworkBridgeDpid);
        if (externalNetworkBridge == null) {
            if (!refreshExternalNetworkBridgeDpidIfNeeded) {
                LOG.error("MAC address for gateway {} can not be resolved, because external bridge {} "
                        + "is not connected to controller.", gatewayIp.getValue(), externalNetworkBridgeDpid);
            } else {
                LOG.debug("MAC address for gateway {} can not be resolved, since dpid was not refreshed yet",
                        gatewayIp.getValue());
            }
            return null;
        }

        sendGatewayArpRequest(externalNetworkBridge,gatewayIp,sourceIpAddress, sourceMacAddress);

        //Wait for MacAddress population in cache
        return waitForMacAddress(gatewayIp);
    }

    private Node getExternalBridge(final Long externalNetworkBridgeDpid){
        if (externalNetworkBridgeDpid != null) {
            final String nodeName = OPENFLOW + externalNetworkBridgeDpid;
            return getOpenFlowNode(nodeName);
        }
        return null;
    }

    private ArpResolverMetadata checkAndGetExternalBridgeDpid(ArpResolverMetadata gatewayMetaData) {
        final Long origDpid = gatewayMetaData.getExternalNetworkBridgeDpid();

        // If we are not allowing dpid to change, there is nothing further to do here
        if (!gatewayMetaData.isRefreshExternalNetworkBridgeDpidIfNeeded()) {
            return gatewayMetaData;
        }

        // If current dpid is null, or if mac is not getting resolved, make an attempt to
        // grab a different dpid, so a different (or updated) external bridge gets used
        if (origDpid == null || !gatewayMetaData.isGatewayMacAddressResolved()) {
            Long newDpid = getAnotherExternalBridgeDpid(origDpid);
            gatewayMetaData.setExternalNetworkBridgeDpid(newDpid);
        }

        return gatewayMetaData;
    }

    private Long getAnotherExternalBridgeDpid(final Long unwantedDpid) {
        LOG.trace("Being asked to find a new dpid. unwantedDpid:{} cachemanager:{} configurationService:{}",
                unwantedDpid, nodeCacheManager, configurationService);

        if (nodeCacheManager == null) {
            LOG.error("Unable to find external dpid to use for resolver: no nodeCacheManager");
            return unwantedDpid;
        }
        if (configurationService == null) {
            LOG.error("Unable to find external dpid to use for resolver: no configurationService");
            return unwantedDpid;
        }

        // Pickup another dpid in list that is different than the unwanted one provided and is in the
        // operational tree. If none can be found, return the provided dpid as a last resort.
        // NOTE: We are assuming that all the br-ex are serving one external network and gateway ip of
        // the external network is reachable from every br-ex
        // TODO: Consider other deployment scenario, and think of another solution.
        List<Long> dpids =  nodeCacheManager.getBridgeDpids(configurationService.getExternalBridgeName());
        Collections.shuffle(dpids);
        for (Long dpid : dpids) {
            if (dpid == null || dpid.equals(unwantedDpid) || getExternalBridge(dpid) == null) {
                continue;
            }

            LOG.debug("Gateway Mac Resolver Service will use dpid {}", dpid);
            return dpid;
        }

        LOG.warn("Unable to find usable external dpid for resolver. Best choice is still {}", unwantedDpid);
        return unwantedDpid;
    }

    private void sendGatewayArpRequest(final Node externalNetworkBridge,final Ipv4Address gatewayIp,
            final Ipv4Address sourceIpAddress, final MacAddress sourceMacAddress){
        final ArpMessageAddress senderAddress = new ArpMessageAddress(sourceMacAddress,sourceIpAddress);

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

                ArpResolverMetadata gatewayArpMetadata = gatewayToArpMetadataMap.get(gatewayIp);
                if (gatewayArpMetadata == null) {
                    LOG.warn("No metadata found for gatewayIp: {}", gatewayIp);
                    return;
                }

                //cache metadata
                gatewayArpMetadata.setFlowToRemove(new RemoveFlowInputBuilder(arpReplyToControllerFlow).setNode(nodeRef).build());

                //get MAC DA for ARP packets
                MacAddress arpRequestDestMacAddress = gatewayArpMetadata.getArpRequestDestMacAddress();

                //Send ARP request packets
                for (NodeConnector egressNc : externalNetworkBridge.getNodeConnector()) {
                    KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> egressNcIid = nodeIid.child(
                            NodeConnector.class, new NodeConnectorKey(egressNc.getId()));
                    ListenableFuture<RpcResult<Void>> futureSendArpResult = arpSender.sendArp(
                            senderAddress, gatewayIp, arpRequestDestMacAddress, egressNcIid);
                    Futures.addCallback(futureSendArpResult, logResult(gatewayIp, egressNcIid));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("ARP Reply to Controller flow was not created: {}", flowIid, t);
            }
            }
        );
    }

    private ListenableFuture<MacAddress> waitForMacAddress(final Ipv4Address gatewayIp){

        return arpWatcherWall.submit(new Callable<MacAddress>(){

            @Override
            public MacAddress call() throws Exception {
                for(int cycle = 0;cycle < WAIT_CYCLES;cycle++){
                    //Sleep before checking mac address, so meanwhile ARP request packets
                    // will be broadcasted on the bridge.
                    Thread.sleep(PER_CYCLE_WAIT_DURATION);
                    ArpResolverMetadata arpResolverMetadata = gatewayToArpMetadataMap.get(gatewayIp);
                    if(arpResolverMetadata != null && arpResolverMetadata.getGatewayMacAddress() != null){
                        if(!arpResolverMetadata.isPeriodicRefresh()){
                            return gatewayToArpMetadataMap.remove(gatewayIp).getGatewayMacAddress();
                        }
                        return arpResolverMetadata.getGatewayMacAddress();
                    }
                }
                return null;
            }
        });
    }

    private Flow createArpReplyToControllerFlow(final ArpMessageAddress senderAddress, final Ipv4Address ipForRequestedMac) {
        checkNotNull(senderAddress);
        checkNotNull(ipForRequestedMac);
        FlowBuilder arpFlow = new FlowBuilder().setTableId(Service.CLASSIFIER.getTable())
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
        arpFlow.setId(createFlowId(ipForRequestedMac));
        return arpFlow.build();
    }

    private FlowId createFlowId(Ipv4Address ipForRequestedMac) {
        String flowId = ARP_REPLY_TO_CONTROLLER_FLOW_NAME + "|" + ipForRequestedMac.getValue();
        return new FlowId(flowId);
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
        Arp arp = ArpResolverUtils.getArpFrom(potentialArp);
        if(arp != null){
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
            ArpResolverMetadata candidateGatewayIp = gatewayToArpMetadataMap.get(gatewayIpAddress);
            if(candidateGatewayIp != null){
                LOG.debug("Resolved MAC for Gateway Ip {} is {}",gatewayIpAddress.getValue(),gatewayMacAddress.getValue());
                candidateGatewayIp.setGatewayMacAddress(gatewayMacAddress);
                flowService.removeFlow(candidateGatewayIp.getFlowToRemove());
            }
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext,
            ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(GatewayMacResolver.class.getName()), this);

    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof NodeCacheManager) {
            nodeCacheManager = (NodeCacheManager) impl;
        } else if (impl instanceof ConfigurationService) {
            configurationService = (ConfigurationService) impl;
        }
    }

    @Override
    public void stopPeriodicRefresh(Ipv4Address gatewayIp) {
        init();
        gatewayToArpMetadataMap.remove(gatewayIp);
    }

}
