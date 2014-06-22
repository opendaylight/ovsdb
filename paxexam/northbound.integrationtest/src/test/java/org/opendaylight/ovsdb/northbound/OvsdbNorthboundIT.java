/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.northbound;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.usermanager.IUserManager;
import org.ops4j.pax.exam.junit.PaxExamParameterized;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(PaxExamParameterized.class)
public class OvsdbNorthboundIT extends OvsdbTestBase {

    private Logger log = LoggerFactory.getLogger(OvsdbNorthboundIT.class);

    @Inject
    private BundleContext bc;
    private Node node = null;
    private int a;
    private int b;
    private int sum;
    private IUserManager userManager;

    /*
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                {2, 3, 5},
                {6, 2, 8}
        });
    }



    public OvsdbNorthboundIT(int a, int b, int sum) {
        this.a = a;
        this.b = b;
        this.sum = sum;
    }

    @Test
    public void add() {
        assertEquals(sum, a + b);
    }

    @Test
    public void bad(){
        fail();
    }

*/
    @Parameterized.Parameters(name="{index}: {0}")
    public static List<Object[]> getData() {
        return Arrays.asList(new Object[][]{
                // TODO: Parse test data from YAML file
                {"testGetBridgeRows", "GET", "http://localhost:8080/ovsdb/nb/v2/node/OVS/${node}/tables/bridge/rows", "", 200 }
        });
    }

    private String fTestCase;
    private String fOperation;
    private String fPath;
    private String fJson;
    private int fExpectedStatusCode;

    public OvsdbNorthboundIT(String testCase, String operation, String path, String json, int expectedStatusCode){
        fTestCase = testCase;
        fOperation = operation;
        fPath = path;
        fJson = json;
        fExpectedStatusCode = expectedStatusCode;
    }

    private String expandPath(String path){
        String uri = path.replace("${node}", node.getNodeIDString());
        return uri;
    }

    @Test
    public void testApi(){
        Client client = Client.create();
        WebResource webResource = client.resource(expandPath(fPath));
        ClientResponse response = null;
        switch (fOperation) {
            case "GET":
                response = webResource.accept("application/json")
                        .get(ClientResponse.class);
                break;
            case "POST":
                response = webResource.accept("application/json")
                        .post(ClientResponse.class, fJson);
                break;
            case "PUT":
                response = webResource.accept("application/json")
                        .put(ClientResponse.class, fJson);
                break;
            case "DELETE":
                response = webResource.accept("application/json")
                        .put(ClientResponse.class, fJson);
                break;
            default:
                fail("Unsupported operation");
        }

        assertEquals(response.getStatus(), fExpectedStatusCode);
    }

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

        // Fail if true, if false we are good to go!
        assertFalse(debugit);

        // If UserManager is null, cannot login to run tests.
        assertNotNull(bc.getServiceReference(IUserManager.class.getName()));

        try {
            node = getTestConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
