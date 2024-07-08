/*
 * Copyright © 2015, 2017 Intel Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.ovsdb.transact;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.operations.Mutate;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.utils.yang.YangUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchExternalIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.OpenvswitchOtherConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbNodeUpdateCommand implements TransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeUpdateCommand.class);

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final DataChangeEvent events, final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, TransactUtils.extractCreatedOrUpdated(events, OvsdbNodeAugmentation.class),
                instanceIdentifierCodec);
    }

    @Override
    public void execute(final TransactionBuilder transaction, final BridgeOperationalState state,
            final Collection<DataTreeModification<Node>> modifications,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        execute(transaction, TransactUtils.extractCreatedOrUpdated(modifications, OvsdbNodeAugmentation.class),
                instanceIdentifierCodec);
    }

    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    private static void execute(final TransactionBuilder transaction,
            final Map<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> updated,
            final InstanceIdentifierCodec instanceIdentifierCodec) {
        for (Entry<InstanceIdentifier<OvsdbNodeAugmentation>, OvsdbNodeAugmentation> ovsdbNodeEntry:
            updated.entrySet()) {
            OvsdbNodeAugmentation ovsdbNode = ovsdbNodeEntry.getValue();
            if (ovsdbNode.getConnectionInfo() != null) {
                LOG.debug("Received request to update ovsdb node ip: {} port: {}",
                        ovsdbNode.getConnectionInfo().getRemoteIp(),
                        ovsdbNode.getConnectionInfo().getRemotePort());
            } else {
                LOG.debug("Received request to update ovsdb node: {}", ovsdbNode);
            }

            // OpenVSwitchPart
            OpenVSwitch ovs = transaction.getTypedRowWrapper(OpenVSwitch.class);

            stampInstanceIdentifier(transaction, ovsdbNodeEntry.getKey().firstIdentifierOf(Node.class),
                    instanceIdentifierCodec);

            try {
                ovs.setExternalIds(YangUtils.convertYangKeyValueListToMap(ovsdbNode.getOpenvswitchExternalIds(),
                        OpenvswitchExternalIds::getExternalIdKey, OpenvswitchExternalIds::getExternalIdValue));
                Mutate<GenericTableSchema> mutate = op.mutate(ovs)
                            .addMutation(ovs.getExternalIdsColumn().getSchema(),
                                Mutator.INSERT,
                                ovs.getExternalIdsColumn().getData());
                transaction.add(mutate);
            } catch (NullPointerException e) {
                LOG.warn("Incomplete OVSDB Node external IDs", e);
            }



            Map<OpenvswitchOtherConfigsKey, OpenvswitchOtherConfigs> otherConfigs =
                    ovsdbNode.getOpenvswitchOtherConfigs();
            if (otherConfigs != null) {
                try {
                    ovs.setOtherConfig(YangUtils.convertYangKeyValueListToMap(otherConfigs,
                            OpenvswitchOtherConfigs::getOtherConfigKey, OpenvswitchOtherConfigs::getOtherConfigValue));
                    transaction.add(op.mutate(ovs).addMutation(ovs.getOtherConfigColumn().getSchema(),
                        Mutator.INSERT,
                        ovs.getOtherConfigColumn().getData()));
                } catch (NullPointerException e) {
                    LOG.warn("Incomplete OVSDB Node other_config", e);
                }
            }
        }
    }

    private static void stampInstanceIdentifier(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> iid, final InstanceIdentifierCodec instanceIdentifierCodec) {
        OpenVSwitch ovs = transaction.getTypedRowWrapper(OpenVSwitch.class);
        ovs.setExternalIds(Collections.emptyMap());
        TransactUtils.stampInstanceIdentifier(transaction, iid, ovs.getSchema(),
                ovs.getExternalIdsColumn().getSchema(), instanceIdentifierCodec);
    }
}
