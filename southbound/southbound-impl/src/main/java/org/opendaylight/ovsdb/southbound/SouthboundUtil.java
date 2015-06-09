/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SouthboundUtil.class);

    private static InstanceIdentifierCodec instanceIdentifierCodec;

    public static void setInstanceIdentifierCodec(InstanceIdentifierCodec iidc) {
        instanceIdentifierCodec = iidc;
    }

    public static String serializeInstanceIdentifier(InstanceIdentifier<?> iid) {
        return instanceIdentifierCodec.serialize(iid);
    }

    public static InstanceIdentifier<?> deserializeInstanceIdentifier(String iidString) {
        InstanceIdentifier<?> result = null;
        try {
            result = instanceIdentifierCodec.bindingDeserializer(iidString);
        } catch (DeserializationException e) {
            LOG.warn("Unable to deserialize iidString", e);
        }
        return result;
    }


    public static Optional<OvsdbNodeAugmentation> getManagingNode(DataBroker db, OvsdbBridgeAttributes mn) {
        Preconditions.checkNotNull(mn);
        try {
            OvsdbNodeRef ref = mn.getManagedBy();
            if (ref != null && ref.getValue() != null) {
                ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
                @SuppressWarnings("unchecked") // Note: erasure makes this safe in combination with the typecheck below
                InstanceIdentifier<Node> path = (InstanceIdentifier<Node>) ref.getValue();

                CheckedFuture<Optional<Node>, ReadFailedException> nf = transaction.read(
                        LogicalDatastoreType.OPERATIONAL, path);
                transaction.close();
                Optional<Node> optional = nf.get();
                if (optional != null && optional.isPresent()) {
                    OvsdbNodeAugmentation ovsdbNode = null;
                    if (optional.get() instanceof Node) {
                        ovsdbNode = optional.get().getAugmentation(OvsdbNodeAugmentation.class);
                    } else if (optional.get() instanceof OvsdbNodeAugmentation) {
                        ovsdbNode = (OvsdbNodeAugmentation) optional.get();
                    }
                    if (ovsdbNode != null) {
                        return Optional.of(ovsdbNode);
                    } else {
                        LOG.warn("OvsdbManagedNode {} claims to be managed by {} but "
                                + "that OvsdbNode does not exist", mn, ref.getValue());
                        return Optional.absent();
                    }
                } else {
                    LOG.warn("Mysteriously got back a thing which is *not* a topology Node: {}", optional);
                    return Optional.absent();
                }
            } else {
                LOG.warn("Cannot find client for OvsdbManagedNode without a specified ManagedBy {}", mn);
                return Optional.absent();
            }
        } catch (Exception e) {
            LOG.warn("Failed to get OvsdbNode that manages OvsdbManagedNode {}", mn, e);
            return Optional.absent();
        }
    }

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readNode(
            ReadWriteTransaction transaction, final InstanceIdentifier<D> connectionIid) {
        Optional<D> node = Optional.absent();
        try {
            node = transaction.read(LogicalDatastoreType.OPERATIONAL, connectionIid).checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Read Operational/DS for Node failed! {}", connectionIid, e);
        }
        return node;
    }

    private static String getLocalControllerHostIpAddress() {
        String ipaddress = null;
        try {
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                 ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            ipaddress = inetAddr.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Exception while fetching local host ip address ",e);
        }
        return ipaddress;
    }

    public static String getControllerTarget(Node ovsdbNode) {
        String target = null;
        String ipAddr = null;
        OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.getAugmentation(OvsdbNodeAugmentation.class);
        ConnectionInfo connectionInfo = ovsdbNodeAugmentation.getConnectionInfo();
        LOG.info("connectionInfo: {}", connectionInfo);
        if (connectionInfo != null && connectionInfo.getLocalIp() != null) {
            ipAddr = new String(connectionInfo.getLocalIp().getValue());
        }
        if (ipAddr == null) {
            ipAddr = getLocalControllerHostIpAddress();
        }

        if (ipAddr != null) {
            target = SouthboundConstants.OPENFLOW_CONNECTION_PROTOCOL + ":"
                    + ipAddr + ":" + SouthboundConstants.DEFAULT_OPENFLOW_PORT;
        }

        return target;
    }
}
