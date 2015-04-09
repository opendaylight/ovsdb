package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;

import com.google.common.collect.Sets;

public class OpenVSwitchBridgeAddCommand implements TransactCommand {

    @Override
    public void execute(TransactionBuilder transaction) {
        List<Operation> operations = transaction.getOperations();
        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
        List<Insert> inserts = TransactUtils.extractInsert(transaction, bridge.getSchema());
        for (Insert insert:inserts) {
            OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);
            ovs.setBridges(Sets.newHashSet(TransactUtils.setOrExtractUUID(insert)));
            transaction.add(op.mutate(ovs).addMutation(ovs.getBridgesColumn().getSchema(),
                    Mutator.INSERT,
                    ovs.getBridgesColumn().getData()));
        }
    }
}
