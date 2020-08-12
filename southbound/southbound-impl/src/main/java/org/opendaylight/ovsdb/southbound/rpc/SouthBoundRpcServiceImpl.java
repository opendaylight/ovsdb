/*
 * Copyright © 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.rpc;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.ConfigureTerminationPointWithQosInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.ConfigureTerminationPointWithQosOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.SouthBoundRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.configure.termination.point.with.qos.input.EgressQos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.configure.termination.point.with.qos.input.IngressQos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthBoundRpcServiceImpl implements SouthBoundRpcService {

    /**
     * The connection manager.
     */
    private static final Logger LOG = LoggerFactory.getLogger("OvsdbEventLogger");
    private final RpcProviderService rpcProviderService;
    private ObjectRegistration<? extends RpcService> rpcRegistration;
    private final DataBroker dataBroker;
    private OvsdbConnectionManager cm = null;

    public SouthBoundRpcServiceImpl(final DataBroker dataBroker, @Reference RpcProviderService rpcProviderService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
    }

    public void setCm(OvsdbConnectionManager cm) {
        this.cm = cm;
    }

    @Override
    public ListenableFuture<RpcResult<ConfigureTerminationPointWithQosOutput>> configureTerminationPointWithQos(
        ConfigureTerminationPointWithQosInput input) {

        RpcResultBuilder<ConfigureTerminationPointWithQosOutput> rpcResultBuilder = null;
        InstanceIdentifier<Node> parentNodeId = (InstanceIdentifier<Node>) input.getNode();
        String terminationPoint = input.getTerminationPointName();
        List<IngressQos> ingressQos = new ArrayList<>(input.nonnullIngressQos().values());
        List<EgressQos> egressQos = new ArrayList<>(input.nonnullEgressQos().values());
        LOG.info(
            "configureTerminationPointWithQos called for Port:{} for configure ingressQos:{}, "
                + "egressQos:{} on node:{}",terminationPoint, ingressQos, egressQos,parentNodeId.toString());
        ReadWriteTransaction transaction = dataBroker.newReadWriteTransaction();
        //InstanceIdentifier<Node> parentNodeIid = SouthboundMapper
        //    .createInstanceIdentifier(new NodeId(parentNodeId.toString()));
        Optional<?> ovsdbNodeOpt = SouthboundUtil.readNode(transaction, parentNodeId);
        if (ovsdbNodeOpt.isPresent() && ovsdbNodeOpt.get() instanceof Node) {
            Node ovsdbNode = (Node)ovsdbNodeOpt.get();
            List<TerminationPoint> terminationPointListForNode = new ArrayList<>(ovsdbNode.nonnullTerminationPoint().values());
            boolean isterminationPointPresentinOper = Boolean.FALSE;
            if (terminationPointListForNode != null) {
                for (TerminationPoint port : terminationPointListForNode) {
                    if (port.getTpId().getValue().equals(input.getTerminationPointName())) {
                        isterminationPointPresentinOper = Boolean.TRUE;
                        break;
                    }
                }
            }
            LOG.trace("configureTerminationPointWithQos having connection-manager : {}", cm);
            if (isterminationPointPresentinOper && cm != null) {
                OvsdbConnectionInstance connectionInstance = cm.getConnectionInstance(parentNodeId);
                if (connectionInstance != null && connectionInstance.getHasDeviceOwnership()) {
                    LOG.info("Found connectionInstance {} and has Ownership", connectionInstance);

                    if (ingressQos != null && !ingressQos.isEmpty()) {
                        connectionInstance.updateInterfaceWithIngressParams(terminationPoint, ingressQos);
                    }
                    if (egressQos != null && !egressQos.isEmpty()) {
                        connectionInstance.updatePortWithEgressParams(terminationPoint, egressQos);
                    }
                } else {
                    LOG.warn("Connection Instance not found for Node: {} or doesn't has ownership",
                        parentNodeId);
                }
            } else {
                LOG.trace("Termination Point {} missing in Oper DS on Node {}",
                    input.getTerminationPointName(), parentNodeId);
                String errMsg = String
                    .format("Termination Point {%s} missing in Oper DS on Node {%s}",
                        input.getTerminationPointName(), parentNodeId.toString());
                rpcResultBuilder = RpcResultBuilder.<ConfigureTerminationPointWithQosOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION, errMsg);
                return Futures.immediateFuture(rpcResultBuilder.build());
            }
        } else {
            LOG.error("Node {} not found in Oper DS for Updating Termination Point {}",
                parentNodeId.toString(),
                input.getTerminationPointName());
            String errMsg = String.format("Node {%s} missing in Oper DS on Node {%s}",
                parentNodeId.toString(), input.getTerminationPointName());
            rpcResultBuilder = RpcResultBuilder.<ConfigureTerminationPointWithQosOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION, errMsg);
            return Futures.immediateFuture(rpcResultBuilder.build());
        }
        return Futures.immediateFuture(
            RpcResultBuilder.<ConfigureTerminationPointWithQosOutput>success().build());
    }

    public void registerRpc(String nodeIid) {
        KeyedInstanceIdentifier<Node, NodeKey> path = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(nodeIid + "/bridge/br-int")));
        LOG.info("registerRpc path is registered : {}", path);
        rpcRegistration = rpcProviderService.registerRpcImplementation(SouthBoundRpcService.class,
            this, ImmutableSet.of(path));
    }

    public void deregisterRpc(String nodeIid) {
        KeyedInstanceIdentifier<Node, NodeKey> path = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
            .child(Node.class, new NodeKey(new NodeId(nodeIid + "/bridge/br-int")));
        LOG.info("deregisterRpc for deregisterRpc : {}", path);
        if (rpcRegistration != null) {
            rpcRegistration.close();
        }
    }
}
