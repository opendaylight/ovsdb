/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.northbound;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

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
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.plugin.api.OvsVswitchdSchemaConstants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    ObjectMapper objectMapper;

    public RowResource(String nodeId, String databaseName, String tableName) {
        this.nodeId = nodeId;
        this.databaseName = databaseName;
        this.tableName = tableName;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    private OvsdbRow getOvsdbRow (InputStream stream) throws IOException {
        StringBuilder rowNodeStrBuilder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        while ((line = in.readLine()) != null) {
            rowNodeStrBuilder.append(line);
        }
        JsonNode jsonNode = objectMapper.readTree(rowNodeStrBuilder.toString());
        OvsdbClient client = NodeResource.getOvsdbClient(nodeId, this);
        return OvsdbRow.fromJsonNode(client, OvsVswitchdSchemaConstants.DATABASE_NAME, jsonNode);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRows() throws JsonProcessingException {
        OvsdbConfigurationService
                ovsdbTable = (OvsdbConfigurationService)ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("Ovsdb ConfigurationService " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = NodeResource.getOvsdbNode(nodeId, this);
        Map<UUID,Row<GenericTableSchema>> rows = null;
        try {
            rows = ovsdbTable.getRows(node, databaseName, tableName);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        String response = objectMapper.writeValueAsString(rows);
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRow(InputStream stream) throws IOException {
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

        Node node = NodeResource.getOvsdbNode(nodeId, this);
        Row row = ovsdbTable.insertTree(node, OvsVswitchdSchemaConstants.DATABASE_NAME, tableName,
                localRow.getParentTable(), new UUID(localRow.getParentUuid()), localRow.getParentColumn(),
                localRow.getRow());
        String response = objectMapper.writeValueAsString(row);
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRowDetails(@PathParam("id") String id) throws JsonProcessingException {
        OvsdbConfigurationService
        ovsdbTable = (OvsdbConfigurationService)ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class,
                                                                                    this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("Ovsdb ConfigurationService " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = NodeResource.getOvsdbNode(nodeId, this);
        Row<GenericTableSchema> row = null;
        try {
            row = ovsdbTable.getRow(node, databaseName, tableName, new UUID(id));
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        String response = objectMapper.writeValueAsString(row);
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRow(@PathParam("id") String id, InputStream stream) throws IOException{
        OvsdbRow localRow = this.getOvsdbRow(stream);
        if (localRow == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        OvsdbConfigurationService ovsdbTable = (OvsdbConfigurationService)ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class,
                                                                                    this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("OVS Configuration Service " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = NodeResource.getOvsdbNode(nodeId, this);
        Row<GenericTableSchema> row = ovsdbTable.updateRow(node, databaseName, tableName, new UUID(id), localRow.getRow(), true);
        String response = objectMapper.writeValueAsString(row);
        return Response.status(Response.Status.OK)
                .entity(response)
                .build();
    }

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRow(@PathParam("id") String id){
        OvsdbConfigurationService
        ovsdbTable = (OvsdbConfigurationService)ServiceHelper.getGlobalInstance(OvsdbConfigurationService.class,
                                                                                    this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("Ovsdb ConfigurationService " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = NodeResource.getOvsdbNode(nodeId, this);
        try {
            ovsdbTable.deleteRow(node, databaseName, tableName, new UUID(id));
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        return Response.status(Response.Status.OK)
                .build();

    }
}
