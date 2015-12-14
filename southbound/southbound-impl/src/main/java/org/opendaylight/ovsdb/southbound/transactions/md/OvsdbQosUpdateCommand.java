/*
 * Copyright (c) 2015 Intel Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.schema.openvswitch.Qos;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.QosEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.qos.entries.QosOtherConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.PortOtherConfigsBuilder;
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

public class OvsdbQosUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbQosUpdateCommand.class);

    private Map<UUID, Qos> updatedQosRows;
    private Map<UUID, OpenVSwitch> updatedOpenVSwitchRows;

    public OvsdbQosUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedOpenVSwitchRows = TyperUtils.extractRowsUpdated(OpenVSwitch.class, getUpdates(), getDbSchema());
        updatedQosRows = TyperUtils.extractRowsUpdated(Qos.class,getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedQosRows != null && !updatedQosRows.isEmpty()) {
            if (updatedOpenVSwitchRows != null && !updatedOpenVSwitchRows.isEmpty()) {
                updateQos(transaction, updatedQosRows, updatedOpenVSwitchRows);
            } else {
                updateQos(transaction, updatedQosRows);
            }
        }
    }

    /**
     * Update the Qos values for the given {@link OpenVSwitch} list.
     *
     * <p>
     * Qos and OpenVSwitch are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Qos fields in the
     * OVSDB Node data. In some cases the OVSDB will send OpenVSwitch and Qos
     * updates together and in other cases independently. This method here
     * assumes the former.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedQosRows updated {@link Qos} rows
     * @param updatedOpenVSwitchRows updated {@link OpenVSwitch} rows
     */
    private void updateQos(ReadWriteTransaction transaction,
                                  Map<UUID, Qos> updatedQosRows,
                                  Map<UUID, OpenVSwitch> updatedOpenVSwitchRows) {
        
        for (Map.Entry<UUID, OpenVSwitch> ovsdbNodeEntry : updatedOpenVSwitchRows.entrySet()) {
            final List<QosEntries> qosEntries =
                    SouthboundMapper.createQosEntries(ovsdbNodeEntry.getValue(), updatedQosRows);
            LOG.debug("Update Ovsdb Node {} with qos entries {}",ovsdbNodeEntry.getValue(), qosEntries);
            for (QosEntries qosEntry : qosEntries) {
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        getQosEntryIid(qosEntry),
                        qosEntry);
            }
        }
    }

    /**
     * Update the QosEntries values after finding the related {@OpenVSwitch} list.
     *
     * <p>
     * Qos and OpenVSwitch are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Qos fields in the
     * OvsdbNode data. In some cases the OVSDB will send OpenVSwitch and Qos
     * updates together and in other cases independently. This method here
     * assumes the latter.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedQosRows updated {@link Qos} rows

     */
    private void updateQos(ReadWriteTransaction transaction,
                                  Map<UUID, Qos> updatedQosRows) {

        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, connectionIId);
        if (ovsdbNode.isPresent()) {
            final List<QosEntries> qosEntries =
                    SouthboundMapper.createQosEntries(ovsdbNode.get(), updatedQosRows);

            LOG.debug("Update Ovsdb Node {} with qos entries {}",ovsdbNode.get(), qosEntries);
            for (QosEntries qosEntry : qosEntries) {
                InstanceIdentifier<QosEntries> iid = connectionIId
                        .augmentation(OvsdbNodeAugmentation.class)
                        .child(QosEntries.class, qosEntry.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        iid, qosEntry);
            }
        }
    }

    /**
     * Create the {@link InstanceIdentifier} for the {@link QosEntries}.
     *
     * @param qosEntries the {@link QosEntries}
     * @return the {@link InstanceIdentifier}
     */
    private InstanceIdentifier<QosEntries> getQosEntryIid(QosEntries qosEntry) {

        OvsdbConnectionInstance client = getOvsdbConnectionInstance();
        String nodeString = client.getNodeKey().getNodeId().getValue();
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> ovsdbNodeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,nodeKey)
                .build();

        return ovsdbNodeIid
                .augmentation(OvsdbNodeAugmentation.class)
                .child(QosEntries.class, qosEntry.getKey());
    }
}
