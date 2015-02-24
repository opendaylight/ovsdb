package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbBridgeUpdateCommand implements TransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBridgeUpdateCommand.class);

    private TableUpdates updates;
    private DatabaseSchema dbSchema;

    private OvsdbClientKey key;

    public OvsdbBridgeUpdateCommand(OvsdbClientKey key,TableUpdates updates, DatabaseSchema dbSchema) {
        this.updates = updates;
        this.dbSchema = dbSchema;
        this.key = key;
    }
    @Override
    public void execute(ReadWriteTransaction transaction) {
        List<TypedBaseTable<?>> updatedRows = TransactionUtils.extractRowsUpdated(Bridge.class, updates, dbSchema);
        for(TypedBaseTable<?> updatedRow : updatedRows) {
            if(updatedRow instanceof Bridge) {
                Bridge bridge = (Bridge)updatedRow;
                final InstanceIdentifier<Node> nodePath = key.toInstanceIndentifier();
                Optional<Node> node = Optional.absent();
                try{
                    node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
                }catch (final ReadFailedException e) {
                    LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
                }
                if(node.isPresent()){
                    LOG.info("Node {} is present",node);
                    NodeBuilder managedNodeBuilder = new NodeBuilder();
                    NodeId manageNodeId = SouthboundMapper.createManagedNodeId(key, bridge.getUuid());
                    managedNodeBuilder.setNodeId(manageNodeId);

                    OvsdbManagedNodeAugmentationBuilder ovsdbManagedNodeBuilder = new OvsdbManagedNodeAugmentationBuilder();
                    ovsdbManagedNodeBuilder.setBridgeName(bridge.getName());
                    ovsdbManagedNodeBuilder.setBridgeUuid(new Uuid(bridge.getUuid().toString()));
                    ovsdbManagedNodeBuilder.setManagedBy(new OvsdbNodeRef(nodePath));
                    managedNodeBuilder.addAugmentation(OvsdbManagedNodeAugmentation.class, ovsdbManagedNodeBuilder.build());

                    InstanceIdentifier<Node> managedNodePath = SouthboundMapper.createInstanceIdentifier(manageNodeId);

                    LOG.debug("Store managed node augmentation data {}",ovsdbManagedNodeBuilder.toString());
                    transaction.put(LogicalDatastoreType.OPERATIONAL, managedNodePath, managedNodeBuilder.build());

                    //Update node with managed node reference
                    NodeBuilder nodeBuilder = new NodeBuilder();
                    nodeBuilder.setNodeId(SouthboundMapper.createNodeId(key.getIp(),key.getPort()));

                    OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
                    List<OvsdbBridgeRef> managedNodes = new ArrayList<OvsdbBridgeRef>();
                    managedNodes.add(new OvsdbBridgeRef(managedNodePath));
                    ovsdbNodeBuilder.setManagedNodeEntry(managedNodes);

                    nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeBuilder.build());

                    LOG.debug("Update node with managed node ref {}",ovsdbNodeBuilder.toString());
                    transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());

                }
            }
        }
    }
}
