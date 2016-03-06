/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TransactUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactUtils.class);

    public static Map<InstanceIdentifier<Node>,Node> extractNode(
            Map<InstanceIdentifier<?>, DataObject> changes) {
        Map<InstanceIdentifier<Node>,Node> result
            = new HashMap<>();
        if (changes != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (created.getValue() instanceof Node) {
                    Node value = (Node) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(Node.class)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractCreated(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,Class<T> klazz) {
        return extract(changes.getCreatedData(),klazz);
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractUpdated(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        return extract(changes.getUpdatedData(),klazz);
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractCreatedOrUpdated(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        Map<InstanceIdentifier<T>,T> result = extractUpdated(changes,klazz);
        result.putAll(extractCreated(changes,klazz));
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractCreatedOrUpdatedOrRemoved(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Class<T> klazz) {
        Map<InstanceIdentifier<T>,T> result = extractCreatedOrUpdated(changes,klazz);
        result.putAll(extractRemovedObjects(changes, klazz));
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractOriginal(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        return extract(changes.getOriginalData(),klazz);
    }

    public static <T extends DataObject> Set<InstanceIdentifier<T>> extractRemoved(
            AsyncDataChangeEvent<InstanceIdentifier<?>,DataObject> changes,Class<T> klazz) {
        Set<InstanceIdentifier<T>> result = new HashSet<>();
        if (changes != null && changes.getRemovedPaths() != null) {
            for (InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
                if (iid.getTargetType().equals(klazz)) {
                    @SuppressWarnings("unchecked") // Actually checked above
                    InstanceIdentifier<T> iidn = (InstanceIdentifier<T>)iid;
                    result.add(iidn);
                }
            }
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractRemovedObjects(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes,
            Class<T> klazz) {
        Set<InstanceIdentifier<T>> iids = extractRemoved(changes, klazz);
        return Maps.filterKeys(extractOriginal(changes, klazz),Predicates.in(iids));
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extract(
            Map<InstanceIdentifier<?>, DataObject> changes, Class<T> klazz) {
        Map<InstanceIdentifier<T>,T> result = new HashMap<>();
        if (changes != null) {
            for (Entry<InstanceIdentifier<?>, DataObject> created : changes.entrySet()) {
                if (klazz.isInstance(created.getValue())) {
                    @SuppressWarnings("unchecked")
                    T value = (T) created.getValue();
                    Class<?> type = created.getKey().getTargetType();
                    if (type.equals(klazz)) {
                        @SuppressWarnings("unchecked") // Actually checked above
                        InstanceIdentifier<T> iid = (InstanceIdentifier<T>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }

    public static List<Insert> extractInsert(TransactionBuilder transaction, GenericTableSchema schema) {
        List<Operation> operations = transaction.getOperations();
        List<Insert> inserts = new ArrayList<>();
        for (Operation operation : operations) {
            if (operation instanceof Insert && operation.getTableSchema().equals(schema)) {
                inserts.add((Insert) operation);
            }
        }
        return inserts;
    }

    /**
     * Extract the NamedUuid from the Insert.
     * If the Insert does not have a NamedUuid set, a random one will be
     * generated, set, and returned.
     *
     * @param insert - Insert from which to extract the NamedUuid
     * @return UUID - NamedUUID of the Insert
     */
    public static UUID extractNamedUuid(Insert insert) {
        String uuidString = insert.getUuidName() != null
                ? insert.getUuidName() : SouthboundMapper.getRandomUUID();
        insert.setUuidName(uuidString);
        return new UUID(uuidString);
    }

    public static <T  extends TableSchema<T>> void stampInstanceIdentifier(TransactionBuilder transaction,
            InstanceIdentifier<?> iid, TableSchema<T> tableSchema,
            ColumnSchema<T, Map<String,String>> columnSchema) {
        transaction.add(stampInstanceIdentifierMutation(transaction,iid,
                tableSchema,columnSchema));
    }

    public static <T  extends TableSchema<T>> Mutate<T> stampInstanceIdentifierMutation(TransactionBuilder transaction,
            InstanceIdentifier<?> iid, TableSchema<T> tableSchema,
            ColumnSchema<T, Map<String,String>> columnSchema) {
        Map<String,String> externalIdsMap = ImmutableMap.of(SouthboundConstants.IID_EXTERNAL_ID_KEY,
                SouthboundUtil.serializeInstanceIdentifier(iid));
        Mutate<T> mutate = op.mutate(tableSchema)
                .addMutation(columnSchema,
                    Mutator.INSERT,
                    externalIdsMap);
        Mutation deleteIidMutation = new Mutation(columnSchema.getName(),
                Mutator.DELETE,
                OvsdbSet.fromSet(Sets.newHashSet(SouthboundConstants.IID_EXTERNAL_ID_KEY)));
        List<Mutation> mutations = Lists.newArrayList(Sets.newHashSet(deleteIidMutation));
        mutations.addAll(mutate.getMutations());
        mutate.setMutations(mutations);
        return mutate;
    }
}
