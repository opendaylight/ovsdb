/*
 * Copyright (c) 2018 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.infrautils.diagstatus.DiagStatusService;
import org.opendaylight.infrautils.diagstatus.ServiceDescriptor;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.diagstatus.ServiceStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbDiagStatusProvider implements ServiceStatusProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDiagStatusProvider.class);
    private static final String OVSDB_SERVICE_NAME = "OVSDB";

    private final DiagStatusService diagStatusService;
    private volatile ServiceDescriptor serviceDescriptor;

    public OvsdbDiagStatusProvider(final DiagStatusService diagStatusService) {
        this.diagStatusService = diagStatusService;
        diagStatusService.register(OVSDB_SERVICE_NAME);
    }


    public void reportStatus(final ServiceState serviceState, final String description) {
        LOG.debug("reporting status as {} for {}", serviceState, OVSDB_SERVICE_NAME);
        serviceDescriptor = new ServiceDescriptor(OVSDB_SERVICE_NAME, serviceState, description);
        diagStatusService.report(serviceDescriptor);
    }

    @Override
    public ServiceDescriptor getServiceDescriptor() {
        // TODO Check 6640 port status to report dynamic status
        return serviceDescriptor;
    }
}
