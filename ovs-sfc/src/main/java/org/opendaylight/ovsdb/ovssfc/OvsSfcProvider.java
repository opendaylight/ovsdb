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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsSfcProvider implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OvsSfcProvider.class);
    private DataBroker dataBroker;
    private SfcDataListener sfcDataListener;
    private SffDataListener sffDataListener;
    private SfpDataListener sfpDataListener;

    public OvsSfcProvider(DataBroker dataBroker) {
        setDataBroker(dataBroker);

        sfcDataListener = new SfcDataListener(dataBroker);
        sffDataListener = new SffDataListener(dataBroker);
        sfpDataListener = new SfpDataListener(dataBroker);

        logger.info("Initialized");
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void close() throws Exception {
        sfcDataListener.closeDataChangeListener();
        sffDataListener.closeDataChangeListener();
        sfpDataListener.closeDataChangeListener();
        logger.info("Closed");
    }
}
