package org.opendaylight.ovsdb.lib.jsonrpc;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * JSONRpc service handler that is registered with JsonRpcEndpoint for servicing JsonRpc requests from peers.
 *
 * @see JsonRpcEndpoint
 */
public interface ServiceHandler {

    /**
     * Called on a request from a peer
     * @param methodName  The methodName requested by peer.
     * @param paramsNode  The parameter object send by the peer.
     * @return A response array as per Json RPC 1.0 spec
     */
    public List<Object> handleCall(String methodName, JsonNode paramsNode);
}
