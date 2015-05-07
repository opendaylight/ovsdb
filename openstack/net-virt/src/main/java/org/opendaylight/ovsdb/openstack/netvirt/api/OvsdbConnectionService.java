package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Created by shague on 4/20/15.
 */
public interface OvsdbConnectionService {
    public List<Node> getNodes();
    public List<Node> getOvsdbNodes();
    public List<Node> getBridgeNodes();
    public Node getNode(String identifier);
}
