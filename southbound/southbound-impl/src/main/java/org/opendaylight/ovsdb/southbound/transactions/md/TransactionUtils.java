package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;

import com.google.common.base.Preconditions;

public class TransactionUtils {

    public static <T> List<T> extractRowsUpdated(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        List<T> result = new ArrayList<T>();

        List<TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates = extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates) {
            if(rowUpdate != null) {
                if(rowUpdate.getNew() != null) {
                    Row<GenericTableSchema> row = rowUpdate.getNew();
                    result.add(TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
                }
            }
        }
        return result;
    }

    public static <T> List<T> extractRowsRemoved(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        List<T> result = new ArrayList<T>();

        List<TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates = extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates) {
            if(rowUpdate != null) {
                if(rowUpdate.getNew() == null && rowUpdate.getOld() != null) {
                    Row<GenericTableSchema> row = rowUpdate.getOld();
                    result.add(TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
                }
            }
        }
        return result;
    }

    public static List<TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> extractRowUpdates(Class<?> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        List<TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> result = new ArrayList<TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>>();
        TableUpdate<GenericTableSchema> update = updates.getUpdate(TyperUtils.getTableSchema(dbSchema, klazz));
        if(update != null) {
            Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rows = update.getRows();
            if(rows != null) {
                for(TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rows.values()) {
                    if(rowUpdate != null) {
                        result.add(rowUpdate);
                    }
                }
            }
        }
        return result;
    }

}

