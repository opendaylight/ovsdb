package org.opendaylight.ovsdb.northbound;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

/**
 * Northbound Interface for OVSDB Nodes
 */
public class NodeResource {
    ObjectMapper objectMapper;
    public NodeResource () {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static OvsdbClient getOvsdbConnection(String nodeId, Object bundleClassRef) {
        Node node = Node.fromString(nodeId);
        if (node == null) {
            throw new ResourceNotFoundException("Node "+nodeId+" not found");
        }
        OvsdbConnectionService connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, bundleClassRef);
        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            throw new ResourceNotFoundException("Connection for "+nodeId+" not available");
        }
        OvsdbClient client = connectionService.getConnection(node).getClient();
        if (client == null) {
            throw new ResourceNotFoundException("No Ovsdb Client to handle Node "+nodeId);
        }
        return client;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNodes() throws JsonProcessingException {
        OvsdbConnectionService connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);
        List<Node> nodes = connectionService.getNodes();
        if (nodes == null) return Response.noContent().build();

        List<String> nodeIds = Lists.newArrayList();
        for (Node node : nodes) {
            nodeIds.add(node.toString());
        }

        String response = objectMapper.writeValueAsString(nodeIds);
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNode(InputStream is){
        return Response.noContent().build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNode(@PathParam("id") String id) throws JsonProcessingException {
        OvsdbConnectionService connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);
        OvsdbClient client = NodeResource.getOvsdbConnection(id, this);
        if (client == null) {
            throw new ResourceNotFoundException("Node "+id+" not found");
        }
        OvsdbConnectionInfo connectionInfo = client.getConnectionInfo();
        String response = objectMapper.writeValueAsString(connectionInfo);
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNode(@PathParam("id") String id, InputStream is){
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteNode(@PathParam("id") String id){
        return Response.noContent().build();
    }

    @Path("{id}/database")
    public DatabaseResource getNodeDatabase(@PathParam("id") String nodeId){
        return new DatabaseResource(nodeId);
    }

}
