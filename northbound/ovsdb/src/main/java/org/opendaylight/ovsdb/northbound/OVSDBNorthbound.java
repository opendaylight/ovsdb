/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.northbound;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.BadRequestException;
import org.opendaylight.controller.northbound.commons.exception.ResourceConflictException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides REST APIs to Create, Read, Update and Delete OVSDB Row in any of the ovsdb table
 * database one at a time.
 */

@Path("/")
public class OVSDBNorthbound {
    protected static final Logger logger = LoggerFactory.getLogger(OVSDBNorthbound.class);

    @Context
    private UriInfo _uriInfo;
    private String username;

    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) {
            username = context.getUserPrincipal().getName();
        }
    }

    protected String getUserName() {
        return username;
    }

    private void handleNameMismatch(String name, String nameinURL) {
        if (name == null || nameinURL == null) {
            throw new BadRequestException(RestMessages.INVALIDDATA.toString() + " : Name is null");
        }

        if (name.equalsIgnoreCase(nameinURL)) {
            return;
        }
        throw new ResourceConflictException(RestMessages.INVALIDDATA.toString()
                + " : Table Name in URL does not match the row name in request body");
    }

    /**
     * Create a Row
     *
     * @param tableName name of the ovsdb table
     * @param row the {@link OVSDBRow} Row that is being inserted
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * https://localhost/controller/nb/v2/ovsdb/tables/bridge/rows
     * </pre>
     */

    @Path("/node/{nodeType}/{nodeId}/tables/{tableName}/rows")
    @POST
    @StatusCodes({ @ResponseCode(code = 201, condition = "Row Inserted successfully"),
        @ResponseCode(code = 400, condition = "Invalid data passed"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation")})
    @Consumes({ MediaType.APPLICATION_JSON})
    public Response addRow(@PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
                           @PathParam("tableName") String tableName, @TypeHint(OVSDBRow.class) OVSDBRow row) {

        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation");
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (row != null && row.getRow() != null) {
            handleNameMismatch(tableName, row.getRow().getTableName().getName());
            Node node = Node.fromString(nodeType, nodeId);
            Status statusWithUUID = ovsdbTable.insertRow(node, tableName, row.getParent_uuid(), row.getRow());

            if (statusWithUUID.isSuccess()) {
                return Response.status(Response.Status.CREATED)
                        .header("Location", String.format("%s/%s", _uriInfo.getAbsolutePath().toString(),
                                                                    statusWithUUID.getDescription()))
                        .entity(statusWithUUID.getDescription())
                        .build();
            } else {
                return NorthboundUtils.getResponse(statusWithUUID);
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Update a Row
     *
     * @param node OVSDB Node identifier
     * @param tableName name of the ovsdb table
     * @param rowUuid UUID of the row being updated
     * @param row the {@link OVSDBRow} Row that is being inserted
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * https://localhost/controller/nb/v2/ovsdb/tables/bridge/rows
     * </pre>
     */

    @Path("/node/{nodeType}/{nodeId}/tables/{tableName}/rows/{rowUuid}")
    @PUT
    @StatusCodes({ @ResponseCode(code = 200, condition = "Row Updated successfully"),
        @ResponseCode(code = 400, condition = "Invalid data passed"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation")})
    @Consumes({ MediaType.APPLICATION_JSON})
    public Response updateRow(@PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
                           @PathParam("tableName") String tableName, @PathParam("rowUuid") String rowUuid,
                           @TypeHint(OVSDBRow.class) OVSDBRow row) {

        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation");
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (row != null && row.getRow() != null) {
            handleNameMismatch(tableName, row.getRow().getTableName().getName());
            Node node = Node.fromString(nodeType, nodeId);
            Status status = ovsdbTable.updateRow(node, rowUuid, row.getRow());
            return NorthboundUtils.getResponse(status);
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Delete a row
     *
     * @param tableName name of the ovsdb table
     * @param uuid UUID of the Row to be removed
     * @return Response as dictated by the HTTP Response Status code
     *
     * <pre>
     * Example:
     *
     * Request URL:
     * https://localhost/controller/nb/v2/ovsdb/tables/bridge/rows/41ab15a9-0dab-4675-a579-63019d4bcbec
     *
     * </pre>
     */
    @Path("/node/{nodeType}/{nodeId}/tables/{tableName}/rows/{uuid}")
    @DELETE
    @StatusCodes({ @ResponseCode(code = 204, condition = "User Deleted Successfully"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The userName passed was not found"),
        @ResponseCode(code = 500, condition = "Internal Server Error : Removal of user failed"),
        @ResponseCode(code = 503, condition = "Service unavailable") })
    public Response removeRow(@PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
                              @PathParam("tableName") String tableName, @PathParam("uuid") String uuid) {
        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation");
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Node node = Node.fromString(nodeType, nodeId);
        Status status = ovsdbTable.deleteRow(node, tableName, uuid);
        if (status.isSuccess()) {
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }
}
