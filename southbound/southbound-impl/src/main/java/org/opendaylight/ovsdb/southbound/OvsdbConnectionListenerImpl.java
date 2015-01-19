package org.opendaylight.ovsdb.southbound;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionListenerImpl implements OvsdbConnectionListener {
    List<OvsdbClient> clients = new CopyOnWriteArrayList<OvsdbClient>();

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionListenerImpl.class);
    @Override
    public void connected(OvsdbClient client) {
        LOG.info("OVSDB Connection from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        clients.add(client);
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnect from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        clients.remove(client);
    }

}
