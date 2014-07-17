/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */

package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

public class SubnetHandler extends AbstractHandler implements INeutronSubnetAware {

    static final Logger logger = LoggerFactory.getLogger(SubnetHandler.class);

    @Override
    public int canCreateSubnet(NeutronSubnet subnet) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSubnetCreated(NeutronSubnet subnet) {
        logger.debug("Neutron Subnet Creation : {}", subnet.toString());
    }

    @Override
    public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSubnetUpdated(NeutronSubnet subnet) {
        // TODO Auto-generated method stub

    }

    @Override
    public int canDeleteSubnet(NeutronSubnet subnet) {
        // TODO Auto-generated method stub
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSubnetDeleted(NeutronSubnet subnet) {
        // TODO Auto-generated method stub

    }
}
