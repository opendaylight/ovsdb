package org.opendaylight.ovsdb.southbound.transactions.md;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class OvsdbNodeRemoveCommand implements TransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeRemoveCommand.class);

    private OvsdbClientKey key;

    public OvsdbNodeRemoveCommand(OvsdbClientKey key,TableUpdates updates,DatabaseSchema dbSchema) {
        this.key = key;
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        CheckedFuture<Optional<Node>, ReadFailedException> ovsdbNodeFuture = transaction.read(LogicalDatastoreType.OPERATIONAL, key.toInstanceIndentifier());
        Optional<Node> ovsdbNodeOptional;
        try {
            ovsdbNodeOptional = ovsdbNodeFuture.get();
            if(ovsdbNodeOptional.isPresent()) {
                Node ovsdbNode = ovsdbNodeOptional.get();
                OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
                for(OvsdbBridgeRef managedNode: ovsdbNodeAugmentation.getManagedNodeEntry()) {
                    transaction.delete(LogicalDatastoreType.OPERATIONAL, managedNode.getValue());
                }
                transaction.delete(LogicalDatastoreType.OPERATIONAL, key.toInstanceIndentifier());
            }
        } catch (Exception e) {
            LOG.warn("Failure to delete ovsdbNode {}",e);
        }
    }

}
