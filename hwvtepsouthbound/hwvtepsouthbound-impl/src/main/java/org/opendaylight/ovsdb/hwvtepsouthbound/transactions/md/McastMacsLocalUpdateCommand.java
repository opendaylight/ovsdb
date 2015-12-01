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
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSetBuilder;
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
    private Map<UUID, PhysicalLocatorSet> updatedPLocSetRows;
    private Map<UUID, PhysicalLocator> updatedPLocRows;
    private Map<UUID, LogicalSwitch> updatedLSRows;

    public McastMacsLocalUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMMacsLocalRows = TyperUtils.extractRowsUpdated(McastMacsLocal.class, getUpdates(),getDbSchema());
        oldMMacsLocalRows = TyperUtils.extractRowsOld(McastMacsLocal.class, getUpdates(),getDbSchema());
        updatedPLocSetRows = TyperUtils.extractRowsUpdated(PhysicalLocatorSet.class, getUpdates(),getDbSchema());
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
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        LocalMcastMacsBuilder mMacLocalBuilder= new LocalMcastMacsBuilder();
        mMacLocalBuilder.setMacEntryKey(new MacAddress(mMacLocal.getMac()));
        setIpAddress(mMacLocalBuilder, mMacLocal);
        setLocatorSet(mMacLocalBuilder, mMacLocal);
        setLogicalSwitch(mMacLocalBuilder, mMacLocal);

        List<LocalMcastMacs> mMacLocalList = new ArrayList<>();
        mMacLocalList.add(mMacLocalBuilder.build());
        hgAugmentationBuilder.setLocalMcastMacs(mMacLocalList);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

    private void setLogicalSwitch(LocalMcastMacsBuilder mMacLocalBuilder, McastMacsLocal mMacLocal) {
        LogicalSwitch lSwitch = null;
        if (mMacLocal.getLogicalSwitchColumn() != null && mMacLocal.getLogicalSwitchColumn().getData() != null) {
            UUID lsUUID = mMacLocal.getLogicalSwitchColumn().getData();
            if (updatedLSRows.get(lsUUID) != null) {
                lSwitch = updatedLSRows.get(lsUUID);
                mMacLocalBuilder.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(lSwitch.getName()));
            }
        }
    }

    private void setIpAddress(LocalMcastMacsBuilder mMacLocalBuilder, McastMacsLocal mMacLocal) {
        if(mMacLocal.getIpAddr() != null && !mMacLocal.getIpAddr().isEmpty()) {
            mMacLocalBuilder.setIpaddr(new IpAddress(mMacLocal.getIpAddr().toCharArray()));
        }
    }

    private void setLocatorSet(LocalMcastMacsBuilder mMacLocalBuilder, McastMacsLocal mMacLocal) {
        if (mMacLocal.getLocatorSetColumn() != null && mMacLocal.getLocatorSetColumn().getData() != null) {
            UUID pLocSetUUID = mMacLocal.getLocatorSetColumn().getData();
            if (updatedPLocSetRows.get(pLocSetUUID) != null) {
                PhysicalLocatorSet plSet = updatedPLocSetRows.get(pLocSetUUID);
                if (plSet.getLocatorsColumn() != null && plSet.getLocatorsColumn().getData() != null
                                && !plSet.getLocatorsColumn().getData().isEmpty()) {
                    List<LocatorSet> plsList = new ArrayList<>();
                    for (UUID pLocUUID : plSet.getLocatorsColumn().getData()) {
                        PhysicalLocator pLoc = updatedPLocRows.get(pLocUUID);
                        InstanceIdentifier<TerminationPoint> tpIid = HwvtepSouthboundMapper.createInstanceIdentifier(
                                        getOvsdbConnectionInstance().getInstanceIdentifier(), pLoc);
                        plsList.add(new LocatorSetBuilder()
                                    .setLocatorRef(new HwvtepPhysicalLocatorRef(tpIid)).build());
                    }
                    mMacLocalBuilder.setLocatorSet(plsList);
                }
            }
        }
    }

}
