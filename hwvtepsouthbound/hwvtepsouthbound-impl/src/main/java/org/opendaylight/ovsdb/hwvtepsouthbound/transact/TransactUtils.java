/*
 * Copyright © 2015, 2017 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepDeviceInfo;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundConstants;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocatorSet;
import org.opendaylight.ovsdb.utils.mdsal.utils.ControllerMdsalUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeVxlanOverIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TransactUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransactUtils.class);

    private TransactUtils(){
    }

    public static Node getCreated(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.WRITE
                        && mod.getDataBefore() == null) {
            return mod.getDataAfter();
        }
        return null;
    }

    public static Node getRemoved(DataObjectModification<Node> mod) {
        if (mod.getModificationType() == ModificationType.DELETE) {
            return mod.getDataBefore();
        }
        return null;
    }

    public static Node getUpdated(DataObjectModification<Node> mod) {
        Node node = null;
        switch (mod.getModificationType()) {
            case SUBTREE_MODIFIED:
                node = mod.getDataAfter();
                break;
            case WRITE:
                if (mod.getDataBefore() != null) {
                    node = mod.getDataAfter();
                }
                break;
            default:
                break;
        }
        return node;
    }

    public static Node getOriginal(DataObjectModification<Node> mod) {
        Node node = null;
        switch (mod.getModificationType()) {
            case SUBTREE_MODIFIED:
            case DELETE:
                node = mod.getDataBefore();
                break;
            case WRITE:
                if (mod.getDataBefore() !=  null) {
                    node = mod.getDataBefore();
                }
                break;
            default:
                break;
        }
        return node;
    }

    //TODO: change this function to be generic
    public static Map<InstanceIdentifier<Node>, Node> extractCreatedOrUpdatedOrRemoved(
            Collection<DataTreeModification<Node>> changes, Class<Node> class1) {
        Map<InstanceIdentifier<Node>, Node> result = new HashMap<>();
        for (DataTreeModification<Node> change : changes) {
            final InstanceIdentifier<Node> key = change.getRootPath().getRootIdentifier();
            final DataObjectModification<Node> mod = change.getRootNode();
            Node created = getCreated(mod);
            if (created != null) {
                result.put(key, created);
            }
            Node updated = getUpdated(mod);
            if (updated != null) {
                result.put(key, updated);
            }
            Node deleted = getRemoved(mod);
            if (deleted != null) {
                result.put(key, deleted);
            }
        }
        return result;
    }

    public static UUID createPhysicalLocatorSet(HwvtepOperationalState hwvtepOperationalState,
            TransactionBuilder transaction, List<LocatorSet> locatorList) {
        Set<UUID> locators = new HashSet<>();
        for (LocatorSet locator: locatorList) {
            @SuppressWarnings("unchecked")
            InstanceIdentifier<TerminationPoint> iid =
                    (InstanceIdentifier<TerminationPoint>) locator.getLocatorRef().getValue();
            UUID locatorUuid = createPhysicalLocator(transaction, hwvtepOperationalState, iid);
            if (locatorUuid != null) {
                locators.add(locatorUuid);
            }
        }
        PhysicalLocatorSet physicalLocatorSet = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                PhysicalLocatorSet.class);
        physicalLocatorSet.setLocators(locators);
        String locatorSetUuid = "PhysicalLocatorSet_" + HwvtepSouthboundMapper.getRandomUUID();
        transaction.add(op.insert(physicalLocatorSet).withId(locatorSetUuid));
        return new UUID(locatorSetUuid);
    }

    public static UUID createPhysicalLocator(TransactionBuilder transaction, HwvtepOperationalState operationalState,
                                             InstanceIdentifier<TerminationPoint> iid) {
        UUID locatorUuid = null;
        HwvtepDeviceInfo.DeviceData deviceData = operationalState.getDeviceInfo().getDeviceOperData(
                TerminationPoint.class, iid);
        if (deviceData != null && deviceData.getUuid() != null) {
            locatorUuid = deviceData.getUuid();
            return locatorUuid;
        }
        locatorUuid = operationalState.getUUIDFromCurrentTx(TerminationPoint.class, iid);
        if (locatorUuid != null) {
            return locatorUuid;
        }
        HwvtepPhysicalLocatorAugmentationBuilder builder = new HwvtepPhysicalLocatorAugmentationBuilder();
        HwvtepPhysicalLocatorAugmentation locatorAugmentation = null;
        builder.setEncapsulationType(EncapsulationTypeVxlanOverIpv4.class);
        String tepKey = iid.firstKeyOf(TerminationPoint.class).getTpId().getValue();
        String ip = tepKey.substring(tepKey.indexOf(":") + 1);
        builder.setDstIp(IpAddressBuilder.getDefaultInstance(ip));
        locatorAugmentation = builder.build();
        locatorUuid = TransactUtils.createPhysicalLocator(transaction, locatorAugmentation);
        operationalState.updateCurrentTxData(TerminationPoint.class, iid, locatorUuid);
        operationalState.getDeviceInfo().markKeyAsInTransit(TerminationPoint.class, iid);
        return locatorUuid;
    }

    public static UUID createPhysicalLocator(TransactionBuilder transaction,
            HwvtepPhysicalLocatorAugmentation inputLocator) {
        LOG.debug("Creating a physical locator: {}", inputLocator.getDstIp());
        PhysicalLocator physicalLocator = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                PhysicalLocator.class);
        setEncapsulationType(physicalLocator, inputLocator);
        setDstIp(physicalLocator, inputLocator);
        String locatorUuid = "PhysicalLocator_" + HwvtepSouthboundMapper.getRandomUUID();
        transaction.add(op.insert(physicalLocator).withId(locatorUuid));
        return new UUID(locatorUuid);
    }

    private static void setEncapsulationType(PhysicalLocator physicalLocator,
            HwvtepPhysicalLocatorAugmentation inputLocator) {
        if (inputLocator.getEncapsulationType() != null) {
            String encapType = HwvtepSouthboundConstants.ENCAPS_TYPE_MAP.get(
                    HwvtepSouthboundMapper.createEncapsulationType(""));
            physicalLocator.setEncapsulationType(encapType);
        }
    }

    private static void setDstIp(PhysicalLocator physicalLocator,
            HwvtepPhysicalLocatorAugmentation inputLocator) {
        if (inputLocator.getDstIp() != null) {
            physicalLocator.setDstIp(inputLocator.getDstIp().getIpv4Address().getValue());
        }
    }

    static String sanitizeUUID(HwvtepNodeName hwvtepNodeName) {
        return sanitizeUUID(hwvtepNodeName.getValue());
    }

    static String sanitizeUUID(String nodeName) {
        //ovs is not accepting '-' in the named uuids
        return nodeName.replaceAll("-", "_");
    }

    public static String getLogicalSwitchId(LogicalSwitches lswitch) {
        return HwvtepSouthboundConstants.LOGICALSWITCH_UUID_PREFIX + sanitizeUUID(lswitch.getHwvtepNodeName());
    }

    public static UUID getLogicalSwitchUUID(InstanceIdentifier<LogicalSwitches> lswitchIid) {
        return new UUID(HwvtepSouthboundConstants.LOGICALSWITCH_UUID_PREFIX
                + sanitizeUUID(lswitchIid.firstKeyOf(LogicalSwitches.class).getHwvtepNodeName()));
    }

    public static UUID getLogicalSwitchUUID(final TransactionBuilder transaction,
                                            final HwvtepOperationalState operationalState,
                                            final InstanceIdentifier<LogicalSwitches> lswitchIid) {
        HwvtepDeviceInfo hwvtepDeviceInfo = operationalState.getDeviceInfo();
        HwvtepDeviceInfo.DeviceData lsData = hwvtepDeviceInfo.getDeviceOperData(LogicalSwitches.class, lswitchIid);
        if (lsData != null) {
            if (lsData.getUuid() != null) {
                return lsData.getUuid();
            }
            if (lsData.isInTransitState()) {
                return getLogicalSwitchUUID(lswitchIid);
            }
            return null;
        }
        LogicalSwitchUpdateCommand cmd = new LogicalSwitchUpdateCommand(operationalState, Collections.emptyList());
        ControllerMdsalUtils mdsalUtils = new ControllerMdsalUtils(operationalState.getDataBroker());
        LogicalSwitches ls = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, lswitchIid);
        if (ls != null) {
            cmd.updateLogicalSwitch(transaction, lswitchIid.firstIdentifierOf(Node.class), Lists.newArrayList(ls));
        } else {
            LOG.error("Could not find logical switch in config ds {}", lswitchIid);
            return null;
        }
        return getLogicalSwitchUUID(lswitchIid);
    }

    public static String getLogicalRouterId(final LogicalRouters lrouter) {
        return HwvtepSouthboundConstants.LOGICALROUTER_UUID_PREFIX + sanitizeUUID(lrouter.getHwvtepNodeName());
    }

    public static UUID getAclUUID(final InstanceIdentifier<Acls> aclIid) {
        return new UUID(HwvtepSouthboundConstants.ACL_UUID_PREFIX
                + sanitizeUUID(aclIid.firstKeyOf(Acls.class).getAclName()));
    }
}
