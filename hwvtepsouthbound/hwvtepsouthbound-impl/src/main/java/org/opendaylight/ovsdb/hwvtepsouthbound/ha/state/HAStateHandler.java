package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

public interface HAStateHandler {
    public void handle(HAContext config, ReadWriteTransaction tx);
}
