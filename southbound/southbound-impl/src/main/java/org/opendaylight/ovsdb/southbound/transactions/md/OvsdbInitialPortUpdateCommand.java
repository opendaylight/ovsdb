/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbInitialPortUpdateCommand extends OvsdbPortUpdateCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbInitialPortUpdateCommand.class);

    // FIXME: we probably want to store a Map here
    private final Map<NodeId, List<TerminationPoint>> brigeToTerminationPointList = new HashMap<>();
    private final Map<NodeId, Node>  updatedBridgeNodes;

    public OvsdbInitialPortUpdateCommand(InstanceIdentifierCodec instanceIdentifierCodec, OvsdbConnectionInstance key,
                                         TableUpdates updates, DatabaseSchema dbSchema,
                                         Map<NodeId, Node> updatedBridgeNodes) {
        super(instanceIdentifierCodec, key, updates, dbSchema);
        this.updatedBridgeNodes = updatedBridgeNodes;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        super.execute(transaction);
        mergeToBridgeNode(transaction);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void updateToDataStore(ReadWriteTransaction transaction, TerminationPointBuilder tpBuilder,
                                     InstanceIdentifier<TerminationPoint> tpPath, boolean merge) {
        try {
            NodeId bridgeNodeId = tpPath.firstKeyOf(Node.class).getNodeId();
            TerminationPoint terminationPoint = tpBuilder.build();
            if (updatedBridgeNodes.containsKey(bridgeNodeId)) {
                if (brigeToTerminationPointList.containsKey(bridgeNodeId)) {
                    brigeToTerminationPointList.get(bridgeNodeId).add(terminationPoint);
                } else {
                    List<TerminationPoint> terminationPointList = new ArrayList<>();
                    terminationPointList.add(terminationPoint);
                    brigeToTerminationPointList.put(bridgeNodeId, terminationPointList);
                }
                LOG.debug("DEVICE - {} Initial TerminationPoint : {} to Bridge : {}", TransactionType.ADD,
                        terminationPoint.key().getTpId().getValue(), bridgeNodeId.getValue());
            }
        }
        catch (RuntimeException exp) {
            LOG.error("Exception in adding the terminationPoint {} to bridge ", tpPath);
        }

    }

    private void mergeToBridgeNode(ReadWriteTransaction transaction) {

        brigeToTerminationPointList.forEach((nodeId, terminationPoints) -> {
            InstanceIdentifier<Node> bridgeIid = SouthboundMapper.createInstanceIdentifier(nodeId);
            StringBuilder terminationPointList = new StringBuilder();
            Node bridgeNode = updatedBridgeNodes.get(nodeId);
            if (bridgeNode != null) {
                Node bridgeNodeWithTerminationPoints = new NodeBuilder(bridgeNode)
                    .setTerminationPoint(BindingMap.ordered(terminationPoints))
                    .build();
                transaction.merge(LogicalDatastoreType.OPERATIONAL, bridgeIid, bridgeNodeWithTerminationPoints);
            }
            terminationPoints.forEach(terminationPoint -> {
                terminationPointList.append(terminationPoint.key().getTpId().getValue() +  ",");
            });
            LOG.info("DEVICE - {} Initial TerminationPoint List : {} to Bridge : {}", TransactionType.ADD,
                terminationPointList.toString(), bridgeIid.firstKeyOf(Node.class).getNodeId().getValue());
        });
    }
}
