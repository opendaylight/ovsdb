/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.northbound;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.northbound.rev150406.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.northbound.rev150406.OvsdbTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.northbound.rev150406.OvsdbTablesAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;

public class NorthboundUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NorthboundUtil.class);

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
            result =  instanceIdentifierCodec.bindingDeserializer(iidString);
        } catch (DeserializationException e) {
            LOG.warn("Unable to deserialize iidString",e);
        }
        return result;
    }

    public static Optional<OvsdbTablesAugmentation> getManagingNode(DataBroker db, OvsdbTables mn) {
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
                if (optional != null && optional.isPresent() && optional.get() instanceof Node) {
                    OvsdbTablesAugmentation ovsdbNode = optional.get().getAugmentation(OvsdbTablesAugmentation.class);
                    if (ovsdbNode != null) {
                        return Optional.of(ovsdbNode);
                    } else {
                        LOG.warn("OvsdbManagedNode {} claims to be managed by {} but "
                                + "that OvsdbNode does not exist", mn,ref.getValue());
                        return Optional.absent();
                    }
                } else {
                    LOG.warn("Mysteriously got back a thing which is *not* a topology Node: {}",optional);
                    return Optional.absent();
                }
            } else {
                LOG.warn("Cannot find client for OvsdbManagedNode without a specified ManagedBy {}",mn);
                return Optional.absent();
            }
        } catch (Exception e) {
            LOG.warn("Failed to get OvsdbNode that manages OvsdbManagedNode {}", mn, e);
            return Optional.absent();
        }
    }
}
