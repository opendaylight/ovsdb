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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This class is just a test for the parameterized runner
 */
@RunWith(Parameterized.class)
public class TempNorthboundTest extends OvsdbTestBase {
    private String fTestCase;
    private String fOperation;
    private String fPath;
    private String fJson;
    private int fExpectedStatusCode;

    @Parameterized.Parameters(name="{0}")
    public static List<Object[]> getData() {
        return Arrays.asList(new Object[][]{
                // TODO: Parse test data from YAML file
                {"testGetBridgeRows", "GET", "http://localhost:8080/ovsdb/nb/v2/node/OVS/${node}/tables/bridge/rows", "", 200}
        });
    }


    public TempNorthboundTest(String testCase, String operation, String path, String json, int expectedStatusCode){
        fTestCase = testCase;
        fOperation = operation;
        fPath = path;
        fJson = json;
        fExpectedStatusCode = expectedStatusCode;
    }

    private String expandPath(String path){
        String uri = path.replace("${node}", "foo");
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
}
