package org.opendaylight.ovsdb.northbound;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Northbound interface for OVSDB tables
 */
public class TableResource {
    ObjectMapper objectMapper;
    String databaseName;
    String nodeId;

    TableResource(String nodeId, String databaseName){
        this.nodeId = nodeId;
        this.databaseName = databaseName;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private DatabaseSchema getDatabaseSchema (String databaseName) {
        OvsdbClient client = NodeResource.getOvsdbClient(nodeId, this);
        return client.getDatabaseSchema(databaseName);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTables() throws JsonProcessingException {
        DatabaseSchema dbSchema = this.getDatabaseSchema(databaseName);
        if (dbSchema == null) {
            return Response.noContent().build();
        }
        String response = objectMapper.writeValueAsString(dbSchema.getTables());
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableDetails(@PathParam("name") String name) throws JsonProcessingException {
        String csTableName = this.caseSensitiveTableName(databaseName, name);
        DatabaseSchema dbSchema = this.getDatabaseSchema(databaseName);
        if (dbSchema == null) {
            return Response.noContent().build();
        }
        TableSchema<GenericTableSchema> tableSchema = dbSchema.table(csTableName, GenericTableSchema.class);
        String response = objectMapper.writeValueAsString(tableSchema);
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @Path("{name}/row")
    public RowResource getDatabaseTables(@PathParam("name") String name){
        return new RowResource(nodeId, databaseName, name);
    }

    private String caseSensitiveTableName (String databaseName, String ciTableName) {
        DatabaseSchema dbSchema = this.getDatabaseSchema(databaseName);
        if (dbSchema == null) {
            return ciTableName;
        }
        Set<String> tables = dbSchema.getTables();
        if (tables == null) {
            return ciTableName;
        }
        for (String tableName : tables) {
            if (tableName.equalsIgnoreCase(ciTableName)) {
                return tableName;
            }
        }
        return ciTableName;
    }
}
