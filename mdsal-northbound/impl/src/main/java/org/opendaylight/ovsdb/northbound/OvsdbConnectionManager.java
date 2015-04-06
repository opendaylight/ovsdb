/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.northbound;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbConnectionManager implements OvsdbConnectionListener, AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionManager.class);

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void connected(final OvsdbClient externalClient) {
        // TODO Auto-generated method stub
    }

    @Override
    public void disconnected(OvsdbClient client) {
        // TODO Auto-generated method stub
    }

}
