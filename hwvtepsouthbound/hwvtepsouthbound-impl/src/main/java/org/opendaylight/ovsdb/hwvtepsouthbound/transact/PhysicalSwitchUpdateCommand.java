/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil.schemaMismatchLog;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.error.SchemaVersionMismatchException;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.schema.hardwarevtep.Global;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.Tunnel;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.ManagementIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.TunnelIpsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdLocalConfigsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdParamsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.tunnel.attributes.BfdRemoteConfigsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PhysicalSwitchUpdateCommand extends AbstractTransactCommand {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalSwitchUpdateCommand.class);

    public PhysicalSwitchUpdateCommand(final HwvtepOperationalState state,
            final Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(final TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> created =
                extractCreated(getChanges(),PhysicalSwitchAugmentation.class);
        if (!created.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> physicalSwitchEntry:
                created.entrySet()) {
                updatePhysicalSwitch(transaction,  physicalSwitchEntry.getKey(), physicalSwitchEntry.getValue());
            }
        }
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> updated =
                extractUpdatedSwitches(getChanges(),PhysicalSwitchAugmentation.class);
        if (!updated.isEmpty()) {
            for (Entry<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> physicalSwitchEntry:
                updated.entrySet()) {
                updatePhysicalSwitch(transaction,  physicalSwitchEntry.getKey(), physicalSwitchEntry.getValue());
            }
        }
    }


    private void updatePhysicalSwitch(final TransactionBuilder transaction,
            final InstanceIdentifier<Node> iid, final PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        LOG.debug("Creating a physical switch named: {}", physicalSwitchAugmentation.getHwvtepNodeName());
        Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional =
                getOperationalState().getPhysicalSwitchAugmentation(iid);
        PhysicalSwitch physicalSwitch = transaction.getTypedRowWrapper(PhysicalSwitch.class);
        setDescription(physicalSwitch, physicalSwitchAugmentation);
        setManagementIps(physicalSwitch, physicalSwitchAugmentation);
        setTunnuleIps(physicalSwitch, operationalPhysicalSwitchOptional.orElseThrow());
        try {
            setTunnels(transaction, iid, physicalSwitch, physicalSwitchAugmentation,
                            operationalPhysicalSwitchOptional.isPresent());
        } catch (SchemaVersionMismatchException e) {
            schemaMismatchLog("tunnels", "Physical_Switch", e);
        }
        if (!operationalPhysicalSwitchOptional.isPresent()) {
            //create a physical switch
            setName(physicalSwitch, physicalSwitchAugmentation, operationalPhysicalSwitchOptional);
            String pswitchUuid = "PhysicalSwitch_" + HwvtepSouthboundMapper.getRandomUUID();
            transaction.add(op.insert(physicalSwitch).withId(pswitchUuid));
            transaction.add(op.comment("Physical Switch: Creating "
                    + physicalSwitchAugmentation.getHwvtepNodeName().getValue()));
            //update global table
            Global global = transaction.getTypedRowWrapper(Global.class);
            global.setSwitches(Collections.singleton(new UUID(pswitchUuid)));

            LOG.trace("execute: create physical switch: {}", physicalSwitch);
            transaction.add(op.mutate(global)
                    .addMutation(global.getSwitchesColumn().getSchema(), Mutator.INSERT,
                            global.getSwitchesColumn().getData()));
            transaction.add(op.comment("Global: Mutating "
                            + physicalSwitchAugmentation.getHwvtepNodeName().getValue() + " " + pswitchUuid));
        } else {
            PhysicalSwitchAugmentation updatedPhysicalSwitch = operationalPhysicalSwitchOptional.orElseThrow();
            String existingPhysicalSwitchName = updatedPhysicalSwitch.getHwvtepNodeName().getValue();
            /* In case TOR devices don't allow creation of PhysicalSwitch name might be null
             * as user is only adding configurable parameters to MDSAL like BFD params
             *
             * TODO Note: Consider handling tunnel udpate/remove in separate command
             */
            if (existingPhysicalSwitchName == null) {
                existingPhysicalSwitchName = operationalPhysicalSwitchOptional.orElseThrow()
                    .getHwvtepNodeName().getValue();
            }
            // Name is immutable, and so we *can't* update it.  So we use extraPhysicalSwitch for the schema stuff
            PhysicalSwitch extraPhysicalSwitch = transaction.getTypedRowWrapper(PhysicalSwitch.class);
            extraPhysicalSwitch.setName("");
            LOG.trace("execute: updating physical switch: {}", physicalSwitch);
            transaction.add(op.update(physicalSwitch)
                    .where(extraPhysicalSwitch.getNameColumn().getSchema().opEqual(existingPhysicalSwitchName))
                    .build());
            transaction.add(op.comment("Physical Switch: Updating " + existingPhysicalSwitchName));
        }
    }

    private static void setName(final PhysicalSwitch physicalSwitch,
            final PhysicalSwitchAugmentation physicalSwitchAugmentation,
            final Optional<PhysicalSwitchAugmentation> operationalPhysicalSwitchOptional) {
        if (physicalSwitchAugmentation.getHwvtepNodeName() != null) {
            physicalSwitch.setName(physicalSwitchAugmentation.getHwvtepNodeName().getValue());
        } else if (operationalPhysicalSwitchOptional.isPresent()
                && operationalPhysicalSwitchOptional.orElseThrow().getHwvtepNodeName() != null) {
            physicalSwitch.setName(operationalPhysicalSwitchOptional.orElseThrow().getHwvtepNodeName().getValue());
        }
    }

    private static void setDescription(final PhysicalSwitch physicalSwitch,
            final PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        if (physicalSwitchAugmentation.getHwvtepNodeDescription() != null) {
            physicalSwitch.setDescription(physicalSwitchAugmentation.getHwvtepNodeDescription());
        }
    }

    private static void setManagementIps(final PhysicalSwitch physicalSwitch,
            final PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        Map<ManagementIpsKey, ManagementIps> managementIps = physicalSwitchAugmentation.getManagementIps();
        if (managementIps != null) {
            Set<String> ipSet = new HashSet<>();
            for (ManagementIps ip: managementIps.values()) {
                ipSet.add(ip.getManagementIpsKey().getIpv4Address().getValue());
            }
            physicalSwitch.setManagementIps(ipSet);
        }
    }

    private static void setTunnuleIps(final PhysicalSwitch physicalSwitch,
            final PhysicalSwitchAugmentation physicalSwitchAugmentation) {
        final Map<TunnelIpsKey, TunnelIps> tunnelIps = physicalSwitchAugmentation.getTunnelIps();
        if (tunnelIps != null) {
            Set<String> ipSet = new HashSet<>();
            for (TunnelIps ip: tunnelIps.values()) {
                ipSet.add(ip.getTunnelIpsKey().getIpv4Address().getValue());
            }
            physicalSwitch.setTunnelIps(ipSet);
        }
    }

    @SuppressWarnings("unchecked")
    private void setTunnels(final TransactionBuilder transaction, final InstanceIdentifier<Node> iid,
            final PhysicalSwitch physicalSwitch, final PhysicalSwitchAugmentation physicalSwitchAugmentation,
            final boolean switchExists) {
        //TODO: revisit this code for optimizations
        //TODO: needs more testing
        for (Tunnels tunnel : physicalSwitchAugmentation.nonnullTunnels().values()) {
            Optional<Tunnels> opTunnelOpt = getOperationalState().getTunnels(iid, tunnel.key());
            Tunnel newTunnel = transaction.getTypedRowWrapper(Tunnel.class);

            UUID localUUID = getLocatorUUID(transaction,
                (InstanceIdentifier<TerminationPoint>) tunnel.getLocalLocatorRef().getValue());
            UUID remoteUUID = getLocatorUUID(transaction,
                (InstanceIdentifier<TerminationPoint>) tunnel.getRemoteLocatorRef().getValue());
            if (localUUID != null && remoteUUID != null) {
                // local and remote must exist
                newTunnel.setLocal(localUUID);
                newTunnel.setRemote(remoteUUID);
                setBfdParams(newTunnel, tunnel);
                setBfdLocalConfigs(newTunnel, tunnel);
                setBfdRemoteConfigs(newTunnel, tunnel);
                if (!opTunnelOpt.isPresent()) {
                    String tunnelUuid = "Tunnel_" + HwvtepSouthboundMapper.getRandomUUID();
                    transaction.add(op.insert(newTunnel).withId(tunnelUuid));
                    transaction.add(op.comment("Tunnel: Creating " + tunnelUuid));
                    if (!switchExists) {
                        //TODO: Figure out a way to handle this
                        LOG.warn("Tunnel configuration requires pre-existing physicalSwitch");
                    } else {
                        // TODO: Can we reuse physicalSwitch instead?
                        PhysicalSwitch phySwitch = transaction.getTypedRowWrapper(PhysicalSwitch.class);
                        phySwitch.setTunnels(Collections.singleton(new UUID(tunnelUuid)));
                        phySwitch.setName(physicalSwitchAugmentation.getHwvtepNodeName().getValue());
                        transaction.add(op.mutate(phySwitch)
                            .addMutation(phySwitch.getTunnels().getSchema(), Mutator.INSERT,
                                phySwitch.getTunnels().getData())
                            .where(phySwitch.getNameColumn().getSchema()
                                .opEqual(phySwitch.getNameColumn().getData()))
                            .build());
                        transaction.add(op.comment("PhysicalSwitch: Mutating " + tunnelUuid));
                    }
                } else {
                    UUID uuid = new UUID(opTunnelOpt.orElseThrow().getTunnelUuid().getValue());
                    Tunnel extraTunnel = transaction.getTypedRowSchema(Tunnel.class);
                    extraTunnel.getUuidColumn().setData(uuid);
                    transaction.add(op.update(newTunnel)
                        .where(extraTunnel.getUuidColumn().getSchema().opEqual(uuid))
                        .build());
                    transaction.add(op.comment("Tunnel: Updating " + uuid));
                }
            }
        }
    }

    private static void setBfdParams(final Tunnel tunnel, final Tunnels psAugTunnel) {
        Map<BfdParamsKey, BfdParams> bfdParams = psAugTunnel.getBfdParams();
        if (bfdParams != null) {
            Map<String, String> bfdParamMap = new HashMap<>();
            for (BfdParams bfdParam : bfdParams.values()) {
                bfdParamMap.put(bfdParam.requireBfdParamKey(), bfdParam.requireBfdParamValue());
            }
            tunnel.setBfdParams(ImmutableMap.copyOf(bfdParamMap));
        }
    }

    private static void setBfdLocalConfigs(final Tunnel tunnel, final Tunnels psAugTunnel) {
        Map<BfdLocalConfigsKey, BfdLocalConfigs> bfdLocalConfigs = psAugTunnel.getBfdLocalConfigs();
        if (bfdLocalConfigs != null) {
            Map<String, String> configLocalMap = new HashMap<>();
            for (BfdLocalConfigs localConfig : bfdLocalConfigs.values()) {
                configLocalMap.put(localConfig.requireBfdLocalConfigKey(), localConfig.requireBfdLocalConfigValue());
            }
            tunnel.setBfdConfigLocal(ImmutableMap.copyOf(configLocalMap));
        }
    }

    private static void setBfdRemoteConfigs(final Tunnel tunnel, final Tunnels psAugTunnel) {
        Map<BfdRemoteConfigsKey, BfdRemoteConfigs> bfdRemoteConfigs = psAugTunnel.getBfdRemoteConfigs();
        if (bfdRemoteConfigs != null) {
            Map<String, String> configRemoteMap = new HashMap<>();
            for (BfdRemoteConfigs remoteConfig : bfdRemoteConfigs.values()) {
                configRemoteMap.put(remoteConfig.requireBfdRemoteConfigKey(),
                    remoteConfig.requireBfdRemoteConfigValue());
            }
            tunnel.setBfdConfigRemote(ImmutableMap.copyOf(configRemoteMap));
        }
    }

    private UUID getLocatorUUID(final TransactionBuilder transaction, final InstanceIdentifier<TerminationPoint> iid) {
        UUID locatorUUID = null;
        Optional<HwvtepPhysicalLocatorAugmentation> opLocOptional =
                        getOperationalState().getPhysicalLocatorAugmentation(iid);
        if (opLocOptional.isPresent()) {
            // Get Locator UUID from operational
            HwvtepPhysicalLocatorAugmentation locatorAug = opLocOptional.orElseThrow();
            locatorUUID = new UUID(locatorAug.getPhysicalLocatorUuid().getValue());
        } else {
            // TODO/FIXME: Not in operational, do we create a new one?
            LOG.warn("Trying to create tunnel without creating physical locators first");
            Optional<TerminationPoint> confLocOptional = new MdsalUtils(getOperationalState().getDataBroker())
                    .readOptional(LogicalDatastoreType.CONFIGURATION, iid);
            if (confLocOptional.isPresent()) {
                locatorUUID = TransactUtils.createPhysicalLocator(transaction, getOperationalState(), iid);
            } else {
                LOG.warn("Unable to find endpoint for tunnel. Endpoint indentifier is {}", iid);
            }
        }
        return locatorUUID;
    }

    private static Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractCreated(
            final Collection<DataTreeModification<Node>> changes, final Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node created = TransactUtils.getCreated(mod);
                if (created != null) {
                    PhysicalSwitchAugmentation physicalSwitch =
                            created.augmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }

    private static Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> extractUpdatedSwitches(
            final Collection<DataTreeModification<Node>> changes, final Class<PhysicalSwitchAugmentation> class1) {
        Map<InstanceIdentifier<Node>, PhysicalSwitchAugmentation> result = new HashMap<>();
        if (changes != null && !changes.isEmpty()) {
            for (DataTreeModification<Node> change : changes) {
                final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<Node> mod = change.getRootNode();
                Node updated = TransactUtils.getUpdated(mod);
                if (updated != null) {
                    PhysicalSwitchAugmentation physicalSwitch =
                            updated.augmentation(PhysicalSwitchAugmentation.class);
                    if (physicalSwitch != null) {
                        result.put(key, physicalSwitch);
                    }
                }
            }
        }
        return result;
    }

}
