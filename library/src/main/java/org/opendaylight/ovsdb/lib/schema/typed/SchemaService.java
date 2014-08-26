package org.opendaylight.ovsdb.lib.schema.typed;

import java.util.Map;

public interface SchemaService {
    public Map.Entry<String, String> getParentColumnToMutate(String childTabletoInsert, String databaseVersion);
}
