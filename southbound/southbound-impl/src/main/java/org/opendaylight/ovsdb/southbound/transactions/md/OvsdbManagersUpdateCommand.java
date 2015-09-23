/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Manager;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagerEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbManagersUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbManagersUpdateCommand.class);

    private Map<UUID, Manager> updatedManagerRows;
    private Map<UUID, OpenVSwitch> updatedOpenVSwitchRows;

    public OvsdbManagersUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedOpenVSwitchRows = TyperUtils.extractRowsUpdated(OpenVSwitch.class, getUpdates(), getDbSchema());
        updatedManagerRows = TyperUtils.extractRowsUpdated(Manager.class,getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedManagerRows != null && !updatedManagerRows.isEmpty()) {
            Map<Uri, Manager> updatedManagerRowsWithUri = getUriManagerMap(updatedManagerRows);
            if (updatedOpenVSwitchRows != null && !updatedOpenVSwitchRows.isEmpty()) {
                updateManagers(transaction, updatedManagerRows, updatedOpenVSwitchRows);
            } else {
                updateManagers(transaction, updatedManagerRowsWithUri);
            }
        }
    }

    /**
     * Update the Manager values for the given {@link OpenVSwitch} list.
     *
     * <p>
     * Manager and OpenVSwitch are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Manager fields in the
     * OVSDB Node data. In some cases the OVSDB will send OpenVSwitch and Manager
     * updates together and in other cases independently. This method here
     * assumes the former.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedManagerRows updated {@link Manager} rows
     * @param updatedOpenVSwitchRows updated {@link OpenVSwitch} rows
     */
    private void updateManagers(ReadWriteTransaction transaction,
                                  Map<UUID, Manager> updatedManagerRows,
                                  Map<UUID, OpenVSwitch> updatedOpenVSwitchRows) {

        for (Map.Entry<UUID, OpenVSwitch> ovsdbNodeEntry : updatedOpenVSwitchRows.entrySet()) {
            final List<ManagerEntry> managerEntries =
                    SouthboundMapper.createManagerEntries(ovsdbNodeEntry.getValue(), updatedManagerRows);
            LOG.debug("Update Ovsdb Node {} with manager entries {}",ovsdbNodeEntry.getValue(), managerEntries);
            for (ManagerEntry managerEntry : managerEntries) {
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        getManagerEntryIid(managerEntry),
                        managerEntry);
            }
        }
    }

    /**
     * Update the ManagerEntry values after finding the related {@OpenVSwitch} list.
     *
     * <p>
     * Manager and OpenVSwitch are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Manager fields in the
     * OvsdbNode data. In some cases the OVSDB will send OpenVSwitch and Manager
     * updates together and in other cases independently. This method here
     * assumes the latter.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedManagerRows updated {@link Manager} rows

     */
    private void updateManagers(ReadWriteTransaction transaction,
                                  Map<Uri, Manager> updatedManagerRows) {

        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, connectionIId);
        if (ovsdbNode.isPresent()) {
            final List<ManagerEntry> managerEntries =
                    SouthboundMapper.createManagerEntries(ovsdbNode.get(), updatedManagerRows);

            LOG.debug("Update Ovsdb Node {} with manager entries {}",ovsdbNode.get(), managerEntries);
            for (ManagerEntry managerEntry : managerEntries) {
                InstanceIdentifier<ManagerEntry> iid = connectionIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(ManagerEntry.class, managerEntry.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        iid, managerEntry);
            }
        }
    }

    /**
     * Create the {@link InstanceIdentifier} for the {@link ManagerEntry}.
     *
     * @param managerEntry the {@link ManagerEntry}
     * @return the {@link InstanceIdentifier}
     */
    private InstanceIdentifier<ManagerEntry> getManagerEntryIid(ManagerEntry managerEntry) {

        OvsdbConnectionInstance client = getOvsdbConnectionInstance();
        String nodeString = client.getNodeKey().getNodeId().getValue();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> ovsdbNodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,nodeKey)
                .build();

        InstanceIdentifier<ManagerEntry> iid = ovsdbNodeIid
                .augmentation(OvsdbNodeAugmentation.class)
                .child(ManagerEntry.class, managerEntry.getKey());
        return iid;
    }

    private Map<Uri, Manager> getUriManagerMap(Map<UUID,Manager> uuidManagerMap) {
        Map<Uri, Manager> uriManagerMap = new HashMap<>();
        for (Map.Entry<UUID, Manager> uuidManagerMapEntry : uuidManagerMap.entrySet()) {
            uriManagerMap.put(
                    new Uri(uuidManagerMapEntry.getValue().getTargetColumn().getData()),
                    uuidManagerMapEntry.getValue());
        }
        return uriManagerMap;

    }
}
