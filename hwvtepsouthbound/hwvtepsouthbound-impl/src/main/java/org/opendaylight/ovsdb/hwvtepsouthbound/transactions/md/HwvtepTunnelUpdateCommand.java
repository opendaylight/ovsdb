/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepTunnelUpdateCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepTunnelUpdateCommand.class);
    private Map<UUID, Tunnel> updatedTunnelRows;

    public HwvtepTunnelUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        try {
            updatedTunnelRows = TyperUtils.extractRowsUpdated(Tunnel.class, getUpdates(), getDbSchema());
        } catch (IllegalArgumentException e) {
            LOG.debug("Tunnel Table not supported on this HWVTEP device", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void execute(ReadWriteTransaction transaction) {
        if (updatedTunnelRows != null && !updatedTunnelRows.isEmpty()) {
            for (Tunnel tunnel : updatedTunnelRows.values()) {
                try {
                    updateTunnel(transaction, tunnel);
                } catch (RuntimeException e) {
                    LOG.warn("Exception updating tunnel {}", tunnel, e);
                }
            }
        }
    }

    private void updateTunnel(ReadWriteTransaction transaction, Tunnel tunnel) {
        Preconditions.checkNotNull(tunnel.getLocalColumn().getData());
        Preconditions.checkNotNull(tunnel.getRemoteColumn().getData());
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        //TODO remove these reads
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        PhysicalSwitch phySwitch =
                        getOvsdbConnectionInstance().getDeviceInfo().getPhysicalSwitchForTunnel(tunnel.getUuid());
        InstanceIdentifier<Node> psIid =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), phySwitch);
        InstanceIdentifier<Tunnels> tunnelIid = getInstanceIdentifier(psIid, tunnel);
        if (connection.isPresent() && phySwitch != null && tunnelIid != null) {
            TunnelsBuilder builder = new TunnelsBuilder();
            builder.setLocalLocatorRef(new HwvtepPhysicalLocatorRef(getPhysicalLocatorRefFromUUID(
                    getOvsdbConnectionInstance().getInstanceIdentifier(), tunnel.getLocalColumn().getData())));
            builder.setRemoteLocatorRef(new HwvtepPhysicalLocatorRef(getPhysicalLocatorRefFromUUID(
                    getOvsdbConnectionInstance().getInstanceIdentifier(), tunnel.getRemoteColumn().getData())));
            builder.setTunnelUuid(new Uuid(tunnel.getUuid().toString()));
            setBfdLocalConfigs(builder, tunnel);
            setBfdRemoteConfigs(builder, tunnel);
            setBfdParams(builder, tunnel);
            setBfdStatus(builder, tunnel);
            Tunnels updatedTunnel = builder.build();
            LOG.trace("Built with the intent to store tunnel data {}", updatedTunnel);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, tunnelIid, updatedTunnel);
            // TODO: Deletion of Tunnel BFD config and params
        } else {
            LOG.warn("Insuficient information. Unable to update tunnel {}", tunnel.getUuid());
        }
    }

    private void setBfdLocalConfigs(TunnelsBuilder tunnelsBuilder, Tunnel tunnel) {
        Map<String, String> localConfigs = tunnel.getBfdConfigLocalColumn().getData();
        if (localConfigs != null && !localConfigs.isEmpty()) {
            Set<String> localConfigKeys = localConfigs.keySet();
            List<BfdLocalConfigs> localConfigsList = new ArrayList<>();
            String localConfigValue = null;
            for (String localConfigKey: localConfigKeys) {
                localConfigValue = localConfigs.get(localConfigKey);
                if (localConfigValue != null && localConfigKey != null) {
                    localConfigsList.add(new BfdLocalConfigsBuilder()
                        .setBfdLocalConfigKey(localConfigKey)
                        .setBfdLocalConfigValue(localConfigValue)
                        .build());
                }
            }
            tunnelsBuilder.setBfdLocalConfigs(localConfigsList);
        }
    }

    private void setBfdRemoteConfigs(TunnelsBuilder tunnelsBuilder, Tunnel tunnel) {
        Map<String, String> remoteConfigs = tunnel.getBfdConfigRemoteColumn().getData();
        if (remoteConfigs != null && !remoteConfigs.isEmpty()) {
            Set<String> remoteConfigKeys = remoteConfigs.keySet();
            List<BfdRemoteConfigs> remoteConfigsList = new ArrayList<>();
            String remoteConfigValue = null;
            for (String remoteConfigKey: remoteConfigKeys) {
                remoteConfigValue = remoteConfigs.get(remoteConfigKey);
                if (remoteConfigValue != null && remoteConfigKey != null) {
                    remoteConfigsList.add(new BfdRemoteConfigsBuilder()
                        .setBfdRemoteConfigKey(remoteConfigKey)
                        .setBfdRemoteConfigValue(remoteConfigValue)
                        .build());
                }
            }
            tunnelsBuilder.setBfdRemoteConfigs(remoteConfigsList);
        }
    }


    private void setBfdParams(TunnelsBuilder tunnelsBuilder, Tunnel tunnel) {
        Map<String, String> params = tunnel.getBfdParamsColumn().getData();
        if (params != null && !params.isEmpty()) {
            Set<String> paramKeys = params.keySet();
            List<BfdParams> paramsList = new ArrayList<>();
            String paramValue = null;
            for (String paramKey: paramKeys) {
                paramValue = params.get(paramKey);
                if (paramValue != null && paramKey != null) {
                    paramsList.add(new BfdParamsBuilder()
                        .setBfdParamKey(paramKey)
                        .setBfdParamValue(paramValue)
                        .build());
                }
            }
            tunnelsBuilder.setBfdParams(paramsList);
        }
    }

    private void setBfdStatus(TunnelsBuilder tunnelsBuilder, Tunnel tunnel) {
        Map<String, String> status = tunnel.getBfdStatusColumn().getData();
        if (status != null && !status.isEmpty()) {
            Set<String> paramKeys = status.keySet();
            List<BfdStatus> statusList = new ArrayList<>();
            String paramValue = null;
            for (String paramKey: paramKeys) {
                paramValue = status.get(paramKey);
                if (paramValue != null && paramKey != null) {
                    statusList.add(new BfdStatusBuilder()
                        .setBfdStatusKey(paramKey)
                        .setBfdStatusValue(paramValue)
                        .build());
                }
            }
            tunnelsBuilder.setBfdStatus(statusList);
        }
    }

    private InstanceIdentifier<Tunnels> getInstanceIdentifier(InstanceIdentifier<Node> psIid, Tunnel tunnel) {
        InstanceIdentifier<Tunnels> result = null;
        InstanceIdentifier<TerminationPoint> localTpPath =
                        getPhysicalLocatorRefFromUUID(getOvsdbConnectionInstance().getInstanceIdentifier(),
                                                        tunnel.getLocalColumn().getData());
        InstanceIdentifier<TerminationPoint> remoteTpPath =
                        getPhysicalLocatorRefFromUUID(getOvsdbConnectionInstance().getInstanceIdentifier(),
                                                        tunnel.getRemoteColumn().getData());
        if (remoteTpPath != null && localTpPath != null) {
            result = HwvtepSouthboundMapper.createInstanceIdentifier(psIid, localTpPath, remoteTpPath);
        }
        return result;
    }

    private InstanceIdentifier<TerminationPoint> getPhysicalLocatorRefFromUUID(InstanceIdentifier<Node> nodeIid,
                                                                               UUID uuid) {
        PhysicalLocator locator = getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocator(uuid);
        if (locator == null) {
            LOG.trace("Available PhysicalLocators: ",
                            getOvsdbConnectionInstance().getDeviceInfo().getPhysicalLocators());
            return null;
        }
        return HwvtepSouthboundMapper.createInstanceIdentifier(nodeIid, locator);
    }
}
