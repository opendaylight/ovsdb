package org.opendaylight.ovsdb.southbound;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class OvsdbConnectionListenerImpl implements OvsdbConnectionListener {
    List<OvsdbClient> clients = new CopyOnWriteArrayList<OvsdbClient>();

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionListenerImpl.class);
    public static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    public static final String OVSDB_URI_PREFIX = "ovsdb";

    DataBroker db;

    private OvsdbConnectionListenerImpl() {}

    public OvsdbConnectionListenerImpl(DataBroker db) {
        this.db = db;
        initializeOvsdbTopology();
    }

    @Override
    public void connected(OvsdbClient client) {
        LOG.info("OVSDB Connection from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, createInstanceIdentifier(client),
                createNode(client));
        transaction.submit();
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("OVSDB Disconnect from {}:{}",client.getConnectionInfo().getRemoteAddress(),
                client.getConnectionInfo().getRemotePort());
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.OPERATIONAL, createInstanceIdentifier(client));
        transaction.submit();
    }

    public Node createNode(OvsdbClient client) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(createNodeId(client));
        nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, createOvsdbAugmentation(client));
        return nodeBuilder.build();
    }

    public OvsdbNodeAugmentation createOvsdbAugmentation(OvsdbClient client) {
        OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
        ovsdbNodeBuilder.setIp(createIpAddress(client.getConnectionInfo().getRemoteAddress()));
        ovsdbNodeBuilder.setPort(new PortNumber(client.getConnectionInfo().getRemotePort()));
        return ovsdbNodeBuilder.build();
    }

    public IpAddress createIpAddress(InetAddress address) {
        IpAddress ip = null;
        if(address instanceof Inet4Address) {
            ip = createIpAddress((Inet4Address)address);
        } else if (address instanceof Inet6Address) {
            ip = createIpAddress((Inet6Address)address);
        }
        return ip;
    }

    public IpAddress createIpAddress(Inet4Address address) {
        Ipv4Address ipv4 = new Ipv4Address(address.getHostAddress());
        return new IpAddress(ipv4);
    }

    public IpAddress createIpAddress(Inet6Address address) {
        Ipv6Address ipv6 = new Ipv6Address(address.getHostAddress());
        return new IpAddress(ipv6);
    }

    public InstanceIdentifier<Node> createInstanceIdentifier(OvsdbClient client) {
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,createNodeKey(client));
        LOG.info("Created ovsdb path: {}",path);
        return path;
    }

    public NodeKey createNodeKey(OvsdbClient client) {
        return new NodeKey(createNodeId(client));
    }

    public NodeId createNodeId(OvsdbClient client) {
        return createNodeId(client.getConnectionInfo());
    }

    public NodeId createNodeId(OvsdbConnectionInfo connectionInfo) {
        String uriString = OVSDB_URI_PREFIX + ":/" + connectionInfo.getRemoteAddress() +
                   ":" + connectionInfo.getRemotePort();
        Uri uri = new Uri(uriString);
        NodeId nodeId = new NodeId(uri);
        return nodeId;
    }

    private void initializeOvsdbTopology() {
        InstanceIdentifier<Topology> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID));
        ReadWriteTransaction transaction = db.newReadWriteTransaction();
        initializeTopology(transaction);
        CheckedFuture<Optional<Topology>, ReadFailedException> ovsdbTp = transaction.read(LogicalDatastoreType.OPERATIONAL, path);
        try {
            if(!ovsdbTp.get().isPresent()) {
                TopologyBuilder tpb = new TopologyBuilder();
                tpb.setTopologyId(OVSDB_TOPOLOGY_ID);
                transaction.put(LogicalDatastoreType.OPERATIONAL, path, tpb.build());
                transaction.submit();
            } else {
                transaction.cancel();
            }
        } catch (Exception e) {
            LOG.error("Error initializing ovsdb topology {}",e);
        }
    }

    private void initializeTopology(ReadWriteTransaction t) {
        InstanceIdentifier<NetworkTopology> path = InstanceIdentifier
                .create(NetworkTopology.class);
       CheckedFuture<Optional<NetworkTopology>, ReadFailedException> tp = t.read(LogicalDatastoreType.OPERATIONAL,path);
       try {
           if(!tp.get().isPresent()) {
               NetworkTopologyBuilder ntb = new NetworkTopologyBuilder();
               t.put(LogicalDatastoreType.OPERATIONAL,path,ntb.build());
           }
       } catch (Exception e) {
           LOG.error("Error initializing ovsdb topology {}",e);
       }
    }


}
