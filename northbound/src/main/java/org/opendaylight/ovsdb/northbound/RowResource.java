package org.opendaylight.ovsdb.northbound;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.plugin.api.OvsVswitchdSchemaConstants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Northbound interface for OVSDB Rows
 */
public class RowResource {

    String nodeId;
    String databaseName;
    String tableName;

    public RowResource(String nodeId, String databaseName, String tableName) {
        this.nodeId = nodeId;
        this.databaseName = databaseName;
        this.tableName = tableName;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRows(){
        return Response.noContent().build();
    }

    private OvsdbRow getOvsdbRow (InputStream stream) throws IOException {
        StringBuilder rowNodeStrBuilder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        while ((line = in.readLine()) != null) {
            rowNodeStrBuilder.append(line);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(rowNodeStrBuilder.toString());

        Node node = Node.fromString(nodeId);
        OvsdbConnectionService
        connectionService = (OvsdbConnectionService)ServiceHelper.getGlobalInstance(OvsdbConnectionService.class, this);
        OvsdbClient client = connectionService.getConnection(node).getClient();
        return OvsdbRow.fromJsonNode(client, OvsVswitchdSchemaConstants.DATABASE_NAME, jsonNode);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRow(InputStream stream) throws IOException {
        Node node = Node.fromString(nodeId);
        OvsdbRow localRow = this.getOvsdbRow(stream);
        if (localRow == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        OvsdbConfigurationService
        ovsdbTable = (OvsdbConfigurationService)ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class,
                                                                                    this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("OVS Configuration Service " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Row row = ovsdbTable.insertTree(node, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName,
                localRow.getParentTable(), new UUID(localRow.getParentUuid()), localRow.getParentColumn(),
                localRow.getRow());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String response = objectMapper.writeValueAsString(row);
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
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
