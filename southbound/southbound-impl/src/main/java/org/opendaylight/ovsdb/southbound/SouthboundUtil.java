/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FluentFuture;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortExternalIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SouthboundUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtil.class);
    private static final String SCHEMA_VERSION_MISMATCH =
            "{} column for {} table is not supported by this version of the {} schema: {}";
    private static final InterfaceExternalIds INTERACE_CREATED_BY_OPENDAYLIGHT = createExternalIdsForInterface(
        SouthboundConstants.CREATED_BY, SouthboundConstants.ODL);
    private static final PortExternalIds PORT_CREATED_BY_OPENDAYLIGHT = createExternalIdsForPort(
        SouthboundConstants.CREATED_BY, SouthboundConstants.ODL);

    private SouthboundUtil() {
        // Prevent instantiating a utility class
    }

    public static Optional<OvsdbNodeAugmentation> getManagingNode(DataBroker db, OvsdbBridgeAttributes mn) {
        requireNonNull(mn);
        try {
            OvsdbNodeRef ref = mn.getManagedBy();
            if (ref != null && ref.getValue() != null) {
                FluentFuture<Optional<Node>> nf;
                try (ReadTransaction transaction = db.newReadOnlyTransaction()) {
                    @SuppressWarnings("unchecked")
                    // Note: erasure makes this safe in combination with the typecheck below
                    InstanceIdentifier<Node> path = (InstanceIdentifier<Node>) ref.getValue();
                    nf = transaction.read(LogicalDatastoreType.OPERATIONAL, path);
                }

                Optional<Node> optional = nf.get();
                if (optional != null && optional.isPresent()) {
                    OvsdbNodeAugmentation ovsdbNode = null;
                    Node node = optional.orElseThrow();
                    if (node instanceof OvsdbNodeAugmentation) {
                        ovsdbNode = (OvsdbNodeAugmentation) node;
                    } else if (node != null) {
                        ovsdbNode = node.augmentation(OvsdbNodeAugmentation.class);
                    }
                    if (ovsdbNode != null) {
                        return Optional.of(ovsdbNode);
                    } else {
                        LOG.warn("OvsdbManagedNode {} claims to be managed by {} but "
                                + "that OvsdbNode does not exist", mn, ref.getValue());
                        return Optional.empty();
                    }
                } else {
                    LOG.warn("Mysteriously got back a thing which is *not* a topology Node: {}", optional);
                    return Optional.empty();
                }
            } else {
                LOG.warn("Cannot find client for OvsdbManagedNode without a specified ManagedBy {}", mn);
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to get OvsdbNode that manages OvsdbManagedNode {}", mn, e);
            return Optional.empty();
        }
    }

    public static <D extends DataObject> Optional<D> readNode(ReadWriteTransaction transaction,
        InstanceIdentifier<D> connectionIid) {
        Optional<D> node;
        try {
            Node cachedNode = OvsdbOperGlobalListener.OPER_NODE_CACHE.get(connectionIid);
            if (cachedNode != null) {
                node = Optional.of((D)cachedNode);
            } else {
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, connectionIid).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Read Operational/DS for Node failed! {}", connectionIid, e);
            throw new IllegalStateException(e);
        }
        return node;
    }

    public static <D extends DataObject> Optional<D> readNode(ReadTransaction transaction,
                                                              InstanceIdentifier<D> connectionIid) {
        Optional<D> node;
        try {
            Node cachedNode = OvsdbOperGlobalListener.OPER_NODE_CACHE.get(connectionIid);
            if (cachedNode != null) {
                node = Optional.of((D)cachedNode);
            } else {
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, connectionIid).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Read Operational/DS for Node failed! {}", connectionIid, e);
            throw new IllegalStateException(e);
        }
        return node;
    }

    @VisibleForTesting
    static String getLocalControllerHostIpAddress() {
        String ipaddress = null;
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces != null) {
                while (ifaces.hasMoreElements()) {
                    NetworkInterface iface = ifaces.nextElement();

                    for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                        InetAddress inetAddr = inetAddrs.nextElement();
                        if (!inetAddr.isLoopbackAddress() && inetAddr.isSiteLocalAddress()) {
                            ipaddress = inetAddr.getHostAddress();
                            break;
                        }
                    }
                }
            } else {
                LOG.warn("Local Host don't have any associated IP address");
            }
        } catch (SocketException e) {
            LOG.warn("Exception while fetching local host ip address ",e);
        }
        return ipaddress;
    }

    public static String getControllerTarget(Node ovsdbNode) {
        String ipAddr = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.augmentation(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
        LOG.info("connectionInfo: {}", connectionInfo);
        if (connectionInfo != null && connectionInfo.getLocalIp() != null) {
            ipAddr = connectionInfo.getLocalIp().stringValue();
        }
        if (ipAddr == null) {
            ipAddr = getLocalControllerHostIpAddress();
        }

        if (ipAddr != null) {
            return SouthboundConstants.OPENFLOW_CONNECTION_PROTOCOL + ":"
                    + ipAddr + ":" + SouthboundConstants.DEFAULT_OPENFLOW_PORT;
        }

        return null;
    }

    public static String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return connectionInfo.getRemoteIp().stringValue() + ":" + connectionInfo.getRemotePort().getValue();
    }

    public static void schemaMismatchLog(String column, String table, SchemaVersionMismatchException ex) {
        LOG.debug(SCHEMA_VERSION_MISMATCH, column, table, SouthboundConstants.OPEN_V_SWITCH, ex.getMessage());
    }

    public static PortExternalIds createExternalIdsForPort(String key, String value) {
        return new PortExternalIdsBuilder()
            .setExternalIdKey(key)
            .setExternalIdValue(value).build();
    }

    public static InterfaceExternalIds createExternalIdsForInterface(String key, String value) {
        return new InterfaceExternalIdsBuilder()
            .setExternalIdKey(key)
            .setExternalIdValue(value).build();
    }

    public static InterfaceExternalIds interfaceCreatedByOpenDaylight() {
        return INTERACE_CREATED_BY_OPENDAYLIGHT;
    }

    public static PortExternalIds portCreatedByOpenDaylight() {
        return PORT_CREATED_BY_OPENDAYLIGHT;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static String getOvsdbNodeId(InstanceIdentifier<Node> nodeIid) {
        String nodeId = "";
        if (nodeIid != null) {
            try {
                nodeId = nodeIid.toString();
                nodeId = nodeIid.firstKeyOf(Node.class).getNodeId().getValue();
            } catch (Exception exp) {
                LOG.debug("Exception in getting the value from {} ", nodeIid);
            }
        }
        return nodeId;
    }

    public static String getBridgeNameFromOvsdbNodeId(InstanceIdentifier<Node> nodeIid) {
        String nodeId = getOvsdbNodeId(nodeIid);
        if (nodeId != null && !nodeId.isEmpty() && nodeId.contains("bridge")
                && nodeId.lastIndexOf("bridge") + 7 < nodeId.length()) {
            return nodeId.substring(nodeId.indexOf("bridge") + 7);// to fetch bridge name ex: "/bridge/br-int"
        } else {
            return null;
        }
    }
}
