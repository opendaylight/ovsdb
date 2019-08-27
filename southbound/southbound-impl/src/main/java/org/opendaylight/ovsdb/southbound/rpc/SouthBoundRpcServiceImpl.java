package org.opendaylight.ovsdb.southbound.rpc;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionManager;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundProvider;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.ConfigureTerminationPointWithQosInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.ConfigureTerminationPointWithQosOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.southboundrpc.rev190820.SouthBoundRpcService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SouthBoundRpcServiceImpl implements SouthBoundRpcService {

  /**
   * The connection manager.
   */
  private static final Logger LOG = LoggerFactory.getLogger(SouthBoundRpcServiceImpl.class);
  private final OvsdbConnectionManager cm;
  private final DataBroker db;

  @Inject
  public SouthBoundRpcServiceImpl(@Reference RpcProviderRegistry rpcRegistry,
      @Reference OvsdbConnectionManager cm, DataBroker db) {
    this.cm = cm;
    this.db = db;
  }


  @Override
  public ListenableFuture<RpcResult<ConfigureTerminationPointWithQosOutput>> configureTerminationPointWithQos(
      ConfigureTerminationPointWithQosInput input) {

    RpcResultBuilder<ConfigureTerminationPointWithQosOutput> rpcResultBuilder = null;
    NodeId parentNodeId = input.getNodeRef();
    String terminationPointStr = input.getTerminationPointName();
    long ingressPolicyBurst = input.getIngressPolicingBurst();
    long ingressPolicyRate = input.getIngressPolicingRate();
    ReadWriteTransaction transaction = SouthboundProvider.getDb().newReadWriteTransaction();
    InstanceIdentifier<Node> parentNodeIid =  SouthboundMapper.createInstanceIdentifier(parentNodeId);
    Optional<Node> ovsdbNodeOpt = SouthboundUtil.readNode(transaction, parentNodeIid);
    if (ovsdbNodeOpt.isPresent()) {
      Node ovsdbNode = ovsdbNodeOpt.get();
      List<TerminationPoint> terminationPointListForNode = ovsdbNode.getTerminationPoint();
      boolean isterminationPointPresentinOper = Boolean.FALSE;

      for(TerminationPoint port: terminationPointListForNode) {
        if (port.getTpId().getValue().equals(terminationPointStr)) {
          isterminationPointPresentinOper = Boolean.TRUE;
          break;
        }
      }
      if (isterminationPointPresentinOper) {
        OvsdbConnectionInstance connectionInstance = cm.getConnectionInstance(parentNodeIid);
        connectionInstance.updateTerminationPointWithQosParameters(terminationPointStr, ingressPolicyBurst, ingressPolicyRate);
      } else {
        LOG.error("Termination Point {} missing in Oper DS on Node {}", terminationPointStr, parentNodeId);
        String errMsg = String.format("Termination Point {%s} missing in Oper DS on Node {%s}",
            terminationPointStr, parentNodeId.getValue());
        rpcResultBuilder = RpcResultBuilder.<ConfigureTerminationPointWithQosOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION, errMsg);
        return Futures.immediateFuture(rpcResultBuilder.build());
      }
    } else {
      LOG.error("Node {} not found in Oper DS for Updating Termination Point {}", parentNodeId.getValue(), terminationPointStr);
      String errMsg = String.format("Node {%s} missing in Oper DS on Node {%s}",
          parentNodeId.getValue(), terminationPointStr);
      rpcResultBuilder = RpcResultBuilder.<ConfigureTerminationPointWithQosOutput>failed()
          .withError(RpcError.ErrorType.APPLICATION, errMsg);
      return Futures.immediateFuture(rpcResultBuilder.build());
    }
    return Futures.immediateFuture(RpcResultBuilder.<ConfigureTerminationPointWithQosOutput>success().build());
  }
}
