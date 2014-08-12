/*
* Copyright (C) 2014 Red Hat, Inc.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Sam Hague
*/
package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public class OvsSfcProvider implements AutoCloseable {
    private SffDataListener sffDataListener;
    private DataBroker dataBroker;

    public OvsSfcProvider(DataBroker dataBroker) {
        this.dataBroker = dataBroker;

        sffDataListener = new SffDataListener(dataBroker);
    }

    @Override
    public void close() throws Exception {

    }
}
