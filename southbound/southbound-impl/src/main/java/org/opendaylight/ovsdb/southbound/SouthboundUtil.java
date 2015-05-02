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
import static org.opendaylight.ovsdb.lib.operations.Operations.op;
import static org.opendaylight.ovsdb.southbound.SouthboundConstants.EXTERNAL_IDS_COLUMN_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

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

    public static Map<String, String> getOpenVswitchExternalIds(OvsdbClient client) {
        Map<String, String> externalIds = new HashMap<String, String>();
        try {
            List<String> databases = client.getDatabases().get();
            for (String database : databases) {
                DatabaseSchema dbSchema = client.getSchema(database).get();
                TransactionBuilder transactionBuilder = new TransactionBuilder(client,dbSchema);
                OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(dbSchema, OpenVSwitch.class);
                //need to set empty map else ovs.getExternalIdsColumn().getSchema() gives null pointer
                ovs.setExternalIds(Maps.<String, String>newHashMap());
                Select<GenericTableSchema> ovsSelect = op.select(ovs.getSchema());
                List<String> columns = new ArrayList<String>();
                columns.add(EXTERNAL_IDS_COLUMN_NAME);
                ovsSelect.setColumns(columns);
                transactionBuilder.add(ovsSelect);
                ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
                List<OperationResult> resultList = results.get();
                if (resultList == null || resultList.isEmpty()) {
                    return externalIds;
                }
                for (OperationResult result : resultList) {
                    List<Row<GenericTableSchema>> rows = result.getRows();
                    if (rows == null || rows.isEmpty()) {
                        return externalIds;
                    }
                    for (Row<GenericTableSchema> row : rows) {
                        Column<GenericTableSchema, Map<String, String>> column =
                                row.getColumn(ovs.getExternalIdsColumn().getSchema());
                        if (null != column) {
                            //Presently there is no known use case wherein one ovs could have multiple externalIds
                            externalIds = column.getData();
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Cannot fetch externalIds from ovsdb. Using connection info for node ID" + e);
        }
        LOG.trace("External Ids from ovs: " + externalIds);
        return externalIds;
    }
}
