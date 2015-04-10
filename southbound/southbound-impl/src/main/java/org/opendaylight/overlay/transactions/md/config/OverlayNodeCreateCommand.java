/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay.transactions.md.config;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.overlay.transactions.md.utils.AbstractTransactionCommand;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayNodeCreateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OverlayNodeCreateCommand.class);

    public OverlayNodeCreateCommand(AsyncDataChangeEvent<InstanceIdentifier<?>,
            DataObject> changes) {
        super(changes);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        // Todo write config execute
    }
}