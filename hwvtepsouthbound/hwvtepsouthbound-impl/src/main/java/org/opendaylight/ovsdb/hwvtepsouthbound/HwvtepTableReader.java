/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.lib.notation.Condition;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Select;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class HwvtepTableReader {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepTableReader.class);
    private HwvtepConnectionInstance key;

    Map<Class, Function<InstanceIdentifier, List<Condition>>> whereClauseGetterMap = new HashMap();
    Map<Class, Class> tableMap = new HashMap();
    Map<Class, TypedBaseTable> tables = new HashMap<>();

    HwvtepConnectionInstance connectionInstance;

    public HwvtepTableReader(HwvtepConnectionInstance connectionInstance) {
        this.connectionInstance = connectionInstance;
        DatabaseSchema dbSchema = null;
        try {
            dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Not able to fetch schema for database {} from device {}",
                    HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
        }

        tableMap.put(RemoteMcastMacs.class, McastMacsRemote.class);
        tableMap.put(RemoteUcastMacs.class, UcastMacsRemote.class);
        tableMap.put(LogicalSwitches.class, LogicalSwitch.class);
        tableMap.put(TerminationPoint.class, PhysicalLocator.class);

        whereClauseGetterMap.put(RemoteMcastMacs.class, new RemoteMcastMacWhereClauseGetter());
        whereClauseGetterMap.put(RemoteUcastMacs.class, new RemoteUcastMacWhereClauseGetter());
        whereClauseGetterMap.put(LogicalSwitches.class, new LogicalSwitchWhereClauseGetter());
        whereClauseGetterMap.put(TerminationPoint.class, new LocatorWhereClauseGetter());

        tables.put(McastMacsRemote.class, TyperUtils.getTypedRowWrapper(dbSchema, McastMacsRemote.class, null));
        tables.put(UcastMacsRemote.class, TyperUtils.getTypedRowWrapper(dbSchema, UcastMacsRemote.class, null));
        tables.put(LogicalSwitch.class, TyperUtils.getTypedRowWrapper(dbSchema, LogicalSwitch.class, null));
        tables.put(PhysicalLocator.class, TyperUtils.getTypedRowWrapper(dbSchema, PhysicalLocator.class, null));
    }

    class RemoteMcastMacWhereClauseGetter implements Function<InstanceIdentifier, List<Condition>> {
        @Override
        public List<Condition> apply(InstanceIdentifier iid) {
            InstanceIdentifier<RemoteMcastMacs> macIid = iid;
            String mac = macIid.firstKeyOf(RemoteMcastMacs.class).getMacEntryKey().getValue();
            InstanceIdentifier<LogicalSwitches> lsIid = (InstanceIdentifier<LogicalSwitches>) macIid.firstKeyOf(RemoteMcastMacs.class).getLogicalSwitchRef().getValue();
            UUID lsUUID = connectionInstance.getDeviceInfo().getUUID(LogicalSwitches.class, lsIid);
            if (lsUUID == null) {
                LOG.error("Could not find uuid for ls key {}", lsIid);
                return null;
            }

            McastMacsRemote macTable = (McastMacsRemote) tables.get(McastMacsRemote.class);
            ArrayList<Condition> conditions = new ArrayList<>();
            conditions.add(macTable.getLogicalSwitchColumn().getSchema().opEqual(lsUUID));
            conditions.add(macTable.getMacColumn().getSchema().opEqual(mac));
            return conditions;
        }
    }

    class RemoteUcastMacWhereClauseGetter implements Function<InstanceIdentifier, List<Condition>> {
        @Override
        public List<Condition> apply(InstanceIdentifier iid) {
            InstanceIdentifier<RemoteUcastMacs> macIid = iid;
            String mac = macIid.firstKeyOf(RemoteUcastMacs.class).getMacEntryKey().getValue();
            InstanceIdentifier<LogicalSwitches> lsIid = (InstanceIdentifier<LogicalSwitches>) macIid.firstKeyOf(RemoteUcastMacs.class).getLogicalSwitchRef().getValue();
            UUID lsUUID = connectionInstance.getDeviceInfo().getUUID(LogicalSwitches.class, lsIid);
            if (lsUUID == null) {
                LOG.error("Could not find uuid for ls key {}", lsIid);
                return null;
            }

            UcastMacsRemote macTable = (UcastMacsRemote) tables.get(UcastMacsRemote.class);
            ArrayList<Condition> conditions = new ArrayList<>();
            conditions.add(macTable.getLogicalSwitchColumn().getSchema().opEqual(lsUUID));
            conditions.add(macTable.getMacColumn().getSchema().opEqual(mac));
            return conditions;
        }
    }

    class LogicalSwitchWhereClauseGetter implements Function<InstanceIdentifier, List<Condition>> {
        @Override
        public List<Condition> apply(InstanceIdentifier iid) {
            InstanceIdentifier<LogicalSwitches> lsIid = iid;
            String lsName = lsIid.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName().getValue();
            LogicalSwitch logicalSwitch = (LogicalSwitch) tables.get(LogicalSwitch.class);
            return Lists.newArrayList(logicalSwitch.getNameColumn().getSchema().opEqual(lsName));
        }
    }

    class LocatorWhereClauseGetter implements Function<InstanceIdentifier, List<Condition>> {
        @Override
        public List<Condition> apply(InstanceIdentifier iid) {
            InstanceIdentifier<TerminationPoint> tepIid = iid;
            String locatorIp = tepIid.firstKeyOf(TerminationPoint.class).getTpId().getValue();
            locatorIp = locatorIp.substring(locatorIp.indexOf(":") + 1);
            LOG.info("Locator ip to look for {}", locatorIp);
            PhysicalLocator locatorTable = (PhysicalLocator) tables.get(PhysicalLocator.class);
            return Lists.newArrayList(locatorTable.getDstIpColumn().getSchema().opEqual(locatorIp));
        }
    }

    public Optional<TypedBaseTable> getHwvtepTableEntryUUID(Class<? extends Identifiable> cls,
                                                            InstanceIdentifier iid,
                                                            UUID existingUUID) {
        try {
            DatabaseSchema dbSchema = null;
            TypedBaseTable globalRow = null;
            Class<TypedBaseTable> tableClass = tableMap.get(cls);
            try {
                dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Not able to fetch schema for database {} from device {}",
                        HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
            }

            if (dbSchema != null) {
                GenericTableSchema hwvtepSchema = TyperUtils.getTableSchema(dbSchema, tableClass);

                List<String> hwvtepTableColumn = new ArrayList<>();
                hwvtepTableColumn.addAll(hwvtepSchema.getColumns());
                Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
                selectOperation.setColumns(hwvtepTableColumn);

                if (existingUUID != null) {
                    TypedBaseTable table = tables.get(tableClass);
                    LOG.info("Setting uuid condition {} ", existingUUID);
                    selectOperation.where(table.getUuidColumn().getSchema().opEqual(existingUUID));
                } else {
                    if (whereClauseGetterMap.get(cls) != null) {
                        List<Condition> conditions = whereClauseGetterMap.get(cls).apply(iid);
                        if (conditions != null) {
                            if (conditions.size() == 2) {
                                selectOperation.where(conditions.get(0)).and(conditions.get(1));
                            } else {
                                selectOperation.where(conditions.get(0));
                            }
                        } else {
                            LOG.error("Could not get where conditions for cls {} key {}", cls, iid);
                            return Optional.empty();
                        }
                    } else {
                        LOG.error("Could not get where class for cls {} ", cls);
                        return Optional.empty();
                    }
                }
                ArrayList<Operation> operations = new ArrayList<>();
                operations.add(selectOperation);
                try {
                    List<OperationResult> results = connectionInstance.transact(dbSchema, operations).get();
                    if (results != null && !results.isEmpty()) {
                        OperationResult selectResult = results.get(0);
                        if (selectResult.getRows() != null && !selectResult.getRows().isEmpty()) {
                            globalRow = TyperUtils.getTypedRowWrapper(
                                    dbSchema, tableClass, selectResult.getRows().get(0));
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("Not able to fetch hardware_vtep table row from device {}",
                            connectionInstance.getConnectionInfo(), e);
                }
            }
            LOG.trace("Fetched {} from hardware_vtep schema", globalRow);
            if (globalRow != null && globalRow.getUuid() != null) {
                return Optional.of(globalRow);
            }
            return Optional.empty();
        } catch (Throwable e) {
            LOG.error("Failed to get the hwvtep row for iid " + iid, e);
            return Optional.empty();
        }
    }

    public List<TypedBaseTable> getHwvtepTableEntries(Class<? extends Identifiable> cls) {
        try {
            List<TypedBaseTable> tableRows = new ArrayList<>();
            DatabaseSchema dbSchema = null;
            TypedBaseTable globalRow = null;
            Class<TypedBaseTable> tableClass = tableMap.get(cls);
            try {
                dbSchema = connectionInstance.getSchema(HwvtepSchemaConstants.HARDWARE_VTEP).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Not able to fetch schema for database {} from device {}",
                        HwvtepSchemaConstants.HARDWARE_VTEP, connectionInstance.getConnectionInfo(), e);
            }

            if (dbSchema != null) {
                GenericTableSchema hwvtepSchema = TyperUtils.getTableSchema(dbSchema, tableClass);

                List<String> hwvtepTableColumn = new ArrayList<>();
                hwvtepTableColumn.addAll(hwvtepSchema.getColumns());
                Select<GenericTableSchema> selectOperation = op.select(hwvtepSchema);
                selectOperation.setColumns(hwvtepTableColumn);

                ArrayList<Operation> operations = Lists.newArrayList(selectOperation);
                try {
                    List<OperationResult> results = connectionInstance.transact(dbSchema, operations).get();
                    if (results != null && !results.isEmpty()) {
                        for (OperationResult selectResult : results) {
                            if (selectResult.getRows() != null && !selectResult.getRows().isEmpty()) {
                                for (Row<GenericTableSchema> row : selectResult.getRows()) {
                                    tableRows.add(TyperUtils.getTypedRowWrapper(dbSchema, tableClass, row));
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Not able to fetch hardware_vtep table row from device {}",
                            connectionInstance.getConnectionInfo(), e);
                }
            }
            return tableRows;
        } catch (Throwable e) {
            LOG.error("Failed to get the hwvtep ", e);
        }
        return Collections.emptyList();
    }
}