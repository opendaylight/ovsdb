package org.opendaylight.ovsdb.southbound;

import java.net.UnknownHostException;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbNodeDataChangeListener implements DataChangeListener, AutoCloseable {

    private ListenerRegistration<DataChangeListener> registration;
    private OvsdbConnectionManager cm;
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeDataChangeListener.class);

    OvsdbNodeDataChangeListener(DataBroker db, OvsdbConnectionManager cm) {
        LOG.info("Registering OvsdbNodeDataChangeListener");
        this.cm = cm;
        InstanceIdentifier<OvsdbNodeAugmentation> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class)
                .augmentation(OvsdbNodeAugmentation.class);
        registration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this, DataChangeScope.ONE);

    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
       LOG.info("Received change to ovsdbNode: {}",changes);
       for( Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
           // TODO validate we have the correct kind of InstanceIdentifier
           if(created.getValue() instanceof OvsdbNodeAugmentation) {
               try {
                cm.connect((OvsdbNodeAugmentation)created.getValue());
            } catch (UnknownHostException e) {
                LOG.warn("Failed to connect to ovsdbNode", e);
            }
           }
       }
       // TODO handle case of updates to ovsdb nodes as needed

       // TODO handle case of delete of ovsdb nodes as needed
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

}
