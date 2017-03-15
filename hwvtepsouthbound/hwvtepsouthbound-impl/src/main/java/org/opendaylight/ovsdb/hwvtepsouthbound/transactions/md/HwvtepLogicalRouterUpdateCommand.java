/*
 * Copyright © 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
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
import org.opendaylight.ovsdb.schema.hardwarevtep.ACL;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepAclRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRoutersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.AclBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.AclBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.SwitchBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.SwitchBindingsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HwvtepLogicalRouterUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HwvtepLogicalRouterUpdateCommand.class);
    private Map<UUID, LogicalRouter> updatedLRRows;

    public HwvtepLogicalRouterUpdateCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedLRRows = TyperUtils.extractRowsUpdated(LogicalRouter.class, getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        for (Entry<UUID, LogicalRouter> entry : updatedLRRows.entrySet()) {
            LOG.trace("Updating logical router {} with {}", entry.getKey(), entry.getValue());
            updateLogicalRouter(transaction, entry.getValue());
        }
    }

    private void updateLogicalRouter(ReadWriteTransaction transaction, final LogicalRouter lRouter) {
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        Optional<Node> connection = HwvtepSouthboundUtil.readNode(transaction, connectionIId);
        if (connection.isPresent()) {
            Node connectionNode = buildConnectionNode(lRouter);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, connectionIId, connectionNode);
            InstanceIdentifier<LogicalRouters> routerIid = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(LogicalRouters.class, new LogicalRoutersKey(new HwvtepNodeName(lRouter.getName())));
            getOvsdbConnectionInstance().getDeviceInfo().updateDeviceOperData(LogicalRouters.class, routerIid,
                    lRouter.getUuid(), lRouter);
        }
    }

    private Node buildConnectionNode(final LogicalRouter lRouter) {
        //Update node with LogicalRouter reference
        NodeBuilder connectionNode = new NodeBuilder();
        connectionNode.setNodeId(getOvsdbConnectionInstance().getNodeId());
        HwvtepGlobalAugmentationBuilder hgAugmentationBuilder = new HwvtepGlobalAugmentationBuilder();
        List<LogicalRouters> lRouters = new ArrayList<>();
        LogicalRoutersBuilder lrBuilder = new LogicalRoutersBuilder();
        lrBuilder.setLogicalRouterUuid(new Uuid(lRouter.getUuid().toString()));
        lrBuilder.setHwvtepNodeDescription(lRouter.getDescription());
        HwvtepNodeName hwvtepName = new HwvtepNodeName(lRouter.getName());
        lrBuilder.setHwvtepNodeName(hwvtepName);
        lrBuilder.setKey(new LogicalRoutersKey(hwvtepName));

        setSwitchBindings(lRouter, lrBuilder);
        setStaticRoutes(lRouter, lrBuilder);
        setAclBindings(lRouter, lrBuilder);

        lRouters.add(lrBuilder.build());
        hgAugmentationBuilder.setLogicalRouters(lRouters);
        connectionNode.addAugmentation(HwvtepGlobalAugmentation.class, hgAugmentationBuilder.build());
        return connectionNode.build();
    }

    private void setStaticRoutes(final LogicalRouter lRouter, final LogicalRoutersBuilder lrBuilder) {
        if (isRouterHasStaticRoutes(lRouter)) {
            List<StaticRoutes> routes = new ArrayList<>();
            for (Entry<String, String> entry : lRouter.getStaticRoutesColumn().getData().entrySet()) {
                StaticRoutesBuilder staticRouteBuilder = new StaticRoutesBuilder();
                staticRouteBuilder.setDestinationAddress(new IpPrefix(new Ipv4Prefix(entry.getKey())));
                staticRouteBuilder.setNexthopAddress(new IpAddress(new Ipv4Address(entry.getKey())));
                routes.add(staticRouteBuilder.build());
            }
            lrBuilder.setStaticRoutes(routes);
        }
    }

    private boolean isRouterHasStaticRoutes(final LogicalRouter lRouter) {
        return lRouter != null && lRouter.getStaticRoutesColumn() != null && lRouter.getStaticRoutesColumn().getData() != null
                && !lRouter.getStaticRoutesColumn().getData().isEmpty();
    }

    private void setAclBindings(final LogicalRouter lRouter, final LogicalRoutersBuilder lrBuilder) {
        if (isRouterHasAcls(lRouter)) {
            List<AclBindings> bindings = new ArrayList<>();
            for (Entry<String, UUID> entry : lRouter.getAclBindingColumn().getData().entrySet()) {
                AclBindingsBuilder aclBindingBuiler = new AclBindingsBuilder();
                UUID aclUUID = entry.getValue();
                ACL acl = (ACL)getOvsdbConnectionInstance().getDeviceInfo().getDeviceOperData(Acls.class, aclUUID);
                if (acl != null) {
                    InstanceIdentifier<Acls> aclIid =
                            HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), acl);
                    aclBindingBuiler.setAclRef(new HwvtepAclRef(aclIid));
                    aclBindingBuiler.setRouterInterface(new IpPrefix(new Ipv4Prefix(entry.getKey())));
                    bindings.add(aclBindingBuiler.build());
                }
                lrBuilder.setAclBindings(bindings);
            }
        }
    }

    private boolean isRouterHasAcls(final LogicalRouter lRouter) {
        return lRouter != null && lRouter.getAclBindingColumn() != null && lRouter.getAclBindingColumn().getData() != null
                && !lRouter.getAclBindingColumn().getData().isEmpty();
    }

    private void setSwitchBindings(final LogicalRouter lRouter, final LogicalRoutersBuilder lrBuilder) {
        if (isRouterHasSwitchBindings(lRouter)) {
            List<SwitchBindings> bindings = new ArrayList<>();
            for (Entry<String, UUID> entry : lRouter.getSwitchBindingColumn().getData().entrySet()) {
                SwitchBindingsBuilder switchBindingBuiler = new SwitchBindingsBuilder();
                UUID lsUUID = entry.getValue();
                LogicalSwitch logicalSwitch = getOvsdbConnectionInstance().getDeviceInfo().getLogicalSwitch(lsUUID);
                if (logicalSwitch != null) {
                    switchBindingBuiler.setDestinationAddress(new IpPrefix(new Ipv4Prefix(entry.getKey())));
                    InstanceIdentifier<LogicalSwitches> lSwitchIid =
                            HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(), logicalSwitch);
                    switchBindingBuiler.setLogicalSwitchRef(new HwvtepLogicalSwitchRef(lSwitchIid));
                    bindings.add(switchBindingBuiler.build());
                }
            }
            lrBuilder.setSwitchBindings(bindings);
        }
    }

    private boolean isRouterHasSwitchBindings(final LogicalRouter lRouter) {
        return lRouter != null && lRouter.getSwitchBindingColumn() != null && lRouter.getSwitchBindingColumn().getData() != null
                && !lRouter.getSwitchBindingColumn().getData().isEmpty();
    }
}
