/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.ovssfc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsSfcProvider implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OvsSfcProvider.class);
    private static OvsSfcProvider ovsSfcProvider;
    private DataBroker dataBroker;
    private SfcDataListener sfcDataListener;
    private SffDataListener sffDataListener;
    private SfpDataListener sfpDataListener;
    protected SfpHandler sfp;
    protected EventHandler eventHandler;
    protected AclUtils aclUtils;
    protected OvsUtils ovsUtils;
    protected SfUtils sfUtils;
    protected SffUtils sffUtils;
    protected Flows flows;

    public OvsSfcProvider (DataBroker dataBroker) {
        ovsSfcProvider = this;
        setDataBroker(dataBroker);

        sfcDataListener = new SfcDataListener(dataBroker);
        sffDataListener = new SffDataListener(dataBroker);
        sfpDataListener = new SfpDataListener(dataBroker);
        aclUtils = new AclUtils();
        ovsUtils = new OvsUtils();
        sfUtils = new SfUtils();
        sffUtils = new SffUtils();
        flows = new Flows();
        sfp = new SfpHandler();
        eventHandler = new EventHandler();
        eventHandler.init();
        eventHandler.start();

        logger.info("Initialized");
    }

    public void setDataBroker (DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public DataBroker getDataBroker () {
        return this.dataBroker;
    }

    public static OvsSfcProvider getOvsSfcProvider () {
        return OvsSfcProvider.ovsSfcProvider;
    }

    @Override
    public void close () throws Exception {
        sfcDataListener.closeDataChangeListener();
        sffDataListener.closeDataChangeListener();
        sfpDataListener.closeDataChangeListener();
        logger.info("Closed");
    }
}
