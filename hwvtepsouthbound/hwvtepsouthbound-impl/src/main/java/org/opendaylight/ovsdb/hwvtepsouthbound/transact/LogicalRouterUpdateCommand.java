/*
 * Copyright © 2017 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalRouter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Acls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalRouters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.AclBindings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.logical.router.attributes.SwitchBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogicalRouterUpdateCommand extends AbstractTransactCommand<LogicalRouters, HwvtepGlobalAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(LogicalRouterUpdateCommand.class);

    public LogicalRouterUpdateCommand(HwvtepOperationalState state,
            Collection<DataTreeModification<Node>> changes) {
        super(state, changes);
    }

    @Override
    public void execute(TransactionBuilder transaction) {
        Map<InstanceIdentifier<Node>, List<LogicalRouters>> updateMap =
                extractUpdated(getChanges(),LogicalRouters.class);

        for (Entry<InstanceIdentifier<Node>, List<LogicalRouters>> updated:
            updateMap.entrySet()) {
            updateLogicalRouter(transaction,  updated.getKey(), updated.getValue());
        }
    }

    private void updateLogicalRouter(TransactionBuilder transaction, final InstanceIdentifier<Node> instanceIdentifier,
            final List<LogicalRouters> routerList) {
        for (LogicalRouters lrouter: routerList) {
            InstanceIdentifier<LogicalRouters> routerKey = instanceIdentifier
                    .augmentation(HwvtepGlobalAugmentation.class).child(LogicalRouters.class, lrouter.getKey());
            LOG.debug("Creating logical router named: {}", lrouter.getHwvtepNodeName());

            final Optional<LogicalRouters> operationalRouterOptional =
                    getOperationalState().getLogicalRouters(instanceIdentifier, lrouter.getKey());
            LogicalRouter logicalRouter = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                    LogicalRouter.class);
            setDescription(logicalRouter, lrouter);

            setSwitchBindings(transaction, logicalRouter, lrouter.getSwitchBindings());
            setStaticRoutes(logicalRouter, lrouter.getStaticRoutes());
            setAclBindings(logicalRouter, lrouter.getAclBindings());

            if (!operationalRouterOptional.isPresent()) {
                setName(logicalRouter, lrouter, operationalRouterOptional);
                LOG.trace("Creating LogicalRouter entry: {}", logicalRouter);
                transaction.add(op.insert(logicalRouter).withId(TransactUtils.getLogicalRouterId(lrouter)));
                transaction.add(op.comment("Logical Router: Creating " + lrouter.getHwvtepNodeName().getValue()));
                UUID lrUuid = new UUID(TransactUtils.getLogicalRouterId(lrouter));
                updateCurrentTxData(LogicalRouters.class, routerKey, lrUuid, lrouter);
            } else {
                LogicalRouters updatedLRouter = operationalRouterOptional.get();
                String existingLogicalRouterName = updatedLRouter.getHwvtepNodeName().getValue();
                LogicalRouter extraLogicalRouter = TyperUtils.getTypedRowWrapper(transaction.getDatabaseSchema(),
                        LogicalRouter.class);
                extraLogicalRouter.setName("");
                LOG.trace("Updating LogicalRouter entry: {}", logicalRouter);
                transaction.add(op.update(logicalRouter)
                        .where(extraLogicalRouter.getNameColumn().getSchema().opEqual(existingLogicalRouterName))
                        .build());
                transaction.add(op.comment("Logical Router: Updating " + existingLogicalRouterName));
            }
        }

    }

    private void setDescription(final LogicalRouter logicalRouter, final LogicalRouters inputRouter) {
        if (inputRouter.getHwvtepNodeDescription() != null) {
            logicalRouter.setDescription(inputRouter.getHwvtepNodeDescription());
        } else {
            LOG.warn("Logical router {} is missing a description string", inputRouter.getHwvtepNodeName());
        }
    }

    private void setName(final LogicalRouter logicalRouter, final LogicalRouters inputRouter,
            Optional<LogicalRouters> inputRouterOptional) {
        if (inputRouter.getHwvtepNodeName() != null) {
            logicalRouter.setName(inputRouter.getHwvtepNodeName().getValue());
        } else if (inputRouterOptional.isPresent() && inputRouterOptional.get().getHwvtepNodeName() != null) {
            logicalRouter.setName(inputRouterOptional.get().getHwvtepNodeName().getValue());
        }
    }

    private void setSwitchBindings(final TransactionBuilder transaction, final LogicalRouter logicalRouter,
            final List<SwitchBindings> switchBindings) {
        if (switchBindings != null) {
            Map<String, UUID> bindingMap = new HashMap<>();
            for (SwitchBindings switchBinding : switchBindings) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<LogicalSwitches> lswitchIid =
                        (InstanceIdentifier<LogicalSwitches>)switchBinding.getLogicalSwitchRef().getValue();
                Optional<LogicalSwitches> operationalSwitchOptional =
                        getOperationalState().getLogicalSwitches(lswitchIid);
                if (operationalSwitchOptional.isPresent()) {
                    Uuid logicalSwitchUuid = operationalSwitchOptional.get().getLogicalSwitchUuid();
                    bindingMap.put(switchBinding.getDestinationAddress().getIpv4Prefix().getValue(),
                            new UUID(logicalSwitchUuid.getValue()));
                } else {
                    bindingMap.put(switchBinding.getDestinationAddress().getIpv4Prefix().getValue(),
                            TransactUtils.getLogicalSwitchUUID(transaction, getOperationalState(), lswitchIid));
                }
            }
            logicalRouter.setSwitchBinding(bindingMap);
        }
    }

    private void setAclBindings(final LogicalRouter logicalRouter, final List<AclBindings> aclBindings) {
        if (aclBindings != null) {
            Map<String, UUID> bindingMap = new HashMap<>();
            for (AclBindings aclBinding : aclBindings) {
                @SuppressWarnings("unchecked")
                InstanceIdentifier<Acls> aclIid =
                        (InstanceIdentifier<Acls>)aclBinding.getAclRef().getValue();
                Optional<Acls> operationalAclOptional =
                        getOperationalState().getAcls(aclIid);
                if (operationalAclOptional.isPresent()) {
                    Uuid aclUuid = operationalAclOptional.get().getAclUuid();
                    bindingMap.put(String.valueOf(aclBinding.getRouterInterface().getValue()),
                            new UUID(aclUuid.getValue()));
                } else {
                    bindingMap.put(String.valueOf(aclBinding.getRouterInterface().getValue()),
                            TransactUtils.getAclUUID(aclIid));
                }
            }
            logicalRouter.setAclBinding(bindingMap);
        }

    }

    private void setStaticRoutes(final LogicalRouter logicalRouter, final List<StaticRoutes> staticRoutes) {
        if (staticRoutes != null) {
            Map<String, String> staticRoutesMap = new HashMap<>();
            for (StaticRoutes staticRoute : staticRoutes) {
                staticRoutesMap.put(String.valueOf(staticRoute.getDestinationAddress().getValue()),
                        String.valueOf(staticRoute.getNexthopAddress().getValue()));
            }
            logicalRouter.setStaticRoutes(staticRoutesMap);
        }
    }

    @Override
    protected List<LogicalRouters> getData(final HwvtepGlobalAugmentation augmentation) {
        return augmentation.getLogicalRouters();
    }
}

