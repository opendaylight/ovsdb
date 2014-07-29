package org.opendaylight.ovsdb.northbound;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Northbound interface for OVSDB tables
 */
public class TableResource {

    String databaseName;
    String nodeId;

    TableResource(String nodeId, String databaseName){
        this.nodeId = nodeId;
        this.databaseName = databaseName;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTables(){
        return Response.noContent().build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableDetails(@PathParam("name") String name){
        return Response.noContent().build();
    }

    @Path("{name}/row")
    public RowResource getDatabaseTables(@PathParam("name") String name){
        return new RowResource(nodeId, databaseName, name);
    }

}
