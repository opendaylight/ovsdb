/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.integrationtest.northbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.ovsdb.integrationtest.ConfigurationBundles;
import org.opendaylight.ovsdb.integrationtest.OvsdbIntegrationTestBase;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExamParameterized;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.propagateSystemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

@RunWith(PaxExamParameterized.class)
@ExamReactorStrategy(PerClass.class)
public class OvsdbNorthboundIT extends OvsdbIntegrationTestBase {

    private Logger log = LoggerFactory.getLogger(OvsdbNorthboundIT.class);
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "admin";
    public static final String BASE_URI = "http://localhost:8888";
    public static final String MEDIA_TYPE_JSON = "application/json";
    public static final String NODE_ID_REPLACEMENT_PATTERN = "${node}";
    public static final String UUID_REPLACEMENT_PATTERN = "${uuid}";
    public static final String BRIDGE_UUID_REPLACEMENT_PATTERN = "${bridge_uuid}";
    public static final String PORT_UUID_REPLACEMENT_PATTERN = "${port_uuid}";
    public static final String QOS_UUID_REPLACEMENT_PATTERN = "${qos_uuid}";
    public static final String OVS_UUID_REPLACEMENT_PATTERN = "${ovs_uuid}";

    @Inject
    private BundleContext bc;
    private Node node = null;
    private IUserManager userManager;

    @Parameterized.Parameters(name = "ApiTest{index}:{0}")
    public static List<Object[]> getData() throws FileNotFoundException {
        ClassLoader classloader = OvsdbNorthboundIT.class.getClassLoader();
        InputStream input = classloader.getResourceAsStream("northbound.yaml");
        Yaml yaml = new Yaml();
        List<Map<String, Object>> object = (List<Map<String, Object>>) yaml.load(input);
        List<Object[]> parameters = Lists.newArrayList();

        for (Map<String, Object> o : object){
            Object[] l = o.values().toArray();
            parameters.add(l);
        }

        return parameters;

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

    @Test
    public void testApi() {
        System.out.println("Running " + fTestCase + "...\n");
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(USERNAME , PASSWORD));
        String uri = BASE_URI + fPath;
        WebResource webResource = client.resource(expand(uri));
        ClientResponse response = null;

        switch (fOperation) {
            case "GET":
                response = webResource.accept(MEDIA_TYPE_JSON)
                        .get(ClientResponse.class);
                break;
            case "POST":
                response = webResource.accept(MEDIA_TYPE_JSON)
                        .header("Content-Type", MEDIA_TYPE_JSON)
                        .post(ClientResponse.class, expand(fJson));
                UuidHelper.setUuid(response.getEntity(String.class));
                saveUuid(fPath);
                break;
            case "PUT":
                response = webResource.accept(MEDIA_TYPE_JSON)
                        .header("Content-Type", MEDIA_TYPE_JSON)
                        .put(ClientResponse.class, fJson);
                break;
            case "DELETE":
                response = webResource.delete(ClientResponse.class);
                UuidHelper.setUuid("");
                break;
            default:
                fail("Unsupported operation");
        }
        assertEquals(fExpectedStatusCode, response.getStatus());

    }

    private String expand(String content){
        if (content.contains(NODE_ID_REPLACEMENT_PATTERN)) {
            content = content.replace(NODE_ID_REPLACEMENT_PATTERN,
                    node.getNodeIDString());
        }
        if (content.contains(UUID_REPLACEMENT_PATTERN)) {
            content = content.replace(UUID_REPLACEMENT_PATTERN,
                    UuidHelper.getUuid());
        }
        if (content.contains(BRIDGE_UUID_REPLACEMENT_PATTERN)) {
            content = content.replace(BRIDGE_UUID_REPLACEMENT_PATTERN,
                    UuidHelper.getBridgeUuid());
        }
        if (content.contains(PORT_UUID_REPLACEMENT_PATTERN)) {
            content = content.replace(PORT_UUID_REPLACEMENT_PATTERN,
                    UuidHelper.getPortUuid());
        }
        if (content.contains(QOS_UUID_REPLACEMENT_PATTERN)) {
            content = content.replace(QOS_UUID_REPLACEMENT_PATTERN,
                    UuidHelper.getQosUuid());
        }
        if (content.contains(OVS_UUID_REPLACEMENT_PATTERN)) {
            content = content.replace(OVS_UUID_REPLACEMENT_PATTERN,
                    getOvsUuid());
        }
        return content;
    }

    private void saveUuid(String path){
        if (path.contains("bridge")) {
            UuidHelper.setBridgeUuid(UuidHelper.getUuid());
        }
        if (path.contains("port")) {
            UuidHelper.setPortUuid(UuidHelper.getUuid());
        }
        if (path.contains("qos")) {
            UuidHelper.setQosUuid(UuidHelper.getUuid());
        }
    }

    private String getOvsUuid() {
        if (UuidHelper.getOvsUuid() == null) {
            Client client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(USERNAME, PASSWORD));
            String uri = OvsdbNorthboundIT.BASE_URI + "/ovsdb/nb/v2/node/OVS/${node}/tables/open_vswitch/rows";
            WebResource webResource = client.resource(expand(uri));
            ClientResponse response = webResource.accept(MEDIA_TYPE_JSON)
                    .get(ClientResponse.class);

            String row = response.getEntity(String.class);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rowsNode = objectMapper.readTree(row).get("rows");
                // The first fieldName is the UUID
                String uuid = rowsNode.fieldNames().next();
                UuidHelper.setOvsUuid(uuid);
            } catch (IOException e) {
                fail("Cannot get the UUID for the Open_vSwitch table");
            }
        }

        return UuidHelper.getOvsUuid();
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

        ServiceReference r = bc.getServiceReference(IUserManager.class.getName());
        if (r != null) {
            this.userManager = (IUserManager) bc.getService(r);
        }
        // If UserManager is null, cannot login to run tests.
        assertTrue(this.userManager != null);

        try {
            node = getTestConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Wait before making a REST call to avoid overloading Tomcat
        Thread.sleep(500);

    }

    @Configuration
    public static Option[] configuration() {
        return options(
                //
                systemProperty("logback.configurationFile").value(
                        PathUtils.getBaseDir() + "/src/test/resources/logback.xml"
                ),

                systemProperty("org.eclipse.gemini.web.tomcat.config.path").value(
                        PathUtils.getBaseDir() + "/src/test/resources/tomcat-server.xml"),

                // To start OSGi console for inspection remotely
                systemProperty("osgi.console").value("2401"),

                propagateSystemProperty("ovsdbserver.ipaddress"),
                propagateSystemProperty("ovsdbserver.port"),

                ConfigurationBundles.controllerBundles(),
                ConfigurationBundles.controllerNorthboundBundles(),
                ConfigurationBundles.ovsdbLibraryBundles(),
                mavenBundle("org.opendaylight.ovsdb", "ovsdb.northbound").versionAsInProject(),
                junitBundles()
        );
    }
}

