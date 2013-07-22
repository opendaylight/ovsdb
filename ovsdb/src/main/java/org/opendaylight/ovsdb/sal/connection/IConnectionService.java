package org.opendaylight.ovsdb.sal.connection;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface that defines the methods available to the functional modules that operate
 * above SAL for disconnecting or connecting to a particular node.
 */
public interface IConnectionService {
    /**
     * Disconnect a Node that is connected to this Controller.
     *
     * @param node
     * @param flow
     */
    public Status disconnect(Node node);

    /**
     * Connect to a node with a specified node type.
     *
     * @param type Type of the node representing NodeIDType.
     * @param connectionIdentifier Convenient identifier for the applications to make use of
     * @param params Connection Params in Map format. This is entirely handled by the south-bound
     * plugins and is an opaque value for SAL. Typical values keyed inside this params are
     * Management IP-Address, Username, Password, Security Keys, etc...
     *
     *  @return Node
     */
    public Node connect (String type, String connectionIdentifier, Map<ConnectionConstants, String> params);


    /**
     * Discover the node type and Connect to the first plugin that is able to connect with the specified parameters.
     *
     * @param type Type of the node representing NodeIDType.
     * @param connectionIdentifier Convenient identifier for the applications to make use of
     * @param params Connection Params in Map format. This is entirely handled by the south-bound
     * plugins and is an opaque value for SAL. Typical values keyed inside this params are
     * Management IP-Address, Username, Password, Security Keys, etc...
     *
     *  @return Node
     */
    public Node connect (String connectionIdentifier, Map<ConnectionConstants, String> params);

}