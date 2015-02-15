/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class OvsdbOperationalDataCollectionManagerImpl implements
		OvsdbOperationalDataCollectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOperationalDataCollectionManagerImpl.class);

	private final ExecutorService statDataStoreOperationServ;

	public OvsdbOperationalDataCollectionManagerImpl(){
		/*
		 * Using single thread executor as of now. If going forward we figure out
		 * that there is no order dependency between ovsdb operational data, we
		 * can move to multi thread executor service.
		 */
		ThreadFactory threadFact;
		threadFact = new ThreadFactoryBuilder().setNameFormat("ovsdb-oper-data-collection-manager-%d").build();
		statDataStoreOperationServ = Executors.newSingleThreadExecutor(threadFact);
	}

	@Override
	public void enqueue(OvsdbDataCollectionOperation ovsdbDataStoreOper) {
		LOG.info("Enqueued task {} for execution",ovsdbDataStoreOper);
		statDataStoreOperationServ.submit(ovsdbDataStoreOper);
	}
}
