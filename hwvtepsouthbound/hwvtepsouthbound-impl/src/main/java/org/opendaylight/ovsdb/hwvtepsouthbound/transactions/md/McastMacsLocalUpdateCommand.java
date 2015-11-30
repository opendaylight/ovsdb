/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsLocal;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorSetRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McastMacsLocalUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(McastMacsLocalUpdateCommand.class);
    private Map<UUID, McastMacsLocal> updatedMMacsLocalRows;
    private Map<UUID, McastMacsLocal> oldMMacsLocalRows;
    private Map<UUID, PhysicalLocator> updatedPLocRows;
    private Map<UUID, LogicalSwitch> updatedLSRows;

    public McastMacsLocalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMMacsLocalRows = TyperUtils.extractRowsUpdated(McastMacsLocal.class, getUpdates(),getDbSchema());
        oldMMacsLocalRows = TyperUtils.extractRowsOld(McastMacsLocal.class, getUpdates(),getDbSchema());
        updatedPLocRows = TyperUtils.extractRowsUpdated(PhysicalLocator.class, getUpdates(),getDbSchema());
        updatedLSRows = TyperUtils.extractRowsUpdated(LogicalSwitch.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if(updatedMMacsLocalRows != null && !updatedMMacsLocalRows.isEmpty()) {
            for (Entry<UUID, McastMacsLocal> entry : updatedMMacsLocalRows.entrySet()) {
                updateData(transaction, entry.getValue());
            }
        }
    }

    private void updateData(ReadWriteTransaction transaction, McastMacsLocal mMacLocal) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            // Update the connection node to let it know it manages this MCastMacsLocal
            Node connectionNode = buildConnectionNode(mMacLocal);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
//            TODO: Delete entries that are no longer needed
        }
    }

    private Node buildConnectionNode(McastMacsLocal mMacLocal) {
      //Update node with McastMacsLocal reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        InstanceIdentifier<Node> nodeIid = getOvsdbConnectionInstance().getInstanceIdentifier();
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        LocalMcastMacsBuilder mMacLocalBuilder= new LocalMcastMacsBuilder();
        mMacLocalBuilder.setMacEntryKey(new MacAddress(mMacLocal.getMac()));
        if(mMacLocal.getLocatorSetColumn() != null
                && mMacLocal.getLocatorSetColumn().getData() != null){
            UUID plocUUID = mMacLocal.getLocatorSetColumn().getData();
            if(updatedPLocRows.get(plocUUID) != null ){
                InstanceIdentifier<TerminationPoint> plIid = HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, updatedPLocRows.get(plocUUID));
                mMacLocalBuilder.setLocatorSetRef(new HwvtepPhysicalLocatorSetRef(plIid));
            }
        }
        if(mMacLocal.getLogicalSwitchColumn() != null
                && mMacLocal.getLogicalSwitchColumn().getData() != null){
            UUID lsUUID = mMacLocal.getLogicalSwitchColumn().getData();
            if (updatedLSRows.get(lsUUID) != null) {
                mMacLocalBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(updatedLSRows.get(lsUUID).getName()));
            }
        }
        List<LocalMcastMacs> mMacLocalList = new ArrayList<>();
        mMacLocalList.add(mMacLocalBuilder.build());
        hgAugmentationBuilder.setLocalMcastMacs(mMacLocalList);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

}
