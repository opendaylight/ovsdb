package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbBridgeUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBridgeUpdateCommand.class);

    public OvsdbBridgeUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<Bridge> updatedRows = TransactionUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema()).values();
        for(Bridge bridge : updatedRows) {
            final InstanceIdentifier<Node> nodePath = getKey().toInstanceIndentifier();
            Optional<Node> node = Optional.absent();
            try{
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
            }catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            if(node.isPresent()){
                LOG.info("Node {} is present",node);
                NodeBuilder managedNodeBuilder = new NodeBuilder();
                NodeId manageNodeId = SouthboundMapper.createManagedNodeId(getKey(), bridge.getUuid());
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
                nodeBuilder.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));

                OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
                List<ManagedNodeEntry> managedNodes = new ArrayList<ManagedNodeEntry>();
                ManagedNodeEntry entry = new ManagedNodeEntryBuilder().setBridgeRef(new OvsdbBridgeRef(managedNodePath)).build();
                managedNodes.add(entry);
                ovsdbNodeBuilder.setManagedNodeEntry(managedNodes);

                nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeBuilder.build());

                LOG.debug("Update node with managed node ref {}",ovsdbNodeBuilder.toString());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());

            }
        }
    }
}
