/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class OvsdbControllerUpdateCommand extends AbstractTransactionCommand {


    private static final Logger LOG = LoggerFactory.getLogger(OvsdbControllerUpdateCommand.class);
    private Map<UUID, Controller> updatedControllerRows;
    private Map<UUID, Bridge> updatedBridgeRows;

    public OvsdbControllerUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        updatedControllerRows = TyperUtils.extractRowsUpdated(Controller.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if ( (updatedBridgeRows == null && updatedControllerRows == null )
                || ( updatedBridgeRows.isEmpty() && updatedControllerRows.isEmpty())) {
            return;
        }
        for (Bridge bridge: updatedBridgeRows.values()) {
            System.out.println(bridge);
            setController(transaction, bridge);
        }
        for (Controller controller: updatedControllerRows.values()) {
            System.out.println(controller);
            updateController(transaction, controller);
        }
    }

    private void setController(ReadWriteTransaction transaction, Bridge bridge) {
        for (ControllerEntry controllerEntry: SouthboundMapper.createControllerEntries(bridge, updatedControllerRows)) {
            InstanceIdentifier<ControllerEntry> iid =
                    SouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), bridge)
                    .augmentation(OvsdbBridgeAugmentation.class)
                    .child(ControllerEntry.class,controllerEntry.getKey());
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, controllerEntry);
        }
    }

    private void updateController(ReadWriteTransaction transaction, Controller controller) {
        Uri controllerTarget = new Uri(getControllerTarget());
        ControllerEntryKey controllerEntryKey = new ControllerEntryKey(controllerTarget);
        ControllerEntry controllerEntry = new ControllerEntryBuilder()
                .setControllerUuid(new Uuid(controller.getUuid().toString()))
                .setIsConnected(controller.getIsConnectedColumn().getData())
                .setKey(controllerEntryKey)
                .setTarget(new Uri(controllerTarget))
                .build();

        Optional<String> bridgeName = getControllerBridgeName(transaction);
        if (bridgeName.isPresent()) {
            InstanceIdentifier<ControllerEntry> iid = SouthboundMapper
                    .createInstanceIdentifier(getOvsdbConnectionInstance(),
                            controller, bridgeName.get())
                    .augmentation(OvsdbBridgeAugmentation.class)
                    .child(ControllerEntry.class, controllerEntry.getKey());
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, controllerEntry);
        }
    }

    private Optional<String> getControllerBridgeName(
            final ReadWriteTransaction transaction) {
        CheckedFuture<Optional<Node>, ReadFailedException> ovsdbNodeFuture = transaction.read(
                LogicalDatastoreType.OPERATIONAL, getOvsdbConnectionInstance().getInstanceIdentifier());
        Optional<Node> ovsdbNodeOptional;
        try {
            ovsdbNodeOptional = ovsdbNodeFuture.get();
            if (ovsdbNodeOptional.isPresent()) {
                Node ovsdbNode = ovsdbNodeOptional.get();
                OvsdbBridgeAugmentation ovsdbBridgeAugmentation = ovsdbNode
                        .getAugmentation(OvsdbBridgeAugmentation.class);
                if (ovsdbBridgeAugmentation == null) {
                    LOG.warn("{} had no OvsdbNodeAugmentation", ovsdbNode);
                } else {
                    return Optional.of(ovsdbBridgeAugmentation.getBridgeName().getValue());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failure to delete ovsdbNode {}",e);
        }
        return Optional.absent();
    }

    private String getControllerTarget() {
        /* TODO SB_MIGRATION
         * hardcoding value, need to find better way to get local ip
         */
        //String target = "tcp:" + getControllerIPAddress() + ":" + getControllerOFPort();
        //TODO: dirty fix, need to remove it once we have proper solution
        String ipaddress = null;
        try {
            for (@SuppressWarnings("rawtypes")
            Enumeration ifaces = NetworkInterface.getNetworkInterfaces();ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                for (@SuppressWarnings("rawtypes")
                Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            ipaddress = inetAddr.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("ROYALLY SCREWED : Exception while fetching local host ip address ",e);
        }
        return "tcp:" + ipaddress + ":6633";
    }
}
