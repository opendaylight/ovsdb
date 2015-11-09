/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class HwvtepSouthboundUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepSouthboundUtil.class);

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

    public static <D extends org.opendaylight.yangtools.yang.binding.DataObject> boolean deleteNode(
                    ReadWriteTransaction transaction, final InstanceIdentifier<D> connectionIid) {
        boolean result = false;
        transaction.delete(LogicalDatastoreType.OPERATIONAL, connectionIid);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        try {
            future.checkedGet();
            result = true;
        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to delete {} ", connectionIid, e);
        }
        return result;
    }
}
