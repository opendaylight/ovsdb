/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepSouthboundUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundUtil.class);
    private static final String SCHEMA_VERSION_MISMATCH =
            "{} column for {} table is not supported by this version of the {} schema: {}";

    private static InstanceIdentifierCodec instanceIdentifierCodec;

    private HwvtepSouthboundUtil() {
        // Prevent instantiating a utility class
    }

    public static void setInstanceIdentifierCodec(InstanceIdentifierCodec iidc) {
        instanceIdentifierCodec = iidc;
    }

    public static InstanceIdentifierCodec getInstanceIdentifierCodec() {
        return instanceIdentifierCodec;
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

    public static Optional<HwvtepGlobalAugmentation> getManagingNode(DataBroker db,
                    HwvtepPhysicalSwitchAttributes pNode) {
        Preconditions.checkNotNull(pNode);
        Optional<HwvtepGlobalAugmentation> result = null;
        HwvtepGlobalRef ref = pNode.getManagedBy();
        if (ref != null && ref.getValue() != null) {
            result = getManagingNode(db, ref);
        } else {
            LOG.warn("Cannot find client for PhysicalSwitch without a specified ManagedBy {}", pNode);
            return Optional.absent();
        }
        if (!result.isPresent()) {
            LOG.warn("Failed to find managing node for PhysicalSwitch {}", pNode);
        }
        return result;
    }

    public static Optional<HwvtepGlobalAugmentation> getManagingNode(DataBroker db, HwvtepGlobalRef ref) {
        try {
            ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
            @SuppressWarnings("unchecked")
            // Note: erasure makes this safe in combination with the typecheck
            // below
            InstanceIdentifier<Node> path = (InstanceIdentifier<Node>) ref.getValue();

            CheckedFuture<Optional<Node>, ReadFailedException> nf =
                            transaction.read(LogicalDatastoreType.OPERATIONAL, path);
            transaction.close();
            Optional<Node> optional = nf.get();
            if (optional != null && optional.isPresent()) {
                HwvtepGlobalAugmentation hwvtepNode = null;
                Node node = optional.get();
                if (node instanceof HwvtepGlobalAugmentation) {
                    hwvtepNode = (HwvtepGlobalAugmentation) node;
                } else if (node != null) {
                    hwvtepNode = node.getAugmentation(HwvtepGlobalAugmentation.class);
                }
                if (hwvtepNode != null) {
                    return Optional.of(hwvtepNode);
                } else {
                    LOG.warn("Hwvtep switch claims to be managed by {} but " + "that HwvtepNode does not exist",
                                    ref.getValue());
                    return Optional.absent();
                }
            } else {
                LOG.warn("Mysteriously got back a thing which is *not* a topology Node: {}", optional);
                return Optional.absent();
            }
        } catch (Exception e) {
            LOG.warn("Failed to get HwvtepNode {}", ref, e);
            return Optional.absent();
        }
    }
    public static String connectionInfoToString(final ConnectionInfo connectionInfo) {
        return String.valueOf(
                connectionInfo.getRemoteIp().getValue()) + ":" + connectionInfo.getRemotePort().getValue();
    }


    public static void schemaMismatchLog(String column, String table, SchemaVersionMismatchException ex) {
        LOG.debug(SCHEMA_VERSION_MISMATCH, column, table, "hw_vtep", ex.getMessage());
    }

    public static <KeyType, D> void updateData(Map<Class<? extends Identifiable>, Map<KeyType, D>> map,
                                               Class<? extends Identifiable> cls, KeyType key, D data) {
        if (key == null) {
            return;
        }
        if (!map.containsKey(cls)) {
            map.put(cls, new ConcurrentHashMap<>());
        }
        map.get(cls).put(key, data);
    }

    public static <KeyType, D> D getData(Map<Class<? extends Identifiable>, Map<KeyType, D>> map,
                                         Class<? extends Identifiable> cls, KeyType key) {
        if (key == null) {
            return null;
        }
        if (map.containsKey(cls)) {
            return map.get(cls).get(key);
        }
        return null;
    }

    public static <KeyType, D> boolean containsKey(Map<Class<? extends Identifiable>, Map<KeyType, D>> map,
                                                   Class<? extends Identifiable> cls, KeyType key) {
        if (key == null) {
            return false;
        }
        if (map.containsKey(cls)) {
            return map.get(cls).containsKey(key);
        }
        return false;
    }

    public static <KeyType, D> void clearData(Map<Class<? extends Identifiable>, Map<KeyType, D>> map,
                                              Class<? extends Identifiable> cls, KeyType key) {
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

    public static boolean isEmptyMap(Map map) {
        return map == null || map.isEmpty();
    }
}
