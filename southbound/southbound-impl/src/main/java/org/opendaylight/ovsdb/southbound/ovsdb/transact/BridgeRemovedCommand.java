package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;

public class BridgeRemovedCommand implements TransactCommand{
    private static final Logger LOG = LoggerFactory.getLogger(BridgeCreateCommand.class);
    private AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes;
    private DataBroker db;

    public BridgeRemovedCommand(DataBroker db,AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbBridgeAugmentation> changes) {
        this.db = db;
        this.changes = changes;
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Set<InstanceIdentifier<Node>> removed = TransactUtils.extractOvsdbManagedNodeRemoved(changes);
        Map<InstanceIdentifier<Node>, OvsdbBridgeAugmentation> originals = TransactUtils.extractOvsdbManagedNodeOriginal(changes);
        for(InstanceIdentifier<Node> ovsdbManagedNodeIid: removed) {
            LOG.info("Received request to delete ovsdb node {}",ovsdbManagedNodeIid);
            OvsdbBridgeAugmentation original = originals.get(ovsdbManagedNodeIid);
            Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class,null);
            Optional<UUID> bridgeUuidOptional = getBridgeUUID(ovsdbManagedNodeIid);
            if(bridgeUuidOptional.isPresent()) {
                UUID bridgeUuid = bridgeUuidOptional.get();
                OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class,null);
                transaction.add(op.delete(bridge.getSchema())
                        .where(bridge.getUuidColumn().getSchema().opEqual(bridgeUuid)).build());
                transaction.add(op.comment("Bridge: Deleting " + original.getBridgeName()));
                transaction.add(op.mutate(ovs.getSchema())
                        .addMutation(ovs.getBridgesColumn().getSchema(), Mutator.DELETE,
                                Sets.newHashSet(bridgeUuid)));
                transaction.add(op.comment("Open_vSwitch: Mutating " + original.getBridgeName() + " " + bridgeUuid));
             } else {
                 LOG.warn("Unable to delete bridge {} because it was not found in the operational store, and thus we cannot retrieve its UUID", ovsdbManagedNodeIid);
             }

        }
    }

    private Optional<UUID> getBridgeUUID(InstanceIdentifier<Node> ovsdbManagedNodeIid) {
        Optional<UUID> result = Optional.absent();
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> future = transaction.read(LogicalDatastoreType.OPERATIONAL, ovsdbManagedNodeIid);
        Optional<Node> optional;
        try {
            optional = future.get();
            if(optional.isPresent()) {
                OvsdbBridgeAugmentation bridge = (OvsdbBridgeAugmentation) optional.get();
                if(bridge != null && bridge.getBridgeUuid() != null) {
                    result = Optional.of(new UUID(bridge.getBridgeUuid().getValue()));
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("Unable to retrieve bridge from operational store",e);
        } catch (ExecutionException e) {
            LOG.warn("Unable to retrieve bridge from operational store",e);
        }
        return result;
    }

}
