/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */
package org.opendaylight.ovs.nx.sal.convertor.action;

import org.opendaylight.openflowplugin.extension.api.ConvertorActionFromOFJava;
import org.opendaylight.openflowplugin.extension.api.ConvertorActionToOFJava;
import org.opendaylight.openflowplugin.extension.api.path.ActionPath;
import org.opendaylight.openflowplugin.extension.vendor.nicira.convertor.CodecPreconditionException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev130731.actions.grouping.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.NxmNxMultipath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.OfjAugNxAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.OfjAugNxActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.ofj.nx.action.multipath.grouping.ActionMultipath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.ofj.nx.action.multipath.grouping.ActionMultipathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.NxActionMultipathGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.flows.statistics.update.flow.and.statistics.map.list.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionMultipathNotifFlowsStatisticsUpdateApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.flows.statistics.update.flow.and.statistics.map.list.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionMultipathNotifFlowsStatisticsUpdateWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.group.desc.stats.updated.group.desc.stats.buckets.bucket.action.action.NxActionMultipathNotifGroupDescStatsUpdatedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.multipath.grouping.NxMultipath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.multipath.grouping.NxMultipathBuilder;

import com.google.common.base.Preconditions;

public class MultipathConvertor implements
ConvertorActionToOFJava<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action, Action>,
ConvertorActionFromOFJava<Action, ActionPath> {

    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action convert(Action input, ActionPath path) {
        ActionMultipath action = input.getAugmentation(OfjAugNxAction.class).getActionMultipath();
        NxMultipathBuilder builder = new NxMultipathBuilder();
        builder.setFields(action.getFields());
        builder.setBasis(action.getBasis());
        builder.setAlgorithm(action.getAlgorithm());
        builder.setMaxLink(action.getMaxLink());
        builder.setArg(action.getArg());
        builder.setOfsNbits(action.getOfsNbits());
        return resolveAction(builder.build(), path);
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action resolveAction(NxMultipath value, ActionPath path) {
        switch (path) {
        case NODES_NODE_TABLE_FLOW_INSTRUCTIONS_INSTRUCTION_WRITEACTIONSCASE_WRITEACTIONS_ACTION_ACTION_EXTENSIONLIST_EXTENSION:
            return new NxActionMultipathNodesNodeTableFlowWriteActionsCaseBuilder().setNxMultipath(value).build();
        case FLOWSSTATISTICSUPDATE_FLOWANDSTATISTICSMAPLIST_INSTRUCTIONS_INSTRUCTION_INSTRUCTION_WRITEACTIONSCASE_WRITEACTIONS_ACTION_ACTION:
            return new NxActionMultipathNotifFlowsStatisticsUpdateWriteActionsCaseBuilder().setNxMultipath(value).build();
        case FLOWSSTATISTICSUPDATE_FLOWANDSTATISTICSMAPLIST_INSTRUCTIONS_INSTRUCTION_INSTRUCTION_APPLYACTIONSCASE_APPLYACTIONS_ACTION_ACTION:
            return new NxActionMultipathNotifFlowsStatisticsUpdateApplyActionsCaseBuilder().setNxMultipath(value).build();
        case GROUPDESCSTATSUPDATED_GROUPDESCSTATS_BUCKETS_BUCKET_ACTION:
            return new NxActionMultipathNotifGroupDescStatsUpdatedCaseBuilder().setNxMultipath(value).build();
        default:
            throw new CodecPreconditionException(path);
        }
    }

    @Override
    public Action convert(org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action nxActionArg) {
        Preconditions.checkArgument(nxActionArg instanceof NxActionMultipathGrouping);
        NxActionMultipathGrouping nxAction = (NxActionMultipathGrouping) nxActionArg;
        ActionMultipathBuilder builder = new ActionMultipathBuilder();

        builder.setFields(nxAction.getNxMultipath().getFields());
        builder.setBasis(nxAction.getNxMultipath().getBasis());
        builder.setAlgorithm(nxAction.getNxMultipath().getAlgorithm());
        builder.setMaxLink(nxAction.getNxMultipath().getMaxLink());
        builder.setArg(nxAction.getNxMultipath().getArg());
        builder.setOfsNbits(nxAction.getNxMultipath().getOfsNbits());

        OfjAugNxActionBuilder augNxActionBuilder = new OfjAugNxActionBuilder();
        augNxActionBuilder.setActionMultipath(builder.build());
        return ActionUtil.createNiciraAction(augNxActionBuilder.build(), NxmNxMultipath.class);
    }

}
