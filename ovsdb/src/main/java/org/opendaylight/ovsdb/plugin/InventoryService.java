package org.opendaylight.ovsdb.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdate.Row;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import com.google.common.collect.Maps;

/**
 * Stub Implementation for IPluginInReadService used by SAL
 *
 *
 */
public class InventoryService implements IPluginInInventoryService, InventoryServiceInternal {
    private static final Logger logger = LoggerFactory
            .getLogger(InventoryService.class);
    private final Set<IPluginOutInventoryService> pluginOutInventoryServices =
            new CopyOnWriteArraySet<IPluginOutInventoryService>();
    private ConcurrentMap<Node, Map<String, Property>> nodeProps;
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps;
    private Map<Node, NodeDB> dbCache = Maps.newHashMap();

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
        nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
        nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    public void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    public void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    public void stop() {
    }

    public void setPluginOutInventoryServices(IPluginOutInventoryService service) {
        if (this.pluginOutInventoryServices != null) {
            this.pluginOutInventoryServices.add(service);
        }
    }

    public void unsetPluginOutInventoryServices(
            IPluginOutInventoryService service) {
        if (this.pluginOutInventoryServices != null) {
            this.pluginOutInventoryServices.remove(service);
        }
    }

    /**
     * Retrieve nodes from openflow
     */
    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        return nodeProps;
    }

    /**
     * Retrieve nodeConnectors from openflow
     */
    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh) {
        return nodeConnectorProps;
    }


    @Override
    public Map<String, Map<String, Table<?>>> getCache(Node n) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getTableCache();
    }


    @Override
    public Map<String, Table<?>> getTableCache(Node n, String tableName) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getTableCache(tableName);
    }


    @Override
    public Table<?> getRow(Node n, String tableName, String uuid) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getRow(tableName, uuid);
    }

    @Override
    public void updateRow(Node n, String tableName, String uuid, Table<?> row) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }
        db.updateRow(tableName, uuid, row);
    }

    @Override
    public void removeRow(Node n, String tableName, String uuid) {
        NodeDB db = dbCache.get(n);
        if (db != null) db.removeRow(tableName, uuid);
    }

    @Override
    public void processTableUpdates(Node n, TableUpdates tableUpdates) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }

        Set<Table.Name> available = tableUpdates.availableUpdates();
        for (Table.Name name : available) {
            TableUpdate tableUpdate = tableUpdates.getUpdate(name);
            Collection<TableUpdate.Row<?>> rows = tableUpdate.getRows();
            for (Row<?> row : rows) {
                String uuid = row.getId();
                Table<?> newRow = (Table<?>)row.getNew();
                Table<?> oldRow = (Table<?>)row.getOld();
                if (newRow != null) {
                    db.updateRow(name.getName(), uuid, newRow);
                } else if (oldRow != null) {
                    db.removeRow(name.getName(), uuid);
                }
            }
        }
    }

    @Override
    public void printCache(Node n) {
        NodeDB db = dbCache.get(n);
        if (db != null) db.printTableCache();
    }

    @Override
    public void addNode(Node node, Set<Property> props) {
        addNodeProperty(node, UpdateType.ADDED, props);
    }

    @Override
    public void addNodeProperty(Node node, UpdateType type, Set<Property> props) {
        Map<String, Property> nProp = nodeProps.get(node);
        if (nProp == null) nProp = new HashMap<String, Property>();
        for (Property prop : props) {
            nProp.put(prop.getName(), prop);
        }
        nodeProps.put(node, nProp);
        for (IPluginOutInventoryService service : pluginOutInventoryServices) {
            service.updateNode(node, type, props);
        }
    }

    @Override
    public DatabaseSchema getDatabaseSchema(Node n) {
        NodeDB db = dbCache.get(n);
        if (db != null) return db.getSchema();
        return null;
    }

    @Override
    public void updateDatabaseSchema(Node n, DatabaseSchema schema) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }
        db.setSchema(schema);
    }

    @Override
    public void removeNode(Node node) {
        for (IPluginOutInventoryService service : pluginOutInventoryServices) {
            service.updateNode(node, UpdateType.REMOVED, null);
        }
        nodeProps.remove(node);
        dbCache.remove(node);
    }
}
