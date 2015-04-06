/*
 * Copyright (c) 2015 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.external.ids.attributes.ExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.other.config.attributes.OtherConfigs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class OvsdbNodeUpdateCommand implements TransactCommand {
    private AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbNodeAugmentation> changes;
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeUpdateCommand.class);


    public OvsdbNodeUpdateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>, OvsdbNodeAugmentation> changes) {
        this.changes = changes;
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, OvsdbNodeAugmentation> updated =
                TransactUtils.extractOvsdbNodeUpdate(changes);
        for (Entry<InstanceIdentifier<Node>, OvsdbNodeAugmentation> ovsdbNodeEntry:
            updated.entrySet()) {
            OvsdbNodeAugmentation ovsdbNode = ovsdbNodeEntry.getValue();
            LOG.debug("Received request to update ovsdb node ip: {} port: {}",
                        ovsdbNode.getIp(),
                        ovsdbNode.getPort());

            // OpenVSwitchPart
            OpenVSwitch ovs = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(), OpenVSwitch.class);

            List<ExternalIds> externalIds = ovsdbNode.getExternalIds();
            if (externalIds != null) {
                HashMap<String, String> externalIdsMap = new HashMap<String, String>();
                for (ExternalIds externalId : externalIds) {
                    externalIdsMap.put(externalId.getExternalIdKey(), externalId.getExternalIdValue());
                }
                try {
                    ovs.setExternalIds(ImmutableMap.copyOf(externalIdsMap));
                    transaction.add(op.mutate(ovs).addMutation(ovs.getExternalIdsColumn().getSchema(),
                        Mutator.INSERT,
                        ovs.getExternalIdsColumn().getData()));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB Node external IDs");
                }
            }

            List<OtherConfigs> otherConfigs = ovsdbNode.getOtherConfigs();
            if (otherConfigs != null) {
                HashMap<String, String> otherConfigsMap = new HashMap<String, String>();
                for (OtherConfigs otherConfig : otherConfigs) {
                    otherConfigsMap.put(otherConfig.getOtherConfigKey(), otherConfig.getOtherConfigValue());
                }
                try {
                    ovs.setOtherConfig(ImmutableMap.copyOf(otherConfigsMap));
                    transaction.add(op.mutate(ovs).addMutation(ovs.getOtherConfigColumn().getSchema(),
                        Mutator.INSERT,
                        ovs.getOtherConfigColumn().getData()));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB Node other_config");
                }
            }
        }
    }
}
