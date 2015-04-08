package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.List;

import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;

import com.google.common.collect.Sets;

public class OpenVSwitchBridgeAddCommand implements TransactCommand {

    @Override
    public void execute(TransactionBuilder transaction) {
        List<Operation> operations = transaction.getOperations();
        Bridge bridge = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), Bridge.class);
        for (int count = 0;count < operations.size();count++) {
            Operation operation = operations.get(count);
            if (operation instanceof Insert && operation.getTableSchema().equals(bridge.getSchema())) {
                Insert insert = (Insert)operation;
                String uuidString = insert.getUuidName() != null
                        ? insert.getUuidName() : SouthboundMapper.getRandomUUID();
                insert.setUuidName(uuidString);

                // OpenVSwitchPart
                UUID uuid = new UUID(uuidString);
                OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);
                ovs.setBridges(Sets.newHashSet(uuid));
                transaction.add(op.mutate(ovs).addMutation(ovs.getBridgesColumn().getSchema(),
                        Mutator.INSERT,
                        ovs.getBridgesColumn().getData()));
            }
        }
    }
}
