/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.EchoServiceCallbackFilters;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;

public class TestOvsdbClient implements EchoServiceCallbackFilters {

	OvsdbClient client = null;
	List<String> dbs_list = null;

	/** URL of the OVS instance */
	String serverurl = "192.168.1.122";
	int serverport = 6640;

	@Before
	public void setUp() throws Exception {
		client = OvsdbConnectionService.getService().connect(
				InetAddress.getByName(serverurl), serverport);
		dbs_list = client.getDatabases().get(1000, TimeUnit.MILLISECONDS);
	}

	@After
	public void tearDown() throws Exception {
		client.disconnect();
	}

	@Test
	public void isHostActive() {
		assertTrue(client.isActive());
	}

	/**
	 * Confirm few connection info
	 */
	@Test
	public void isConnectionInfoCorrect() {
		assertEquals(serverport, client.getConnectionInfo().getRemotePort());
		assertEquals(serverurl, client.getConnectionInfo().getRemoteAddress()
				.getHostAddress());
		assertEquals("ACTIVE", client.getConnectionInfo().getType().name());
	}

	/**
	 * List databases
	 * 
	 * @throws Exception
	 */
	@Test
	public void getDatabases() throws Exception {
		if (!dbs_list.isEmpty()) {
			for (String db : dbs_list) {
				System.out.println("Database name: " + db);
			}
		} else {
			assertTrue(!dbs_list.isEmpty());
		}
	}

	/**
	 * Try to get the databases' tables
	 * 
	 * @throws Exception
	 */
	@Test
	public void getSchema() throws Exception {
		if (!dbs_list.isEmpty()) {
			for (String db : dbs_list) {
				System.out.println("Database: " + db + " Schema: "
						+ client.getSchema(db).get().getTables());
			}
		} else {
			assertTrue(!dbs_list.isEmpty());
		}
	}
}
