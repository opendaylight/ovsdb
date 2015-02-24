package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;

import com.google.common.base.Preconditions;

public class TransactionUtils {

    public static <T> Map<UUID,T> extractRowsUpdated(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID,T> result = new HashMap<UUID,T>();

        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates = extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if(rowUpdate != null) {
                if(rowUpdate.getNew() != null) {
                    Row<GenericTableSchema> row = rowUpdate.getNew();
                    result.put(rowUpdate.getUuid(),TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
                }
            }
        }
        return result;
    }

    public static <T> Map<UUID,T> extractRowsRemoved(Class<T> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID,T> result = new HashMap<UUID,T>();

        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rowUpdates = extractRowUpdates(klazz,updates,dbSchema);
        for (TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema> rowUpdate : rowUpdates.values()) {
            if(rowUpdate != null) {
                if(rowUpdate.getNew() == null && rowUpdate.getOld() != null) {
                    Row<GenericTableSchema> row = rowUpdate.getOld();
                    result.put(rowUpdate.getUuid(),TyperUtils.getTypedRowWrapper(dbSchema,klazz,row));
                }
            }
        }
        return result;
    }

    public static Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> extractRowUpdates(Class<?> klazz,TableUpdates updates,DatabaseSchema dbSchema) {
        Preconditions.checkNotNull(klazz);
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(dbSchema);
        Map<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> result = new HashMap<UUID,TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>>();
        TableUpdate<GenericTableSchema> update = updates.getUpdate(TyperUtils.getTableSchema(dbSchema, klazz));
        if(update != null) {
            Map<UUID, TableUpdate<GenericTableSchema>.RowUpdate<GenericTableSchema>> rows = update.getRows();
            if(rows != null) {
                result = rows;
            }
        }
        return result;
    }

}

