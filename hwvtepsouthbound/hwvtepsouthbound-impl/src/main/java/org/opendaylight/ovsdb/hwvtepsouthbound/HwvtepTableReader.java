/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TypedDatabaseSchema;
import org.opendaylight.ovsdb.schema.hardwarevtep.ACL;
import org.opendaylight.ovsdb.schema.hardwarevtep.ACLEntry;
import org.opendaylight.ovsdb.schema.hardwarevtep.ArpSourcesLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.ArpSourcesRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Manager;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepTableReader {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepTableReader.class);

    private final Class[] alltables = new Class[] {
        ACLEntry.class,
        ACL.class,
        ArpSourcesLocal.class,
        Global.class,
        ArpSourcesRemote.class,
        LogicalRouter.class,
        Manager.class,
        LogicalSwitch.class,
        McastMacsLocal.class,
        PhysicalLocator.class,
        McastMacsRemote.class,
        PhysicalPort.class,
        Tunnel.class,
        PhysicalLocatorSet.class,
        PhysicalSwitch.class,
        UcastMacsLocal.class,
        UcastMacsRemote.class
    };

    private static final ImmutableMap<Class<?>, Class<? extends TypedBaseTable<?>>> TABLE_MAP = ImmutableMap.of(
        RemoteMcastMacs.class, McastMacsRemote.class,
        RemoteUcastMacs.class, UcastMacsRemote.class,
        LogicalSwitches.class, LogicalSwitch.class,
        TerminationPoint.class, PhysicalLocator.class);

    private final ImmutableMap<Class<? extends Identifiable<?>>, WhereClauseGetter<?>> whereClauseGetters;
    private final ImmutableClassToInstanceMap<TypedBaseTable<?>> tables;
    private final HwvtepConnectionInstance connectionInstance;

    public HwvtepTableReader(final HwvtepConnectionInstance connectionInstance) {
        this.connectionInstance = connectionInstance;
        TypedDatabaseSchema dbSchema = null;
        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
        }

        final Builder<TypedBaseTable<?>> tableBuilder = ImmutableClassToInstanceMap.<TypedBaseTable<?>>builder();
        final ImmutableMap.Builder<Class<? extends Identifiable<?>>, WhereClauseGetter<?>> whereBuilder =
                ImmutableMap.builderWithExpectedSize(4);

        if (dbSchema != null) {
            final McastMacsRemote mcastMacsTable = dbSchema.getTypedRowWrapper(McastMacsRemote.class, null);
            if (mcastMacsTable != null) {
                tableBuilder.put(McastMacsRemote.class, mcastMacsTable);
                whereBuilder.put(RemoteMcastMacs.class, new RemoteMcastMacWhereClauseGetter(mcastMacsTable));
            }
            final UcastMacsRemote ucastMacsTable = dbSchema.getTypedRowWrapper(UcastMacsRemote.class, null);
            if (ucastMacsTable != null) {
                tableBuilder.put(UcastMacsRemote.class, ucastMacsTable);
                whereBuilder.put(RemoteUcastMacs.class, new RemoteUcastMacWhereClauseGetter(ucastMacsTable));
            }
            final LogicalSwitch lsTable = dbSchema.getTypedRowWrapper(LogicalSwitch.class, null);
            if (lsTable != null) {
                tableBuilder.put(LogicalSwitch.class, lsTable);
                whereBuilder.put(LogicalSwitches.class, new LogicalSwitchWhereClauseGetter(lsTable));
            }
            final PhysicalLocator plTable = dbSchema.getTypedRowWrapper(PhysicalLocator.class, null);
            if (plTable != null) {
                tableBuilder.put(PhysicalLocator.class, plTable);
                whereBuilder.put(TerminationPoint.class, new LocatorWhereClauseGetter(plTable));
            }
        }

        tables = tableBuilder.build();
        whereClauseGetters = whereBuilder.build();
    }

    @FunctionalInterface
    private interface WhereClauseGetter<T extends DataObject> extends Function<InstanceIdentifier<T>, List<Condition>> {

    }

    class RemoteMcastMacWhereClauseGetter implements WhereClauseGetter<RemoteMcastMacs> {
        private final McastMacsRemote macTable;

        RemoteMcastMacWhereClauseGetter(final McastMacsRemote macTable) {
            this.macTable = requireNonNull(macTable);
        }

        @Override
        public List<Condition> apply(final InstanceIdentifier<RemoteMcastMacs> iid) {
            RemoteMcastMacsKey key = iid.firstKeyOf(RemoteMcastMacs.class);
            InstanceIdentifier<LogicalSwitches> lsIid = (InstanceIdentifier<LogicalSwitches>) key.getLogicalSwitchRef()
                    .getValue();
            UUID lsUUID = connectionInstance.getDeviceInfo().getUUID(LogicalSwitches.class, lsIid);
            if (lsUUID == null) {
                LOG.error("Could not find uuid for ls key {}", lsIid);
                return null;
            }

            ArrayList<Condition> conditions = new ArrayList<>();
            conditions.add(macTable.getLogicalSwitchColumn().getSchema().opEqual(lsUUID));
            conditions.add(macTable.getMacColumn().getSchema().opEqual(key.getMacEntryKey().getValue()));
            return conditions;
        }
    }

    class RemoteUcastMacWhereClauseGetter implements WhereClauseGetter<RemoteUcastMacs> {
        private final UcastMacsRemote macTable;

        RemoteUcastMacWhereClauseGetter(final UcastMacsRemote macTable) {
            this.macTable = requireNonNull(macTable);
        }

        @Override
        public List<Condition> apply(final InstanceIdentifier<RemoteUcastMacs> iid) {
            RemoteUcastMacsKey key = iid.firstKeyOf(RemoteUcastMacs.class);
            InstanceIdentifier<LogicalSwitches> lsIid = (InstanceIdentifier<LogicalSwitches>) key.getLogicalSwitchRef()
                    .getValue();
            UUID lsUUID = connectionInstance.getDeviceInfo().getUUID(LogicalSwitches.class, lsIid);
            if (lsUUID == null) {
                LOG.error("Could not find uuid for ls key {}", lsIid);
                return null;
            }

            ArrayList<Condition> conditions = new ArrayList<>();
            conditions.add(macTable.getLogicalSwitchColumn().getSchema().opEqual(lsUUID));
            conditions.add(macTable.getMacColumn().getSchema().opEqual(key.getMacEntryKey().getValue()));
            return conditions;
        }
    }

    static class LogicalSwitchWhereClauseGetter implements WhereClauseGetter<LogicalSwitches> {
        private final LogicalSwitch logicalSwitch;

        LogicalSwitchWhereClauseGetter(final LogicalSwitch logicalSwitch) {
            this.logicalSwitch = requireNonNull(logicalSwitch);
        }

        @Override
        public List<Condition> apply(final InstanceIdentifier<LogicalSwitches> iid) {
            String lsName = iid.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
            return Lists.newArrayList(logicalSwitch.getNameColumn().getSchema().opEqual(lsName));
        }
    }

    static class LocatorWhereClauseGetter implements WhereClauseGetter<TerminationPoint> {
        private final PhysicalLocator locatorTable;

        LocatorWhereClauseGetter(final PhysicalLocator locatorTable) {
            this.locatorTable = requireNonNull(locatorTable);
        }

        @Override
        public List<Condition> apply(final InstanceIdentifier<TerminationPoint> iid) {
            String locatorIp = iid.firstKeyOf(TerminationPoint.class).getTpId().getValue();
            locatorIp = locatorIp.substring(locatorIp.indexOf(":") + 1);
            LOG.info("Locator ip to look for {}", locatorIp);
            return Lists.newArrayList(locatorTable.getDstIpColumn().getSchema().opEqual(locatorIp));
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public Optional<TypedBaseTable> getHwvtepTableEntryUUID(final Class<? extends Identifiable> cls,
                                                            final InstanceIdentifier iid,
                                                            final UUID existingUUID) {
        final TypedDatabaseSchema dbSchema;
        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
            return Optional.empty();
        }

        final Class<? extends TypedBaseTable<?>> tableClass = TABLE_MAP.get(cls);
        final GenericTableSchema hwvtepSchema = dbSchema.getTableSchema(tableClass);

        final Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
        selectOperation.setColumns(new ArrayList<>(hwvtepSchema.getColumns()));

        if (existingUUID == null) {
            final WhereClauseGetter<?> whereClausule = whereClauseGetters.get(cls);
            if (whereClausule == null) {
                LOG.error("Could not get where class for cls {} ", cls);
                return Optional.empty();
            }
            final List<Condition> conditions = whereClausule.apply(iid);
            if (conditions == null) {
                LOG.error("Could not get where conditions for cls {} key {}", cls, iid);
                return Optional.empty();
            }

            if (conditions.size() == 2) {
                selectOperation.where(conditions.get(0)).and(conditions.get(1));
            } else {
                selectOperation.where(conditions.get(0));
            }
        } else {
            TypedBaseTable<?> table = tables.get(tableClass);
            LOG.info("Setting uuid condition {} ", existingUUID);
            selectOperation.where(table.getUuidColumn().getSchema().opEqual(existingUUID));
        }

        ArrayList<Operation> operations = new ArrayList<>();
        operations.add(selectOperation);

        final List<OperationResult> results;
        try {
            results = connectionInstance.transact(dbSchema, operations).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch hardware_vtep table row from device {}",
                connectionInstance.getConnectionInfo(), e);
            return Optional.empty();
        }
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        final List<Row<GenericTableSchema>> selectResult = results.get(0).getRows();
        if (selectResult == null || selectResult.isEmpty()) {
            return Optional.empty();
        }

        final TypedBaseTable globalRow = dbSchema.getTypedRowWrapper(tableClass, selectResult.get(0));
        LOG.trace("Fetched {} from hardware_vtep schema", globalRow);
        return globalRow != null && globalRow.getUuid() != null ?  Optional.of(globalRow) : Optional.empty();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public List<TypedBaseTable> getHwvtepTableEntries(final Class<? extends Identifiable> cls) {
        final TypedDatabaseSchema dbSchema;
        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Not able to fetch schema for database {} from device {}",
                HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
            return null;
        }

        final Class<? extends TypedBaseTable<?>> tableClass = TABLE_MAP.get(cls);
        final GenericTableSchema hwvtepSchema = dbSchema.getTableSchema(tableClass);
        final Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
        selectOperation.setColumns(new ArrayList<>(hwvtepSchema.getColumns()));

        final List<OperationResult> results;
        try {
            results = connectionInstance.transact(dbSchema, Lists.newArrayList(selectOperation)).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Not able to fetch hardware_vtep table row from device {}",
                connectionInstance.getConnectionInfo(), e);
            return Collections.emptyList();
        }
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            final List<TypedBaseTable> tableRows = new ArrayList<>();
            for (OperationResult selectResult : results) {
                if (selectResult.getRows() != null && !selectResult.getRows().isEmpty()) {
                    for (Row<GenericTableSchema> row : selectResult.getRows()) {
                        tableRows.add(dbSchema.getTypedRowWrapper(tableClass, row));
                    }
                }
            }
            return tableRows;
        } catch (RuntimeException e) {
            LOG.error("Failed to get the hwvtep ", e);
            return Collections.emptyList();
        }
    }

    public TableUpdates readAllTables() throws ExecutionException, InterruptedException {
        TypedDatabaseSchema dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        List<Operation> operations = Arrays.asList(alltables).stream()
                .map(tableClass -> dbSchema.getTableSchema(tableClass))
                .map(tableSchema -> buildSelectOperationFor(tableSchema))
                .collect(Collectors.toList());
        List<OperationResult> results = connectionInstance.transact(dbSchema, operations).get();

        Map<String, TableUpdate> tableUpdates = new HashMap<>();
        if (results != null) {
            for (OperationResult result : results) {
                final List<Row<GenericTableSchema>> rows = result.getRows();
                if (rows != null) {
                    for (Row<GenericTableSchema> row : rows) {
                        tableUpdates.computeIfAbsent(row.getTableSchema().getName(), key -> new TableUpdate<>())
                            .addRow(getRowUuid(row), null, row);
                    }
                }
            }
        }
        return new TableUpdates(tableUpdates);
    }

    private static Select<GenericTableSchema> buildSelectOperationFor(final GenericTableSchema tableSchema) {
        Select<GenericTableSchema> selectOpearation = op.select(tableSchema);
        selectOpearation.setColumns(new ArrayList<>(tableSchema.getColumns()));
        return selectOpearation;
    }

    private static UUID getRowUuid(final Row<GenericTableSchema> row) {
        return row.getColumns().stream()
                .filter(column -> column.getSchema().getName().equals("_uuid"))
                .map(column -> (UUID) column.getData())
                .findFirst().orElse(new UUID("test"));
    }
}
