/*
 * Copyright (C) 2014 Red Hat, Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.northbound;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Table;
import org.opendaylight.ovsdb.lib.table.Tables;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.StatusWithUuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* OVSDB Northbound REST API.<br>
* This class provides REST APIs to Create, Read, Update and Delete OVSDB Row in any of the ovsdb table
* database one at a time. The JSON used to create rows is in the same format as the OVSDB JSON-RPC messages.
* This format is documented in the <a href="http://openvswitch.org/ovs-vswitchd.conf.db.5.pdf">OVSDB Schema</a>
* and in <a href="http://tools.ietf.org/rfc/rfc7047.txt">RFC 7047</a>.
*
* <br>
* <br>
* Authentication scheme : <b>HTTP Basic</b><br>
* Authentication realm : <b>opendaylight</b><br>
* Transport : <b>HTTP and HTTPS</b><br>
* <br>
* HTTPS Authentication is disabled by default.
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

    private String getOVSTableName(String tableName) {
        List<Table> tables = Tables.getTables();
        for (Table table : tables) {
            if (table.getTableName().getName().equalsIgnoreCase(tableName)) {
                return table.getTableName().getName();
            }
        }
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Create a Row
     *
     * @param nodeType type of node e.g OVS
     * @param nodeId ID of the node
     * @param tableName name of the OVSDB table
     * @param row the {@link OVSDBRow} Row that is being inserted
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     * <br>
     * Examples:
     * <br>
     * Create a Bridge Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/bridge/rows
     *
     * JSON:
     * {
     *   "row": {
     *     "Bridge": {
     *       "name": "bridge1",
     *       "datapath_type": "OPENFLOW"
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create a Port Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/port/rows
     *
     * JSON:
     * {
     *   "parent_uuid": "b01cd26b-9c63-4216-8cf2-55f7087adab1",
     *   "row": {
     *     "Port": {
     *       "name": "port1",
     *       "mac": [
     *         "set",
     *         "00:00:00:00:00:01"
     *       ],
     *       "tag": [
     *         "set",
     *         200
     *       ]
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create an Interface Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/interface/rows
     *
     * JSON:
     * {
     *   "parent_uuid": "c7b54c9b-9b25-4801-a81d-d7bc489d4840",
     *   "row": {
     *     "Interface": {
     *       "name": "br2",
     *       "mac": [
     *         "set",
     *         "00:00:bb:bb:00:01"
     *       ],
     *       "admin_state": "up"
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create an SSL Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/SSL/rows
     *
     * JSON:
     * {
     *   "row": {
     *     "SSL": {
     *       "name": "mySSL",
     *       "ca_cert": "ca_cert",
     *       "bootstrap_ca_cert": true,
     *       "certificate": "pieceofpaper",
     *       "private_key": "private"
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create an sFlow Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/sflow/rows
     *
     * JSON:
     * {
     *   "parent_uuid": "6b3072ba-a120-4db9-82f8-a8ce4eae6942",
     *   "row": {
     *     "sFlow": {
     *       "agent": [
     *         "set",
     *         "agent_string"
     *       ],
     *       "targets": [
     *         "set",
     *         "targets_string"
     *       ]
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create a QoS Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/qos/rows
     *
     * JSON:
     * {
     *   "parent_uuid": "b109dbcf-47bb-4121-b244-e623b3421d6e",
     *   "row": {
     *     "QoS": {
     *       "type": "linux-htb"
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create a Queue Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/queue/rows
     *
     * {
     *   "parent_uuid": "b16eae7d-7e97-46d2-95d1-333d1de4a3d7",
     *   "row": {
     *     "Queue": {
     *       "dscp": [
     *         "set",
     *         "25"
     *       ]
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create a Netflow Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/netflow/rows
     *
     * JSON:
     * {
     *   "parent_uuid": "b01cd26b-9c63-4216-8cf2-55f7087adab1",
     *   "row": {
     *     "NetFlow": {
     *       "targets": [
     *         "set",
     *         [
     *           "192.168.1.102:9998"
     *         ]
     *       ],
     *       "active_timeout": "0"
     *     }
     *   }
     * }
     * </pre>
     *
     *
     * Create a Manager Row:
     * <pre>
     *
     * Request URL:
     * POST http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/manager/rows
     *
     * JSON:
     * {
     *   "parent_uuid": "8d3fb89b-5fac-4631-a990-f5a4e7f5383a",
     *   "row": {
     *     "Manager": {
     *       "target": "a_string",
     *       "is_connected": true,
     *       "state": "active"
     *     }
     *   }
     * }
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

        String ovsTableName = getOVSTableName(tableName);
        if (ovsTableName == null) {
            Status status = new Status(StatusCode.NOTFOUND, "Table "+tableName+" is not currently supported");
            return NorthboundUtils.getResponse(status);
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("OVS Configuration Service " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (row != null && row.getRow() != null) {
            handleNameMismatch(tableName, row.getRow().getTableName().getName());
            Node node = Node.fromString(nodeType, nodeId);
            StatusWithUuid statusWithUUID = ovsdbTable.insertRow(node, ovsTableName, row.getParent_uuid(), row.getRow());

            if (statusWithUUID.isSuccess()) {
                UUID uuid = statusWithUUID.getUuid();
                return Response.status(Response.Status.CREATED)
                        .header("Location", String.format("%s/%s", _uriInfo.getAbsolutePath().toString(),
                                                                    uuid.toString()))
                        .entity(uuid.toString())
                        .build();
            } else {
                return NorthboundUtils.getResponse(statusWithUUID);
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Read a Row
     *
     * @param nodeType type of node e.g OVS
     * @param nodeId ID of the node
     * @param tableName name of the ovsdb table
     * @param rowUuid UUID of the row being read
     *
     * @return Row corresponding to the UUID.
     *
     * <br>
     * Examples:
     * <br>
     * <pre>
     * Get a specific Bridge Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/bridge/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific Port Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/port/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific Interface Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/interface/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific Controller Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/controller/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific SSL Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/SSL/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific sFlow Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/sflow/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific QoS Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/qos/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific Queue Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/queue/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific Netflow Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/netflow/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Get a specific Manager Row:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/manager/rows/6f4c602c-026f-4390-beea-d50d6d448100
     * </pre>
     */

    @Path("/node/{nodeType}/{nodeId}/tables/{tableName}/rows/{rowUuid}")
    @GET
    @StatusCodes({ @ResponseCode(code = 200, condition = "Row Updated successfully"),
        @ResponseCode(code = 400, condition = "Invalid data passed"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation")})
    @Consumes({ MediaType.APPLICATION_JSON})
    @TypeHint(OVSDBRow.class)
    public OVSDBRow getRow(@PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
                           @PathParam("tableName") String tableName, @PathParam("rowUuid") String rowUuid) {

        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation");
        }

        String ovsTableName = getOVSTableName(tableName);
        if (ovsTableName == null) return null;

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = Node.fromString(nodeType, nodeId);
        Table<?> row = null;
        try {
            row = ovsdbTable.getRow(node, ovsTableName, rowUuid);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        return new OVSDBRow(null, row);
    }

    /**
     * Read all Rows of a table
     *
     * @param nodeType type of node e.g OVS
     * @param nodeId ID of the node
     * @param tableName name of the ovsdb table
     *
     * @return All the Rows of a table
     *
     * <br>
     * Examples:
     * <br>
     * <pre>
     * Get all Bridge Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/bridge/rows
     *
     * Get all Port Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/port/rows
     *
     * Get all Interface Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/interface/rows
     *
     * Get all Controller Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/controller/rows
     *
     * Get all SSL Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/SSL/rows
     *
     * Get all sFlow Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/sflow/rows
     *
     * Get all QoS Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/qos/rows
     *
     * Get all Queue Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/queue/rows
     *
     * Get all Netflow Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/netflow/rows
     *
     * Get all Manager Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/manager/rows
     *
     * Get all Open vSwitch Rows:
     * GET http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/open_vswitch/rows
     * </pre>
     */

    @Path("/node/{nodeType}/{nodeId}/tables/{tableName}/rows")
    @GET
    @StatusCodes({ @ResponseCode(code = 200, condition = "Row Updated successfully"),
        @ResponseCode(code = 400, condition = "Invalid data passed"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation")})
    @Consumes({ MediaType.APPLICATION_JSON})
    @TypeHint(OVSDBRows.class)
    public OVSDBRows getAllRows(@PathParam("nodeType") String nodeType, @PathParam("nodeId") String nodeId,
                               @PathParam("tableName") String tableName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), "default", Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation");
        }

        String ovsTableName = getOVSTableName(tableName);
        if (ovsTableName == null) return null;

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        Node node = Node.fromString(nodeType, nodeId);
        Map<String, Table<?>> rows = null;
        try {
            rows = ovsdbTable.getRows(node, ovsTableName);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        return new OVSDBRows(rows);
    }

    /**
     * Update a Row
     *
     * @param nodeType type of node e.g OVS
     * @param nodeId ID of the node
     * @param tableName name of the ovsdb table
     * @param rowUuid UUID of the row being updated
     * @param row the {@link OVSDBRow} Row that is being updated
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     * <br>
     * Examples:
     * <br>
     * Update the Bridge row to add a controller
     * <pre>
     *
     * Request URL:
     * PUT http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/bridge/rows/b01cd26b-9c63-4216-8cf2-55f7087adab1
     *
     * JSON:
     * {
     *   "row": {
     *     "Bridge": {
     *       "controller": [
     *         "set",
     *         [
     *           [
     *             "uuid",
     *             "a566e8b4-fc38-499b-8623-6087d5b36b72"
     *           ]
     *         ]
     *       ]
     *     }
     *   }
     * }
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

        String ovsTableName = getOVSTableName(tableName);
        if (ovsTableName == null) {
            Status status = new Status(StatusCode.NOTFOUND, "Table "+tableName+" is not currently supported");
            return NorthboundUtils.getResponse(status);
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class,
                                                                                            this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        if (row != null && row.getRow() != null) {
            handleNameMismatch(tableName, row.getRow().getTableName().getName());
            Node node = Node.fromString(nodeType, nodeId);
            Status status = ovsdbTable.updateRow(node, ovsTableName, row.getParent_uuid(), rowUuid, row.getRow());
            return NorthboundUtils.getResponse(status);
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * Delete a row
     *
     * @param nodeType type of node e.g OVS
     * @param nodeId ID of the node
     * @param tableName name of the ovsdb table
     * @param uuid UUID of the Row to be removed
     *
     * @return Response as dictated by the HTTP Response Status code
     *
     * <br>
     * Examples:
     * <br>
     * <pre>
     * Delete a specific Bridge Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/bridge/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific Port Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/port/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific Interface Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/interface/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific Controller Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/controller/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific SSL Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/SSL/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific sFlow Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/sflow/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific QoS Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/qos/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific Queue Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/queue/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific Netflow Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/netflow/rows/6f4c602c-026f-4390-beea-d50d6d448100
     *
     * Delete a specific Manager Row:
     * DELETE http://localhost:8080/ovsdb/nb/v2/node/OVS/HOST1/tables/manager/rows/6f4c602c-026f-4390-beea-d50d6d448100
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

        String ovsTableName = getOVSTableName(tableName);
        if (ovsTableName == null) {
            Status status = new Status(StatusCode.NOTFOUND, "Table "+tableName+" is not currently supported");
            return NorthboundUtils.getResponse(status);
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        if (ovsdbTable == null) {
            throw new ServiceUnavailableException("UserManager " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Node node = Node.fromString(nodeType, nodeId);
        Status status = ovsdbTable.deleteRow(node, ovsTableName, uuid);
        if (status.isSuccess()) {
            return Response.noContent().build();
        }
        return NorthboundUtils.getResponse(status);
    }
}
