package org.opendaylight.ovsdb.northbound;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.InputStream;
import java.util.Collections;
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

import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

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
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public static Node getOvsdbNode(String nodeId, Object bundleClassRef) {
        OvsdbConnectionService connectionService =
                (OvsdbConnectionService)ServiceHelper.
                        getGlobalInstance(OvsdbConnectionService.class, bundleClassRef);
        if (connectionService == null) {
            throw new ServiceUnavailableException("Ovsdb ConnectionService "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = connectionService.getNode(nodeId);
        if (node == null) {
            throw new ResourceNotFoundException("Node "+nodeId+" not found");
        }

        return node;
    }

    public static Connection getOvsdbConnection(String nodeId, Object bundleClassRef) {
        OvsdbConnectionService connectionService =
                (OvsdbConnectionService)ServiceHelper.
                        getGlobalInstance(OvsdbConnectionService.class, bundleClassRef);
        if (connectionService == null) {
            throw new ServiceUnavailableException("Ovsdb ConnectionService "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = connectionService.getNode(nodeId);
        if (node == null) {
            throw new ResourceNotFoundException("Node "+nodeId+" not found");
        }

        Connection connection = connectionService.getConnection(node);
        if (connection == null) {
            throw new ResourceNotFoundException("Connection for "+nodeId+" not available");
        }

        return connection;
    }

    public static OvsdbClient getOvsdbClient(String nodeId, Object bundleClassRef) {
        Connection connection = NodeResource.getOvsdbConnection(nodeId, bundleClassRef);
        OvsdbClient client = connection.getClient();
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
        if (nodes == null) {
            return Response.noContent().build();
        }

        List<String> nodeIds = Lists.newArrayList();
        for (Node node : nodes) {
            nodeIds.add(node.getId().getValue());
        }
        Collections.sort(nodeIds);

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
        OvsdbClient client = NodeResource.getOvsdbClient(id, this);
        String response = objectMapper.writeValueAsString(client.getConnectionInfo());
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
