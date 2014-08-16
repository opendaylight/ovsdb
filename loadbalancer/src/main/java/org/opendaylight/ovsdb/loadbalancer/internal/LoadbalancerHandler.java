package org.opendaylight.ovsdb.loadbalancer.internal;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface LoadbalancerHandler {

    void onSwitchAppeared(InstanceIdentifier<Table> appearedTablePath);

}
