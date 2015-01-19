package org.opendaylight.ovsdb.southbound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionListenerImpl implements OvsdbConnectionListener, AutoCloseable {
    Map<OVSDBClientKey,OvsdbClient> clients = new ConcurrentHashMap<OVSDBClientKey,OvsdbClient>();
    Map<OVSDBClientKey,InstanceIdentifier<Node>> instances = new ConcurrentHashMap<OVSDBClientKey,InstanceIdentifier<Node>>();
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionListenerImpl.class);

    DataBroker db;

    public OvsdbConnectionListenerImpl(DataBroker db) {
        this.db = db;
    }

    @Override
    public void connected(OvsdbClient client) {
        LOG.info("OVSDB Connection from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        clients.put(new OVSDBClientKey(client), client);
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, SouthboundMapper.createInstanceIdentifier(client),
                SouthboundMapper.createNode(client));
        // TODO - Check the future and retry if needed
        transaction.submit();
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnect from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, SouthboundMapper.createInstanceIdentifier(client));
        // TODO - Check the future and retry if needed
        transaction.submit();
        clients.remove(new OVSDBClientKey(client));
    }

    @Override
    public void close() throws Exception {
        for(OvsdbClient client: clients.values()) {
            client.disconnect();
        }
    }


}
