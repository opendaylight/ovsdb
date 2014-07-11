/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.schema.openvswitch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class SslTestCases extends OpenVswitchSchemaTestBase {
    Logger logger = LoggerFactory.getLogger(SslTestCases.class);

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        super.setUp();
    }

    @Test
    public void testCreateTypedSslTable() throws InterruptedException, ExecutionException, IllegalArgumentException {

        String sslUuidStr = "sslUuidName";
        String caCert = "PARC";
        String certificate = "01101110 01100101 01110010 01100100";
        String privateKey = "SSL_Table_Test_Secret";
        ImmutableMap<String, String> externalIds = ImmutableMap.of("roomba", "powered");

        SSL ssl = ovs.createTypedRowWrapper(SSL.class);
        ssl.setCaCert(caCert);
        ssl.setCertificate(certificate);
        ssl.setPrivateKey(privateKey);
        ssl.setExternalIds(externalIds);
        // Get the parent OVS table UUID in it's single row
        UUID openVSwitchRowUuid = this.getOpenVSwitchTableUuid(ovs, OpenVswitchSchemaSuiteIT.getTableCache());
        OpenVSwitch openVSwitch = ovs.getTypedRowWrapper(OpenVSwitch.class, null);
        // The transaction index for the SSL insert is used to store the SSL UUID
        int insertSslOperationIndex = 0;
        TransactionBuilder transactionBuilder = ovs.transactBuilder(OpenVswitchSchemaSuiteIT.dbSchema)
                .add(op.insert(ssl.getSchema())
                        .withId(sslUuidStr)
                        .value(ssl.getCertificateColumn())
                        .value(ssl.getPrivateKeyColumn())
                        .value(ssl.getCaCertColumn())
                        .value(ssl.getExternalIdsColumn()))
                .add(op.mutate(openVSwitch.getSchema())
                        .addMutation(openVSwitch.getSslColumn().getSchema(), Mutator.INSERT,
                                Sets.newHashSet(new UUID(sslUuidStr)))
                        .where(openVSwitch.getUuidColumn().getSchema().opEqual(openVSwitchRowUuid))
                        .build());

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        for (OperationResult result : operationResults) {
            Assert.assertNull(result.getError());
        }
        // Store the returned SSL row UUID to be used in the TearDown deletion transaction
        OpenVswitchSchemaSuiteIT.setTestSslUuid(operationResults.get(insertSslOperationIndex).getUuid());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        logger.info("Insert & Mutate operation results for SSL = {} ", operationResults);

    }

    @Override
    public void update(Object context, UpdateNotification upadateNotification) {

    }

    @Override
    public void locked(Object context, List<String> ids) {

    }

    @Override
    public void stolen(Object context, List<String> ids) {

    }
}
