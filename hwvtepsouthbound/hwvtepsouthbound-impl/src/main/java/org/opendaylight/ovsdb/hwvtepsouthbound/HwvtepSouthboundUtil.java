/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HwvtepSouthboundUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundUtil.class);
    private static final String SCHEMA_VERSION_MISMATCH =
            "{} column for {} table is not supported by this version of the {} schema: {}";

    private static InstanceIdentifierCodec instanceIdentifierCodec;

    private static ScheduledExecutorService scheduledExecutorService = Executors
            .newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("hwvteputil-executor-service-%d").build());

    private HwvtepSouthboundUtil() {
        // Prevent instantiating a utility class
    }

    // FIXME: eliminate this static wiring by encaspulating the codec into a service
    @Deprecated
    public static void setInstanceIdentifierCodec(InstanceIdentifierCodec iidc) {
        instanceIdentifierCodec = iidc;
    }

    // FIXME: this should be an instance method
    public static InstanceIdentifierCodec getInstanceIdentifierCodec() {
        return instanceIdentifierCodec;
    }

    // FIXME: this should be an instance method
    public static String serializeInstanceIdentifier(InstanceIdentifier<?> iid) {
        return instanceIdentifierCodec.serialize(iid);
    }

    // FIXME: this should be an instance method
    public static InstanceIdentifier<?> deserializeInstanceIdentifier(String iidString) {
        InstanceIdentifier<?> result = null;
        try {
            result = instanceIdentifierCodec.bindingDeserializer(iidString);
        } catch (DeserializationException e) {
            LOG.warn("Unable to deserialize iidString", e);
        }
        return result;
    }

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readNode(
            DataBroker db,
            LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> connectionIid) {
        if (logicalDatastoreType == LogicalDatastoreType.OPERATIONAL) {
            Node node = HwvtepOperGlobalListener.getNode((InstanceIdentifier<Node>) connectionIid);
            if (node != null) {
                return Optional.of((D)node);
            } else {
                LOG.debug("Node not available in cache. Read from datastore - {}", connectionIid);
            }
        }
        try (ReadTransaction transaction = db.newReadOnlyTransaction()) {
            return transaction.read(logicalDatastoreType, connectionIid).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Read failed from datastore for Node : {}",connectionIid,e);
            throw new RuntimeException(e);
        }
    }

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readNode(
            ReadTransaction transaction,
            LogicalDatastoreType logicalDatastoreType, final InstanceIdentifier<D> connectionIid) {
        if (logicalDatastoreType == LogicalDatastoreType.OPERATIONAL) {
            Node node = HwvtepOperGlobalListener.getNode((InstanceIdentifier<Node>) connectionIid);
            if (node != null) {
                return Optional.of((D)node);
            } else {
                LOG.debug("Node not available in cache. Read from datastore - {}", connectionIid);
            }
        }
        try {
            return transaction.read(logicalDatastoreType, connectionIid).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Read failed from datastore for Node : {}",connectionIid,e);
            throw new RuntimeException(e);
        }
    }

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> Optional<D> readNode(
            ReadWriteTransaction transaction, final InstanceIdentifier<D> connectionIid) {
        return readNode(transaction, LogicalDatastoreType.OPERATIONAL, connectionIid);
    }

    public static <D extends DataObject> Optional<D> readNode(ReadWriteTransaction transaction,
                                                              LogicalDatastoreType logicalDatastoreType,
                                                              InstanceIdentifier<D> connectionIid) {
        if (logicalDatastoreType == LogicalDatastoreType.OPERATIONAL) {
            Node node = HwvtepOperGlobalListener.getNode((InstanceIdentifier<Node>) connectionIid);
            if (node != null) {
                return Optional.of((D)node);
            } else {
                LOG.debug("Node not available in cache. Read from datastore - {}", connectionIid);
            }
        }
        try {
            return transaction.read(logicalDatastoreType, connectionIid).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Read failed from datastore for Node : {}",connectionIid,e);
            throw new RuntimeException(e);
        }
    }

    public static Optional<HwvtepGlobalAugmentation> getManagingNode(DataBroker db,
                    HwvtepPhysicalSwitchAttributes node) {
        Optional<HwvtepGlobalAugmentation> result = null;
        HwvtepGlobalRef ref = requireNonNull(node).getManagedBy();
        if (ref != null && ref.getValue() != null) {
            result = getManagingNode(db, ref);
        } else {
            LOG.warn("Cannot find client for PhysicalSwitch without a specified ManagedBy {}", node);
            return Optional.empty();
        }
        if (!result.isPresent()) {
            LOG.warn("Failed to find managing node for PhysicalSwitch {}", node);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static Optional<HwvtepGlobalAugmentation> getManagingNode(DataBroker db, HwvtepGlobalRef ref) {
        try {
            @SuppressWarnings("unchecked")
            // Note: erasure makes this safe in combination with the typecheck
            // below
            InstanceIdentifier<Node> path = (InstanceIdentifier<Node>) ref.getValue();

            Optional<Node> optional = new MdsalUtils(db).readOptional(LogicalDatastoreType.OPERATIONAL, path);
            if (optional != null && optional.isPresent()) {
                HwvtepGlobalAugmentation hwvtepNode = null;
                Node node = optional.get();
                if (node instanceof HwvtepGlobalAugmentation) {
                    hwvtepNode = (HwvtepGlobalAugmentation) node;
                } else if (node != null) {
                    hwvtepNode = node.augmentation(HwvtepGlobalAugmentation.class);
                }
                if (hwvtepNode != null) {
                    return Optional.of(hwvtepNode);
                } else {
                    LOG.warn("Hwvtep switch claims to be managed by {} but " + "that HwvtepNode does not exist",
                                    ref.getValue());
                    return Optional.empty();
                }
            } else {
                LOG.warn("Mysteriously got back a thing which is *not* a topology Node: {}", optional);
                return Optional.empty();
            }
        } catch (RuntimeException e) {
            LOG.warn("Failed to get HwvtepNode {}", ref, e);
            return Optional.empty();
        }
    }

    public static String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return connectionInfo.getRemoteIp().stringValue() + ":" + connectionInfo.getRemotePort().getValue();
    }


    public static void schemaMismatchLog(String column, String table, SchemaVersionMismatchException ex) {
        LOG.debug(SCHEMA_VERSION_MISMATCH, column, table, "hw_vtep", ex.getMessage());
    }

    public static <K, D> void updateData(Map<Class<? extends Identifiable>, Map<K, D>> map,
            Class<? extends Identifiable> cls, K key, D data) {
        LOG.debug("Updating data {} {} {}", cls, key, data);
        if (key == null) {
            return;
        }
        if (!map.containsKey(cls)) {
            map.put(cls, new ConcurrentHashMap<>());
        }
        map.get(cls).put(key, data);
    }

    public static <K, D> D getData(Map<Class<? extends Identifiable>, Map<K, D>> map,
            Class<? extends Identifiable> cls, K key) {
        if (key == null) {
            return null;
        }
        if (map.containsKey(cls)) {
            return map.get(cls).get(key);
        }
        return null;
    }

    public static <K, D> boolean containsKey(Map<Class<? extends Identifiable>, Map<K, D>> map,
            Class<? extends Identifiable> cls, K key) {
        if (key == null) {
            return false;
        }
        if (map.containsKey(cls)) {
            return map.get(cls).containsKey(key);
        }
        return false;
    }

    public static <K, D> void clearData(Map<Class<? extends Identifiable>, Map<K, D>> map,
            Class<? extends Identifiable> cls, K key) {
        LOG.debug("Clearing data {} {}", cls, key);
        if (key == null) {
            return;
        }
        if (map.containsKey(cls)) {
            map.get(cls).remove(key);
        }
    }

    public static <T> boolean isEmpty(Collection<T> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isEmptyMap(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static InstanceIdentifier<Node> getGlobalNodeIid(final InstanceIdentifier<Node> physicalNodeIid) {
        String nodeId = physicalNodeIid.firstKeyOf(Node.class).getNodeId().getValue();
        int physicalSwitchIndex = nodeId.indexOf(HwvtepSouthboundConstants.PSWITCH_URI_PREFIX);
        if (physicalSwitchIndex > 0) {
            nodeId = nodeId.substring(0, physicalSwitchIndex - 1);
        } else {
            return null;
        }
        return physicalNodeIid.firstIdentifierOf(Topology.class).child(Node.class , new NodeKey(new NodeId(nodeId)));
    }

    public static Integer getRemotePort(Node node) {
        HwvtepGlobalAugmentation augmentation = node.augmentation(HwvtepGlobalAugmentation.class);
        if (augmentation != null && augmentation.getConnectionInfo() != null) {
            return augmentation.getConnectionInfo().getRemotePort().getValue().toJava();
        }
        return 0;
    }

    public static ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
}
