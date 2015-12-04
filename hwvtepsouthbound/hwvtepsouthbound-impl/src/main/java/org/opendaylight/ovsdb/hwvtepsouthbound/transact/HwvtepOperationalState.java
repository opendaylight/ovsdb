/*
 * Copyright (c) 2015 China Telecom Beijing Research Institute and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalLocatorAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalPortAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.PhysicalSwitchAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LocalUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class HwvtepOperationalState {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepOperationalState.class);
    private Map<InstanceIdentifier<Node>, Node> operationalNodes = new HashMap<>();
    public DataBroker db;

    public HwvtepOperationalState(DataBroker db, Collection<DataTreeModification<Node>> changes) {
        this.db = db;
        Map<InstanceIdentifier<Node>, Node> nodeCreateOrUpdate =
            TransactUtils.extractCreatedOrUpdatedOrRemoved(changes, Node.class);
        if (nodeCreateOrUpdate != null) {
            final ReadWriteTransaction transaction = db.newReadWriteTransaction();
            for (Entry<InstanceIdentifier<Node>, Node> entry: nodeCreateOrUpdate.entrySet()) {
                Optional<Node> readNode = HwvtepSouthboundUtil.readNode(transaction, entry.getKey());
                //add related globalNode or physicalSwitchNode to operationalNodes map
                //for example, when creating physical port, logical switch is needed
                //but logical switch is in HwvtepGlobalAugmentation rather than PhysicalSwitchAugmentation
                if (readNode.isPresent()) {
                    operationalNodes.put(entry.getKey(), readNode.get());
                    HwvtepGlobalAugmentation hgAugmentation = readNode.get().getAugmentation(HwvtepGlobalAugmentation.class);
                    PhysicalSwitchAugmentation psAugmentation = readNode.get().getAugmentation(PhysicalSwitchAugmentation.class);
                    if (hgAugmentation != null) {
                        for (Switches pswitch : hgAugmentation.getSwitches()) {
                            @SuppressWarnings("unchecked")
                            InstanceIdentifier<Node> psNodeIid = (InstanceIdentifier<Node>) pswitch.getSwitchRef().getValue();
                            Optional<Node> psNode = HwvtepSouthboundUtil.readNode(transaction, psNodeIid);
                            if (psNode.isPresent()) {
                                operationalNodes.put(psNodeIid, psNode.get());
                            }
                        }
                    }
                    if (psAugmentation != null) {
                        @SuppressWarnings("unchecked")
                        InstanceIdentifier<Node> hgNodeIid = (InstanceIdentifier<Node>) psAugmentation.getManagedBy().getValue();
                        Optional<Node> hgNode = HwvtepSouthboundUtil.readNode(transaction, hgNodeIid);
                        if (hgNode.isPresent()) {
                            operationalNodes.put(hgNodeIid, hgNode.get());
                        }
                    }
                }
            }
        }
    }

    private Optional<Node> getGlobalNode(InstanceIdentifier<?> iid) {
        InstanceIdentifier<Node> nodeIid = iid.firstIdentifierOf(Node.class);
        return Optional.fromNullable(operationalNodes.get(nodeIid));
    }

    public Optional<HwvtepGlobalAugmentation> getHwvtepGlobalAugmentation(InstanceIdentifier<?> iid) {
        Preconditions.checkNotNull(iid);
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(HwvtepGlobalAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<PhysicalSwitchAugmentation> getPhysicalSwitchAugmentation(InstanceIdentifier<?> iid) {
        Preconditions.checkNotNull(iid);
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent()) {
            return Optional.fromNullable(nodeOptional.get().getAugmentation(PhysicalSwitchAugmentation.class));
        }
        return Optional.absent();
    }

    public Optional<List<TerminationPoint>> getTerminationPointList(InstanceIdentifier<?> iid) {
        Preconditions.checkNotNull(iid);
        Optional<Node> nodeOptional = getGlobalNode(iid);
        if (nodeOptional.isPresent() && nodeOptional.get().getTerminationPoint() != null) {
            return Optional.fromNullable(nodeOptional.get().getTerminationPoint());
        }
        return Optional.absent();
    }

    public Optional<LogicalSwitches> getLogicalSwitches(InstanceIdentifier<?> iid, LogicalSwitchesKey logicalSwitchesKey) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<LogicalSwitches> lswitchList = null;
            if (hgAugmentation != null) {
                lswitchList = hgAugmentation.getLogicalSwitches();
            }
            if (lswitchList != null) {
                for (LogicalSwitches lswitch: lswitchList) {
                    if (lswitch.getKey().equals(logicalSwitchesKey)) {
                        return Optional.fromNullable(lswitch);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalPortAugmentation> getPhysicalPortAugmentation(InstanceIdentifier<?> iid,
            HwvtepNodeName hwvtepNodeName) {
        Preconditions.checkNotNull(iid);
        Optional<List<TerminationPoint>> nodeOptional = getTerminationPointList(iid);
        if (nodeOptional.isPresent()) {
            List<TerminationPoint> tpList = nodeOptional.get();
            for (TerminationPoint tp : tpList) {
                HwvtepPhysicalPortAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalPortAugmentation.class);
                if (hppAugmentation.getHwvtepNodeName().equals(hwvtepNodeName)) {
                    return Optional.fromNullable(hppAugmentation);
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation> getPhysicalLocatorAugmentation(InstanceIdentifier<?> iid,
            IpAddress dstIp, Class<? extends EncapsulationTypeBase> encapType) {
        Preconditions.checkNotNull(iid);
        Optional<List<TerminationPoint>> nodeOptional = getTerminationPointList(iid);
        if (nodeOptional.isPresent()) {
            List<TerminationPoint> tpList = nodeOptional.get();
            for (TerminationPoint tp : tpList) {
                HwvtepPhysicalLocatorAugmentation hppAugmentation = tp.getAugmentation(HwvtepPhysicalLocatorAugmentation.class);
                if (hppAugmentation.getDstIp().equals(dstIp)
                        && hppAugmentation.getEncapsulationType().equals(encapType)) {
                    return Optional.fromNullable(hppAugmentation);
                }
            }
        }
        return Optional.absent();
    }

    public Optional<LocalMcastMacs> getLocalMcastMacs(InstanceIdentifier<?> iid, LocalMcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<LocalMcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getLocalMcastMacs();
            }
            if (macList != null) {
                for (LocalMcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<RemoteMcastMacs> getRemoteMcastMacs(InstanceIdentifier<?> iid, RemoteMcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<RemoteMcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getRemoteMcastMacs();
            }
            if (macList != null) {
                for (RemoteMcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<LocalUcastMacs> getLocalUcastMacs(InstanceIdentifier<?> iid, LocalUcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<LocalUcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getLocalUcastMacs();
            }
            if (macList != null) {
                for (LocalUcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<RemoteUcastMacs> getRemoteUcastMacs(InstanceIdentifier<?> iid, RemoteUcastMacsKey key) {
        Preconditions.checkNotNull(iid);
        Optional<HwvtepGlobalAugmentation> nodeOptional = getHwvtepGlobalAugmentation(iid);
        if (nodeOptional.isPresent()) {
            HwvtepGlobalAugmentation hgAugmentation = nodeOptional.get();
            List<RemoteUcastMacs> macList = null;
            if (hgAugmentation != null) {
                macList = hgAugmentation.getRemoteUcastMacs();
            }
            if (macList != null) {
                for (RemoteUcastMacs mac: macList) {
                    if (mac.getKey().equals(key)) {
                        return Optional.fromNullable(mac);
                    }
                }
            }
        }
        return Optional.absent();
    }

    public Optional<HwvtepPhysicalLocatorAugmentation> getPhysicalLocatorAugmentation(InstanceIdentifier<TerminationPoint> iid) {
        final ReadWriteTransaction transaction = db.newReadWriteTransaction();
        Optional<TerminationPoint> tp = HwvtepSouthboundUtil.readNode(transaction, iid);
        if (tp.isPresent()) {
            return Optional.fromNullable(tp.get().getAugmentation(HwvtepPhysicalLocatorAugmentation.class));
        }
        return Optional.absent();
    }
}
