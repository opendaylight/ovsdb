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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.Manager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Managers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.ManagersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.managers.ManagerOtherConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepManagerUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepManagerUpdateCommand.class);
    private Map<UUID, Manager> updatedMgrRows;
    private Map<UUID, Manager> oldMgrRows;

    public HwvtepManagerUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedMgrRows = TyperUtils.extractRowsUpdated(Manager.class, getUpdates(),getDbSchema());
        oldMgrRows = TyperUtils.extractRowsOld(Manager.class, getUpdates(),getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if(updatedMgrRows != null && !updatedMgrRows.isEmpty()){
            for(Entry<UUID, Manager> entry : updatedMgrRows.entrySet()){
                updateManager(transaction, entry.getValue());
            }
        }
    }

    private void updateManager(ReadWriteTransaction transaction, Manager manager) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            LOG.debug("Connection {} is present",connection);
            Node connectionNode = buildConnectionNode(manager);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
//            TODO: Delete entries that are no longer needed
        }
    }

    private Node buildConnectionNode(Manager manager) {
      //Update node with Manager reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<Managers> mList= new ArrayList<>();
        ManagersBuilder mBuilder = new ManagersBuilder();
        if (manager.getTargetColumn().getData() != null && ! manager.getTargetColumn().getData().isEmpty()){
            mBuilder.setTarget(new Uri(manager.getTargetColumn().getData()));
        }
        if(manager.getIsConnectedColumn().getData() != null ){
            mBuilder.setIsConnected(manager.getIsConnectedColumn().getData());
        }
        ManagerOtherConfigsBuilder  mocBuilder = new ManagerOtherConfigsBuilder();
        List<ManagerOtherConfigs> mocList = new ArrayList<>();
        if(manager.getOtherConfigColumn().getData() != null
                && !manager.getOtherConfigColumn().getData().isEmpty()){
            Map<String, String> ocList = manager.getOtherConfigColumn().getData();
            for(String key : ocList.keySet()){
                mocBuilder.setOtherConfigKey(key);
                mocBuilder.setOtherConfigValue(ocList.get(key));
                mocList.add(mocBuilder.build());
            }
        }
        mBuilder.setManagerUuid(new Uuid(manager.getUuid().toString()));
        if(mocList != null && !mocList.isEmpty()){
            mBuilder.setManagerOtherConfigs(mocList);
        }
        mList.add(mBuilder.build());
        hgAugmentationBuilder.setManagers(mList);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

}


