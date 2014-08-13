/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovs.nx.sal;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.openflowjava.nx.api.NiciraUtil;
import org.opendaylight.ovs.nx.sal.convertor.action.ResubmitConvertor;
import org.opendaylight.ovs.nx.sal.convertor.action.SetNsiConvertor;
import org.opendaylight.ovs.nx.sal.convertor.action.SetNspConvertor;
import org.opendaylight.ovs.nx.sal.convertor.match.NspConvertor;
import org.opendaylight.ovs.nx.ofjava.codec.action.ResubmitCodec;
import org.opendaylight.ovs.nx.ofjava.codec.action.SetNsiCodec;
import org.opendaylight.ovs.nx.ofjava.codec.action.SetNspCodec;
import org.opendaylight.ovs.nx.ofjava.codec.match.NspCodec;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowplugin.extension.api.ConverterExtensionKey;
import org.opendaylight.openflowplugin.extension.api.ConvertorActionToOFJava;
import org.opendaylight.openflowplugin.extension.api.ExtensionConverterRegistrator;
import org.opendaylight.openflowplugin.extension.api.TypeVersionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.flow.input.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitRpcAddFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.flow.input.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitRpcAddFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.group.input.buckets.bucket.action.action.NxActionResubmitRpcAddGroupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionResubmitNodesNodeGroupBucketsBucketActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionResubmitNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.remove.group.input.buckets.bucket.action.action.NxActionResubmitRpcRemoveGroupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.update.group.input.original.group.buckets.bucket.action.action.NxActionResubmitRpcUpdateGroupOriginalCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.update.group.input.updated.group.buckets.bucket.action.action.NxActionResubmitRpcUpdateGroupUpdatedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.flow.input.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspRpcAddFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.flow.input.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionSetNspRpcAddFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.group.input.buckets.bucket.action.action.NxActionSetNspRpcAddGroupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionSetNspNodesNodeGroupBucketsBucketActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionSetNspNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.remove.group.input.buckets.bucket.action.action.NxActionSetNspRpcRemoveGroupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.update.group.input.original.group.buckets.bucket.action.action.NxActionSetNspRpcUpdateGroupOriginalCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.update.group.input.updated.group.buckets.bucket.action.action.NxActionSetNspRpcUpdateGroupUpdatedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.flow.input.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiRpcAddFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.flow.input.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionSetNsiRpcAddFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.add.group.input.buckets.bucket.action.action.NxActionSetNsiRpcAddGroupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionSetNsiNodesNodeGroupBucketsBucketActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiNodesNodeTableFlowApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.write.actions._case.write.actions.action.action.NxActionSetNsiNodesNodeTableFlowWriteActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.remove.group.input.buckets.bucket.action.action.NxActionSetNsiRpcRemoveGroupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.update.group.input.original.group.buckets.bucket.action.action.NxActionSetNsiRpcUpdateGroupOriginalCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.update.group.input.updated.group.buckets.bucket.action.action.NxActionSetNsiRpcUpdateGroupUpdatedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxmNxNspKey;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class NiciraExtensionProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory
            .getLogger(NiciraExtensionProvider.class);

    private ExtensionConverterRegistrator extensionConverterRegistrator;
    private Set<ObjectRegistration<?>> registrations;

    private final static ResubmitConvertor RESUBMIT_CONVERTOR = new ResubmitConvertor();
    private final static SetNspConvertor SET_NSP_CONVERTOR = new SetNspConvertor();
    private final static SetNsiConvertor SET_NSI_CONVERTOR = new SetNsiConvertor();
    private final static NspConvertor NSP_CONVERTOR = new NspConvertor();

    @Override
    public void close() {
        for (AutoCloseable janitor : registrations) {
            try {
                janitor.close();
            } catch (Exception e) {
                LOG.warn("closing of extension converter failed", e);
            }
        }
        extensionConverterRegistrator = null;
    }

    public void setExtensionConverterRegistrator(
            ExtensionConverterRegistrator extensionConverterRegistrator) {
                this.extensionConverterRegistrator = extensionConverterRegistrator;
    }

    public void registerConverters() {
        Preconditions.checkNotNull(extensionConverterRegistrator);
        registrations = new HashSet<>();

        registerAction13(NxActionResubmitNodesNodeTableFlowApplyActionsCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitNodesNodeTableFlowWriteActionsCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitNodesNodeGroupBucketsBucketActionsCase.class, RESUBMIT_CONVERTOR);


        registerAction13(NxActionResubmitRpcAddFlowApplyActionsCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitRpcAddFlowWriteActionsCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitRpcAddGroupCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitRpcRemoveGroupCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitRpcUpdateGroupOriginalCase.class, RESUBMIT_CONVERTOR);
        registerAction13(NxActionResubmitRpcUpdateGroupUpdatedCase.class, RESUBMIT_CONVERTOR);

        registerAction13(NxActionSetNspNodesNodeTableFlowApplyActionsCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspNodesNodeTableFlowWriteActionsCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspNodesNodeGroupBucketsBucketActionsCase.class, SET_NSP_CONVERTOR);


        registerAction13(NxActionSetNspRpcAddFlowApplyActionsCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspRpcAddFlowWriteActionsCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspRpcAddGroupCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspRpcRemoveGroupCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspRpcUpdateGroupOriginalCase.class, SET_NSP_CONVERTOR);
        registerAction13(NxActionSetNspRpcUpdateGroupUpdatedCase.class, SET_NSP_CONVERTOR);

        registerAction13(NxActionSetNsiNodesNodeTableFlowApplyActionsCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiNodesNodeTableFlowWriteActionsCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiNodesNodeGroupBucketsBucketActionsCase.class, SET_NSI_CONVERTOR);


        registerAction13(NxActionSetNsiRpcAddFlowApplyActionsCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiRpcAddFlowWriteActionsCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiRpcAddGroupCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiRpcRemoveGroupCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiRpcUpdateGroupOriginalCase.class, SET_NSI_CONVERTOR);
        registerAction13(NxActionSetNsiRpcUpdateGroupUpdatedCase.class, SET_NSI_CONVERTOR);

        registrations.add(extensionConverterRegistrator.registerActionConvertor(NiciraUtil.createOfJavaKeyFrom(ResubmitCodec.SERIALIZER_KEY), RESUBMIT_CONVERTOR));
        registrations.add(extensionConverterRegistrator.registerActionConvertor(NiciraUtil.createOfJavaKeyFrom(SetNspCodec.SERIALIZER_KEY), SET_NSP_CONVERTOR));
        registrations.add(extensionConverterRegistrator.registerActionConvertor(NiciraUtil.createOfJavaKeyFrom(SetNsiCodec.SERIALIZER_KEY), SET_NSI_CONVERTOR));

        registrations.add(extensionConverterRegistrator.registerMatchConvertor(new ConverterExtensionKey<>(NxmNxNspKey.class, EncodeConstants.OF13_VERSION_ID), NSP_CONVERTOR));
        registrations.add(extensionConverterRegistrator.registerMatchConvertor(NspCodec.SERIALIZER_KEY, NSP_CONVERTOR));
    }

    private void registerAction13(
            Class<? extends Action> actionCaseType,
            ConvertorActionToOFJava<Action, org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev130731.actions.grouping.Action> actionConvertor) {
        TypeVersionKey<? extends Action> key = new TypeVersionKey<>(actionCaseType, EncodeConstants.OF13_VERSION_ID);
        registrations.add(extensionConverterRegistrator.registerActionConvertor(key, actionConvertor));
    }
}
