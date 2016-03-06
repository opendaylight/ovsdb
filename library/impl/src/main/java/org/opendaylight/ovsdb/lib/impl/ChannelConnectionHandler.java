/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.opendaylight.ovsdb.lib.OvsdbClient;

public class ChannelConnectionHandler implements ChannelFutureListener {
    OvsdbClient client;
    public ChannelConnectionHandler(OvsdbClient client) {
        this.client = client;
    }
    @Override
    public void operationComplete(ChannelFuture arg0) throws Exception {
        OvsdbConnectionService.channelClosed(client);
    }
}
