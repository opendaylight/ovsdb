/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IFlowMDSalClientBuilder implements IFlowMDSalClient {
    private static final Logger logger = LoggerFactory.getLogger(IFlowMDSalClientBuilder.class);

    public FlowBuilder buildMdSalFlow(OvsFlowMatchClient ovsFlowMatch,
            OvsIFlowInstructionClient ovsFlowInstruction,
            OvsFlowParams ovsFlowParams, FlowBuilder flowBuilder) {

        ovsFlowMatch.buildMDSalMatch(flowBuilder, ovsFlowMatch);
        ovsFlowInstruction.buildMDSalInstructions(flowBuilder, ovsFlowInstruction);
        return flowBuilder;
    }
}