/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.lib.notation.Mutation;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.OvsdbSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Insert;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TransactUtils.class, SouthboundMapper.class, SouthboundUtil.class, OvsdbSet.class, Operations.class})
public class TransactUtilsTest {

    private static final String UUID_NAME = "uuid name";
    private static final String IID_STRING = "iid string";
    private static final String COLUMN_SCHEMA_NAME = "column schema name";

    @Before
    public void setUp() {
        PowerMockito.mockStatic(TransactUtils.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testExtractNode() {
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        assertEquals(HashMap.class, TransactUtils.extractNode(changes).getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExtractCreatedAndExtractUpdated() {
        DataChangeEvent changes = mock(DataChangeEvent.class);
        Class<DataObject> klazz = DataObject.class;
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        when(changes.getCreatedData()).thenReturn(map);
        when(TransactUtils.extract(any(Map.class),eq(DataObject.class))).thenReturn(new HashMap<>());

        //test extractCreated()
        assertEquals(map, TransactUtils.extractCreated(changes, klazz));

        //test extractUpdated()
        assertEquals(map, TransactUtils.extractUpdated(changes, klazz));
    }

    @Test
    public void testExtractCreatedOrUpdated() {
        Map<InstanceIdentifier<DataObject>, DataObject> result = new HashMap<>();

        PowerMockito.doReturn(result).when(TransactUtils.class);
        TransactUtils.extractUpdated(any(DataChangeEvent.class), eq(NetworkTopology.class));

        Map<InstanceIdentifier<NetworkTopology>, NetworkTopology> map = new HashMap<>();
        InstanceIdentifier<NetworkTopology> iid = InstanceIdentifier.create(NetworkTopology.class);

        NetworkTopology db = mock(NetworkTopology.class);
        map.put(iid, db);

        PowerMockito.doReturn(map).when(TransactUtils.class);
        TransactUtils.extractCreated(any(DataChangeEvent.class), eq(NetworkTopology.class));

        Map<InstanceIdentifier<NetworkTopology>, NetworkTopology> testResult = new HashMap<>();
        testResult.put(iid, db);
        DataChangeEvent changes = mock(DataChangeEvent.class);
        assertEquals(testResult, TransactUtils.extractCreatedOrUpdated(changes, NetworkTopology.class));
    }

    @Test
    public void testExtractCreatedOrUpdatedOrRemoved() {
        Map<InstanceIdentifier<NetworkTopology>, NetworkTopology> result = new HashMap<>();

        PowerMockito.doReturn(result).when(TransactUtils.class);
        TransactUtils.extractCreatedOrUpdated(any(DataChangeEvent.class), eq(NetworkTopology.class));

        Map<InstanceIdentifier<NetworkTopology>, NetworkTopology> map = new HashMap<>();
        InstanceIdentifier<NetworkTopology> iid = InstanceIdentifier.create(NetworkTopology.class);
        NetworkTopology db = mock(NetworkTopology.class);
        map.put(iid, db);

        PowerMockito.doReturn(map).when(TransactUtils.class);
        TransactUtils.extractRemovedObjects(any(DataChangeEvent.class), eq(NetworkTopology.class));

        Map<InstanceIdentifier<NetworkTopology>, NetworkTopology> testResult = new HashMap<>();
        testResult.put(iid, db);
        DataChangeEvent changes = mock(DataChangeEvent.class);
        assertEquals(testResult, TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, NetworkTopology.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExtractOriginal() {
        DataChangeEvent changes = mock(DataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        when(changes.getOriginalData()).thenReturn(map);
        when(TransactUtils.extract(any(Map.class), eq(DataObject.class))).thenReturn(new HashMap<>());

        //test extractOriginal()
        assertEquals(map, TransactUtils.extractCreated(changes, DataObject.class));
    }

    @Test
    public void testExtractRemoved() {
        DataChangeEvent changes = mock(DataChangeEvent.class);
        assertEquals(HashSet.class, TransactUtils.extractRemoved(changes, DataObject.class).getClass());
    }

    @Test
    public void testExtractRemovedObjects() {
        Set<InstanceIdentifier<DataObject>> iids = new HashSet<>();

        PowerMockito.doReturn(iids).when(TransactUtils.class);
        TransactUtils.extractRemoved(any(DataChangeEvent.class), eq(DataObject.class));

        Map<InstanceIdentifier<DataObject>, DataObject> result = new HashMap<>();

        PowerMockito.doReturn(result).when(TransactUtils.class);
        TransactUtils.extractOriginal(any(DataChangeEvent.class), eq(DataObject.class));

        Class<DataObject> klazz = DataObject.class;
        DataChangeEvent changes = mock(DataChangeEvent.class);
        assertEquals(Maps.filterKeys(result, Predicates.in(iids)), TransactUtils.extractRemovedObjects(changes, klazz));
    }

    @Test
    public void testExtract() {
        Map<InstanceIdentifier<?>, DataObject> changes = new HashMap<>();
        Class<DataObject> klazz = DataObject.class;
        assertEquals(HashMap.class, TransactUtils.extract(changes, klazz).getClass());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testExtractInsert() {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        GenericTableSchema schema = mock(GenericTableSchema.class);

        List<Operation> operations = new ArrayList<>();
        Operation operation = mock(Insert.class);
        operations.add(operation);
        when(transaction.getOperations()).thenReturn(operations);
        when(operation.getTableSchema()).thenReturn(schema);

        List<Insert> inserts = TransactUtils.extractInsert(transaction, schema);
        assertEquals(operation, inserts.get(0));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testExtractNamedUuid() throws Exception {
        Insert insert = mock(Insert.class);
        when(insert.getUuidName()).thenReturn(UUID_NAME);
        PowerMockito.mockStatic(SouthboundMapper.class);
        PowerMockito.when(SouthboundMapper.getRandomUuid()).thenReturn(UUID_NAME);
        doNothing().when(insert).setUuidName(anyString());

        UUID uuid = mock(UUID.class);
        PowerMockito.whenNew(UUID.class).withAnyArguments().thenReturn(uuid);
        assertEquals(uuid, TransactUtils.extractNamedUuid(insert));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStampInstanceIdentifier() {
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        InstanceIdentifier<?> iid = InstanceIdentifier.create(NetworkTopology.class);
        TableSchema<GenericTableSchema> tableSchema = mock(TableSchema.class);
        ColumnSchema<GenericTableSchema, Map<String, String>> columnSchema = mock(ColumnSchema.class);
        InstanceIdentifierCodec instanceIdentifierCodec = mock(InstanceIdentifierCodec.class);

        PowerMockito.doReturn(mock(Mutate.class)).when(TransactUtils.class);
        TransactUtils.stampInstanceIdentifierMutation(transaction, iid, tableSchema, columnSchema,
            instanceIdentifierCodec);

        when(transaction.add(any(Operation.class))).thenReturn(transaction);
        TransactUtils.stampInstanceIdentifier(transaction, iid, tableSchema, columnSchema, instanceIdentifierCodec);
        verify(transaction).add(any(Operation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStampInstanceIdentifierMutation() throws Exception {
        InstanceIdentifierCodec instanceIdentifierCodec = Mockito.mock(InstanceIdentifierCodec.class);
        when(instanceIdentifierCodec.serialize(any(InstanceIdentifier.class))).thenReturn(IID_STRING);

        Mutate<GenericTableSchema> mutate = mock(Mutate.class);
        Operations op = (Operations) setField("op");
        Mockito.<Mutate<GenericTableSchema>>when(op.mutate(any(TableSchema.class))).thenReturn(mutate);
        when(mutate.addMutation(any(ColumnSchema.class), any(Mutator.class), any(Map.class))).thenReturn(mutate);

        ColumnSchema<GenericTableSchema, Map<String,String>> columnSchema = mock(ColumnSchema.class);
        when(columnSchema.getName()).thenReturn(COLUMN_SCHEMA_NAME);
        PowerMockito.mockStatic(OvsdbSet.class);
        PowerMockito.when(OvsdbSet.fromSet(any(Set.class))).thenReturn(mock(OvsdbSet.class));
        Mutation deleteIidMutation = mock(Mutation.class);
        PowerMockito.whenNew(Mutation.class).withAnyArguments().thenReturn(deleteIidMutation);

        List<Mutation> listMutations = new ArrayList<>();
        when(mutate.getMutations()).thenReturn(listMutations);
        doNothing().when(mutate).setMutations(any(List.class));

        InstanceIdentifier<?> iid = InstanceIdentifier.create(NetworkTopology.class);
        TransactionBuilder transaction = mock(TransactionBuilder.class);
        TableSchema<GenericTableSchema> tableSchema = mock(TableSchema.class);
        assertEquals(mutate, TransactUtils.stampInstanceIdentifierMutation(transaction, iid, tableSchema, columnSchema,
                instanceIdentifierCodec));
    }

    private static Object setField(final String fieldName) throws Exception {
        Field field = Operations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(field.get(Operations.class), mock(Operations.class));
        return field.get(Operations.class);
    }
}
