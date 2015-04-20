package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 * Created by shague on 4/20/15.
 */
public interface OvsdbConnectionService {
    public Connection getConnection(Node node);
    public List<Node> getNodes();
    public Node getNode(String identifier);
    public Node connect(String identifier, Map<ConnectionConstants, String> params);
}
