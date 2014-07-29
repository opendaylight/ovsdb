package org.opendaylight.ovsdb.northbound;

import java.io.InputStream;

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

/**
 * Northbound Interface for OVSDB Nodes
 */
public class NodeResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNodes(){
        return Response.noContent().build();
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
    public Response getNode(@PathParam("id") String id){
        return Response.noContent().build();
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
