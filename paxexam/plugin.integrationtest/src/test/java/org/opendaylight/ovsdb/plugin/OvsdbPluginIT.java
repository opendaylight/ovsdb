/*
 * Copyright (c) 2014 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class OvsdbPluginIT extends OvsdbTestBase {
    private Logger log = LoggerFactory.getLogger(OvsdbPluginIT.class);
    @Inject
    private BundleContext bc;
    private OVSDBConfigService ovsdbConfigService = null;
    private Node node = null;


    private String stateToString(int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "Not CONVERTED";
        }
    }

    @Before
    public void areWeReady() throws InterruptedException {
        assertNotNull(bc);
        boolean debugit = false;
        Bundle b[] = bc.getBundles();
        for (Bundle element : b) {
            int state = element.getState();
            if (state != Bundle.ACTIVE && state != Bundle.RESOLVED) {
                log.info("Bundle:" + element.getSymbolicName() + " state:"
                          + stateToString(state));
                debugit = true;
            }
        }
        if (debugit) {
            log.debug("Do some debugging because some bundle is unresolved");
        }

        // Assert if true, if false we are good to go!
        assertFalse(debugit);
        try {
            node = getTestConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.ovsdbConfigService = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
    }

    @Test
    public void tableTest() throws Exception {
        assertNotNull("Invalid Node. Check connection params", node);
        Thread.sleep(3000); // Wait for a few seconds to get the Schema exchange done
        List<String> tables = ovsdbConfigService.getTables(node);
        System.out.println("Tables = "+tables);
        assertNotNull(tables);
    }
}
