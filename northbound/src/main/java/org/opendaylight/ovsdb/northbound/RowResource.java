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
 * Northbound interface for OVSDB Rows
 */
public class RowResource {

    String nodeId;
    String databaseName;
    String rowName;

    public RowResource(String nodeId, String databaseName, String rowName) {
        this.nodeId = nodeId;
        this.databaseName = databaseName;
        this.rowName = rowName;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRows(){
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRow(InputStream stream) {
        return Response.noContent().build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRowDetails(@PathParam("id") String id){
        return Response.noContent().build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRow(@PathParam("id") String id){
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRow(@PathParam("id") String id){
        return Response.noContent().build();
    }


}
