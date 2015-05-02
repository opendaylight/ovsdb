package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbNodeCreateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeCreateCommand.class);

    public OvsdbNodeCreateCommand(
            ConnectionInfo key,TableUpdates updates,DatabaseSchema dbSchema,InstanceIdentifier<Node> connectionIid) {
        super(key,updates,dbSchema,connectionIid);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        LOG.trace("Getting external ids from ovs to see if node ID is present");
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(getConnectionIid().firstKeyOf(Node.class, NodeKey.class).getNodeId());
        OvsdbNodeAugmentationBuilder ovsdbNodeAugBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeAugBuilder.setConnectionInfo(getConnectionInfo());
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeAugBuilder.build());
        transaction.put(LogicalDatastoreType.OPERATIONAL,getConnectionIid(),nodeBuilder.build(),true);
    }

}
