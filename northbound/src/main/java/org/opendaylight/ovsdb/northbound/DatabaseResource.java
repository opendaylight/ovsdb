package org.opendaylight.ovsdb.northbound;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Northbound interface for OVSDB Databases
 */
public class DatabaseResource {

    String nodeId;

    DatabaseResource(String id) {
        this.nodeId = id;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatabases(){
        return Response.noContent().build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatabases(@PathParam("name") String name){
        return Response.noContent().build();
    }

    @Path("{name}/table")
    public TableResource getDatabaseTables(@PathParam("name") String name){
        return new TableResource(nodeId, name);
    }

}
