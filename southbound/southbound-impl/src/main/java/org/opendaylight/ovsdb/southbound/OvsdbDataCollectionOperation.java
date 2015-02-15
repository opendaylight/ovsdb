/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound;

import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Main responsibility of the class is to define the interface that various
 * config operataion can extend to. It define common interface for data store
 * operations.
 *
 * @author Anil Vishnoi (avishnoi@brocade.com)
 *
 */
public abstract class OvsdbDataCollectionOperation implements Callable<Object>{
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataCollectionOperation.class);

	public enum OperationType {
		FETCH_OVSDB_OPER_DATA,
		REFERESH_OVSDB_OPER_DATA,
		DELETE_OVSDB_OPER_DATA
	}

	private OperationType operType = OperationType.FETCH_OVSDB_OPER_DATA;
	private OvsdbClient ovsdbClient = null;
    private DataBroker db;


	public OvsdbDataCollectionOperation(final OperationType operType, final OvsdbClient ovsdbClient, final DataBroker db){
		Preconditions.checkNotNull(ovsdbClient);
		this.ovsdbClient = ovsdbClient;
		this.operType = operType;
		this.db = db;
	}
	@Override
	public Object call(){
		if( operType == OperationType.FETCH_OVSDB_OPER_DATA ){
			LOG.debug("Fetch ovsdb operational data from {} and store it in the md-sal data store.",this.ovsdbClient);
			this.fetchAndStoreOperData(ovsdbClient, db);
		}else if( operType == OperationType.REFERESH_OVSDB_OPER_DATA ){
			LOG.debug("Fetch ovsdb operational data from {} and update the existing data in the md-sal data store.",this.ovsdbClient);
			this.fetchAndUpdateOperData(ovsdbClient, db);
		}else if( operType == OperationType.DELETE_OVSDB_OPER_DATA ){
			LOG.debug("Delete operational data fetched from {}",this.ovsdbClient);
			this.deleteOperData(ovsdbClient, db);
		}
		return operType;
	}

	public abstract void fetchAndStoreOperData(final OvsdbClient ovsdbClient, final DataBroker db);

	public abstract void fetchAndUpdateOperData(final OvsdbClient ovsdbClient, final DataBroker db);

	public abstract void deleteOperData( final OvsdbClient ovsdbClient, final DataBroker db);
}
