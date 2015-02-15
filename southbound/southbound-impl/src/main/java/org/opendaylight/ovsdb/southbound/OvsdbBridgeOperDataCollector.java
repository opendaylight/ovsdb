/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.southbound.OvsdbSchemaContants.OVSDBSCHEMATABLES;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbManagedNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeRef;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
*
* @author Anil Vishnoi (avishnoi@brocade.com)
*
*/

public class OvsdbBridgeOperDataCollector extends OvsdbDataCollectionOperation {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbBridgeOperDataCollector.class);

	public OvsdbBridgeOperDataCollector(final OperationType operType,
			final OvsdbClient ovsdbClient, final DataBroker db) {
		super(operType, ovsdbClient, db);
	}

	@Override
	public void fetchAndStoreOperData(final OvsdbClient ovsdbClient, final DataBroker db) {
		final ReadWriteTransaction rwTransaction = db.newReadWriteTransaction();
        DatabaseSchema dbSchema = ovsdbClient.getDatabaseSchema(OvsdbSchemaContants.databaseName);
        TransactionBuilder transactionBuilder = ovsdbClient.transactBuilder(dbSchema);
        TableSchema<GenericTableSchema> childTableSchema = dbSchema.table(OVSDBSCHEMATABLES.BRIDGE.getTableName(), GenericTableSchema.class);
        transactionBuilder.add(Operations.op.select(childTableSchema)).build();
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();

        Futures.addCallback(results, new FutureCallback<List<OperationResult>>(){

			@Override
			public void onFailure(Throwable arg0) {
				LOG.error("Read operation failure while reading {} database from ovsdb node "
						+ "running on  {}",OVSDBSCHEMATABLES.BRIDGE.getTableName(),ovsdbClient.getConnectionInfo().toString());

			}

			@Override
			public void onSuccess(List<OperationResult> arg0) {
				LOG.info("Successfully read operational data from {} database of ovsdb node "
						+ "running on {}",OVSDBSCHEMATABLES.BRIDGE.getTableName(),ovsdbClient.getConnectionInfo().toString());

				for(OperationResult result : arg0){
					if(result.getError() != null){
						LOG.error("Error occured while fetching bridge operational data from ovsdb database : {}",result.getDetails());
					}else{
						for(Row<GenericTableSchema> row : result.getRows()){
							Bridge bridge = ovsdbClient.getTypedRowWrapper(Bridge.class, row);
							final InstanceIdentifier<Node> nodeIdent = SouthboundMapper.createInstanceIdentifier(ovsdbClient);
							Optional<Node> node = Optional.absent();
					        try{
					        	node = rwTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
					        }catch (final ReadFailedException e) {
		                        LOG.info("Read Operational/DS for Node fail! {}", nodeIdent, e);
		                    }
					        if(node.isPresent()){
					        	LOG.info("Node {} is present",node);
					            NodeBuilder nodeBuilder = new NodeBuilder();
					            nodeBuilder.setNodeId(SouthboundMapper.createNodeId(ovsdbClient.getConnectionInfo()));

					        	OvsdbManagedNodeAugmentationBuilder ovsdbManagedNodeBuilder = new OvsdbManagedNodeAugmentationBuilder();
					        	ovsdbManagedNodeBuilder.setBridgeName(bridge.getName());
					        	ovsdbManagedNodeBuilder.setBridgeUuid(new Uuid(bridge.getUuid().toString()));
					        	ovsdbManagedNodeBuilder.setManagedBy(new OvsdbNodeRef(nodeIdent));
					            nodeBuilder.addAugmentation(OvsdbManagedNodeAugmentation.class, ovsdbManagedNodeBuilder.build());

					            LOG.info("Created managed node augmentation data {}",ovsdbManagedNodeBuilder.toString());
					            rwTransaction.put(LogicalDatastoreType.OPERATIONAL, nodeIdent, nodeBuilder.build());
					            Futures.addCallback(rwTransaction.submit(),new FutureCallback<Void>(){

									@Override
									public void onFailure(Throwable arg0) {
										LOG.info("Write to Operational Data Store for managed node {} failed with exception {}!",nodeIdent,arg0 );
									}

									@Override
									public void onSuccess(Void arg0) {
										LOG.info("Managed node operational data stored successfully to md-sal operational data store.");
									}

					            });
					        }

						}
					}
				}
			}

        });

	}

	@Override
	public void fetchAndUpdateOperData(final OvsdbClient ovsdbClient, final DataBroker db) {
		final ReadWriteTransaction rwTransaction = db.newReadWriteTransaction();
        DatabaseSchema dbSchema = ovsdbClient.getDatabaseSchema(OvsdbSchemaContants.databaseName);
        TransactionBuilder transactionBuilder = ovsdbClient.transactBuilder(dbSchema);
        TableSchema<GenericTableSchema> childTableSchema = dbSchema.table(OVSDBSCHEMATABLES.BRIDGE.getTableName(), GenericTableSchema.class);
        transactionBuilder.add(Operations.op.select(childTableSchema)).build();
        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();

        Futures.addCallback(results, new FutureCallback<List<OperationResult>>(){

			@Override
			public void onFailure(Throwable arg0) {
				LOG.error("Read operation failure while reading {} database from ovsdb node "
						+ "running on  {}",OVSDBSCHEMATABLES.BRIDGE.getTableName(),ovsdbClient.getConnectionInfo().toString());

			}

			@Override
			public void onSuccess(List<OperationResult> arg0) {
				LOG.info("Successfully read operational data from {} database of ovsdb node "
						+ "running on {}",OVSDBSCHEMATABLES.BRIDGE.getTableName(),ovsdbClient.getConnectionInfo().toString());

				for(OperationResult result : arg0){
					if(result.getError() != null){
						LOG.error("Error occured while fetching bridge operational data from ovsdb database : {}",result.getDetails());
					}else{
						for(Row<GenericTableSchema> row : result.getRows()){
							Bridge bridge = ovsdbClient.getTypedRowWrapper(Bridge.class, row);
							final InstanceIdentifier<Node> nodeIdent = SouthboundMapper.createInstanceIdentifier(ovsdbClient);
							Optional<Node> node = Optional.absent();
					        try{
					        	node = rwTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeIdent).checkedGet();
					        }catch (final ReadFailedException e) {
		                        LOG.info("Read Operational/DS for Node fail! {}", nodeIdent, e);
		                    }
					        if(node.isPresent()){
					        	LOG.info("Node {} is present",node);
					            NodeBuilder nodeBuilder = new NodeBuilder();
					            nodeBuilder.setNodeId(SouthboundMapper.createNodeId(ovsdbClient.getConnectionInfo()));

					        	OvsdbManagedNodeAugmentationBuilder ovsdbManagedNodeBuilder = new OvsdbManagedNodeAugmentationBuilder();
					        	ovsdbManagedNodeBuilder.setBridgeName(bridge.getName());
					        	ovsdbManagedNodeBuilder.setBridgeUuid(new Uuid(bridge.getUuid().toString()));
					        	ovsdbManagedNodeBuilder.setManagedBy(new OvsdbNodeRef(nodeIdent));
					            nodeBuilder.addAugmentation(OvsdbManagedNodeAugmentation.class, ovsdbManagedNodeBuilder.build());

					            LOG.info("Created managed node augmentation data {}",ovsdbManagedNodeBuilder.toString());
					            rwTransaction.merge(LogicalDatastoreType.OPERATIONAL, nodeIdent, nodeBuilder.build());
					            Futures.addCallback(rwTransaction.submit(),new FutureCallback<Void>(){

									@Override
									public void onFailure(Throwable arg0) {
										LOG.info("Write to Operational Data Store for managed node {} failed with exception {}!",nodeIdent,arg0 );
									}

									@Override
									public void onSuccess(Void arg0) {
										LOG.info("Managed node operational data stored successfully to md-sal operational data store.");
									}

					            });
					        }

						}
					}
				}
			}

        });

	}

	@Override
	public void deleteOperData(OvsdbClient ovsdbClient, DataBroker db) {
		/* TODO Operational data of managed node need to be deleted in following two scenario
		 * 1) When node is disconnected
		 * 2) When any specific managed node will be deleted.
		 * In case of 1), all the managed node data will get deleted with node data.
		 * In case of 2), whenever user delete any bridge, ovsdb south bound plugin
		 * should get the notification and it should update the data using fetchAndUpdateOperData
		 */

	}

}
