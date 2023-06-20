/*
 * Copyright © 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
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
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Key;
import org.opendaylight.yangtools.yang.binding.KeyAware;

// This class needs to be mocked
@SuppressWarnings("checkstyle:FinalClass")
public class TransactUtils {
    private TransactUtils() {

    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>> hasDataBefore() {
        return input -> input != null && input.getDataBefore() != null;
    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>> hasDataBeforeAndDataAfter() {
        return input -> input != null && input.getDataBefore() != null && input.getDataAfter() != null;
    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>> hasNoDataBefore() {
        return input -> input != null && input.getDataBefore() == null;
    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>> hasDataAfterAndMatchesFilter(
            final Predicate<DataObjectModification<T>> filter) {
        return input -> input != null && input.getDataAfter() != null && filter.test(input);
    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>> matchesEverything() {
        return input -> true;
    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>> modificationIsDeletion() {
        return input -> input != null && input.getModificationType() == DataObjectModification
                .ModificationType.DELETE;
    }

    private static <T extends DataObject> Predicate<DataObjectModification<T>>
        modificationIsDeletionAndHasDataBefore() {
        return input -> input != null && input.getModificationType() == DataObjectModification
                .ModificationType.DELETE && input.getDataBefore() != null;
    }

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
                        // Actually checked above
                        @SuppressWarnings("unchecked")
                        InstanceIdentifier<Node> iid = (InstanceIdentifier<Node>) created.getKey();
                        result.put(iid, value);
                    }
                }
            }
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractCreated(
            DataChangeEvent changes, Class<T> klazz) {
        return extract(changes.getCreatedData(),klazz);
    }

    /**
     * Extract all the instances of {@code clazz} which were created in the given set of modifications.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The created instances, mapped by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T> extractCreated(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        return extractCreatedOrUpdated(changes, clazz, hasNoDataBefore());
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractUpdated(
            DataChangeEvent changes, Class<T> klazz) {
        return extract(changes.getUpdatedData(),klazz);
    }

    /**
     * Extract all the instances of {@code clazz} which were updated in the given set of modifications.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The updated instances, mapped by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T> extractUpdated(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        return extractCreatedOrUpdated(changes, clazz, hasDataBeforeAndDataAfter());
    }

    /**
     * Extract all the instance of {@code clazz} which were created or updated in the given set of modifications, and
     * which satisfy the given filter.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param filter The filter the changes must satisfy.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The created or updated instances which satisfy the filter, mapped by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T> extractCreatedOrUpdated(
            Collection<DataTreeModification<U>> changes, Class<T> clazz,
            Predicate<DataObjectModification<T>> filter) {
        Map<InstanceIdentifier<T>, T> result = new HashMap<>();
        for (Map.Entry<InstanceIdentifier<T>, DataObjectModification<T>> entry : extractDataObjectModifications(changes,
                clazz, hasDataAfterAndMatchesFilter(filter)).entrySet()) {
            result.put(entry.getKey(), entry.getValue().getDataAfter());
        }
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractCreatedOrUpdated(
            DataChangeEvent changes,Class<T> klazz) {
        Map<InstanceIdentifier<T>,T> result = extractUpdated(changes,klazz);
        result.putAll(extractCreated(changes,klazz));
        return result;
    }

    /**
     * Extract all the instances of {@code clazz} which were created or updated in the given set of modifications.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The created or updated instances, mapped by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T> extractCreatedOrUpdated(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        return extractCreatedOrUpdated(changes, clazz, matchesEverything());
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractCreatedOrUpdatedOrRemoved(
            DataChangeEvent changes, Class<T> klazz) {
        Map<InstanceIdentifier<T>,T> result = extractCreatedOrUpdated(changes,klazz);
        result.putAll(extractRemovedObjects(changes, klazz));
        return result;
    }

    /**
     * Extract all the instances of {@code clazz} which were created, updated, or removed in the given set of
     * modifications. For instances which were created or updated, the new instances are returned; for instances
     * which were removed, the old instances are returned.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The created, updated or removed instances, mapped by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T>
        extractCreatedOrUpdatedOrRemoved(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        Map<InstanceIdentifier<T>, T> result = extractCreatedOrUpdated(changes, clazz);
        result.putAll(extractRemovedObjects(changes, clazz));
        return result;
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>,T> extractOriginal(
            DataChangeEvent changes, Class<T> klazz) {
        return extract(changes.getOriginalData(),klazz);
    }

    /**
     * Extract the original instances of class {@code clazz} in the given set of modifications.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The original instances, mapped by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T> extractOriginal(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        Map<InstanceIdentifier<T>, T> result = new HashMap<>();
        for (Map.Entry<InstanceIdentifier<T>, DataObjectModification<T>> entry :
                extractDataObjectModifications(changes, clazz, hasDataBefore()).entrySet()) {
            result.put(entry.getKey(), entry.getValue().getDataBefore());
        }
        return result;
    }

    public static <T extends DataObject> Set<InstanceIdentifier<T>> extractRemoved(
            DataChangeEvent changes, Class<T> klazz) {
        Set<InstanceIdentifier<T>> result = new HashSet<>();
        if (changes != null && changes.getRemovedPaths() != null) {
            for (InstanceIdentifier<?> iid : changes.getRemovedPaths()) {
                if (iid.getTargetType().equals(klazz)) {
                    // Actually checked above
                    @SuppressWarnings("unchecked")
                    InstanceIdentifier<T> iidn = (InstanceIdentifier<T>)iid;
                    result.add(iidn);
                }
            }
        }
        return result;
    }

    /**
     * Extract the instance identifier of removed instances of {@code clazz} from the given set of modifications.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The instance identifiers of removed instances.
     */
    public static <T extends DataObject, U extends DataObject> Set<InstanceIdentifier<T>> extractRemoved(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        return extractDataObjectModifications(changes, clazz, modificationIsDeletion()).keySet();
    }

    /**
     * Extract all the modifications affecting instances of {@code clazz} which are present in the given set of
     * modifications and satisfy the given filter.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param filter The filter the changes must satisfy.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The modifications, mapped by instance identifier.
     */
    private static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, DataObjectModification<T>>
        extractDataObjectModifications(Collection<DataTreeModification<U>> changes, Class<T> clazz,
                                       Predicate<DataObjectModification<T>> filter) {
        List<DataObjectModification<? extends DataObject>> dataObjectModifications = new ArrayList<>();
        List<InstanceIdentifier<? extends DataObject>> paths = new ArrayList<>();
        if (changes != null) {
            for (DataTreeModification<? extends DataObject> change : changes) {
                dataObjectModifications.add(change.getRootNode());
                paths.add(change.getRootPath().getRootIdentifier());
            }
        }
        return extractDataObjectModifications(dataObjectModifications, paths, clazz, filter);
    }

    /**
     * Extract all the modifications affecting instances of {@code clazz} which are present in the given set of
     * modifications and satisfy the given filter.
     *
     * @param changes The changes to process.
     * @param paths The paths of the changes.
     * @param clazz The class we're interested in.
     * @param filter The filter the changes must satisfy.
     * @param <T> The type of changes we're interested in.
     * @return The modifications, mapped by instance identifier.
     */
    private static <T extends DataObject> Map<InstanceIdentifier<T>, DataObjectModification<T>>
        extractDataObjectModifications(
            Collection<DataObjectModification<? extends DataObject>> changes,
            Collection<InstanceIdentifier<? extends DataObject>> paths, Class<T> clazz,
            Predicate<DataObjectModification<T>> filter) {
        Map<InstanceIdentifier<T>, DataObjectModification<T>> result = new HashMap<>();
        Queue<DataObjectModification<? extends DataObject>> remainingChanges = new LinkedList<>(changes);
        Queue<InstanceIdentifier<? extends DataObject>> remainingPaths = new LinkedList<>(paths);
        while (!remainingChanges.isEmpty()) {
            DataObjectModification<? extends DataObject> change = remainingChanges.remove();
            InstanceIdentifier<? extends DataObject> path = remainingPaths.remove();
            // Is the change relevant?
            if (clazz.isAssignableFrom(change.getDataType())) {
                @SuppressWarnings("unchecked")
                final DataObjectModification<T> dao = (DataObjectModification<T>) change;
                if (filter.test(dao)) {
                    result.put((InstanceIdentifier<T>) path, dao);
                }
            }
            // Add any children to the queue
            for (DataObjectModification<? extends DataObject> child : change.getModifiedChildren()) {
                remainingChanges.add(child);
                remainingPaths.add(extendPath(path, child));
            }
        }
        return result;
    }

    /**
     * Extends the given instance identifier path to include the given child. Augmentations are treated in the same way
     * as children; keyed children are handled correctly.
     *
     * @param path The current path.
     * @param child The child modification to include.
     * @return The extended path.
     */
    private static <N extends KeyAware<K> & ChildOf<? super T>, K extends Key<N>, T extends DataObject>
        InstanceIdentifier<? extends DataObject> extendPath(
            InstanceIdentifier<T> path,
            DataObjectModification<?> child) {
        @SuppressWarnings("unchecked")
        final Class<N> item = (Class<N>) child.getDataType();
        if (child.getIdentifier() instanceof InstanceIdentifier.IdentifiableItem) {
            return path.child(item, ((InstanceIdentifier.IdentifiableItem<N, K>) child.getIdentifier()).getKey());
        }

        return path.child(item);
    }

    public static <T extends DataObject> Map<InstanceIdentifier<T>, T> extractRemovedObjects(
            DataChangeEvent changes, Class<T> klazz) {
        Set<InstanceIdentifier<T>> iids = extractRemoved(changes, klazz);
        return Maps.filterKeys(extractOriginal(changes, klazz),Predicates.in(iids));
    }

    /**
     * Extract the removed instances of {@code clazz} from the given set of modifications.
     *
     * @param changes The changes to process.
     * @param clazz The class we're interested in.
     * @param <T> The type of changes we're interested in.
     * @param <U> The type of changes to process.
     * @return The removed instances, keyed by instance identifier.
     */
    public static <T extends DataObject, U extends DataObject> Map<InstanceIdentifier<T>, T> extractRemovedObjects(
            Collection<DataTreeModification<U>> changes, Class<T> clazz) {
        Map<InstanceIdentifier<T>, T> result = new HashMap<>();
        for (Map.Entry<InstanceIdentifier<T>, DataObjectModification<T>> entry :
                extractDataObjectModifications(changes, clazz, modificationIsDeletionAndHasDataBefore()).entrySet()) {
            result.put(entry.getKey(), entry.getValue().getDataBefore());
        }
        return result;
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
                        // Actually checked above
                        @SuppressWarnings("unchecked")
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
                ? insert.getUuidName() : SouthboundMapper.getRandomUuid();
        insert.setUuidName(uuidString);
        return new UUID(uuidString);
    }

    public static <T extends TableSchema<T>> void stampInstanceIdentifier(TransactionBuilder transaction,
            InstanceIdentifier<?> iid, TableSchema<T> tableSchema, ColumnSchema<T, Map<String, String>> columnSchema,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        transaction.add(
                stampInstanceIdentifierMutation(transaction, iid, tableSchema, columnSchema, instanceIdentifierCodec));
    }

    public static <T extends TableSchema<T>> Mutate<T> stampInstanceIdentifierMutation(TransactionBuilder transaction,
            InstanceIdentifier<?> iid, TableSchema<T> tableSchema, ColumnSchema<T, Map<String, String>> columnSchema,
            InstanceIdentifierCodec instanceIdentifierCodec) {
        Map<String,String> externalIdsMap = ImmutableMap.of(SouthboundConstants.IID_EXTERNAL_ID_KEY,
                instanceIdentifierCodec.serialize(iid));
        Mutate<T> mutate = op.mutate(tableSchema)
                .addMutation(columnSchema,
                    Mutator.INSERT,
                    externalIdsMap);
        Mutation deleteIidMutation = new Mutation(columnSchema.getName(),
                Mutator.DELETE,
                OvsdbSet.fromSet(Collections.singleton(SouthboundConstants.IID_EXTERNAL_ID_KEY)));
        List<Mutation> mutations = new ArrayList<>();
        mutations.add(deleteIidMutation);
        mutations.addAll(mutate.getMutations());
        mutate.setMutations(mutations);
        return mutate;
    }

    /**
     * This method builds a string by concatenating the 2 character
     * hexadecimal representation of each byte from the input byte array.
     * <p>
     * For example: an input byte array containing:
     *   bytes[0] = 'a'
     *   bytes[1] = 'b'
     *   bytes[2] = 'c'
     *   bytes[3] = '-'
     *   bytes[4] = '1'
     *   bytes[5] = '2'
     *   bytes[6] = '3'
     * returns the string "6162632d313233"
     * </p>
     * @param bytes
     *            The byte array to convert to string
     * @return The hexadecimal representation of the byte array. If bytes is
     *         null, the string "" is returned
     */
    public static String bytesToHexString(byte[] bytes) {

        if (bytes == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            short u8byte = (short) (b & 0xff);
            String tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1) {
                buf.append('0');
            }
            buf.append(tmp);
        }
        return buf.toString();
    }
}
