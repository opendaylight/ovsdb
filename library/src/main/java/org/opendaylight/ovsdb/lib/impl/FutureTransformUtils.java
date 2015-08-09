/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FutureTransformUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final ListenableFuture<List<OperationResult>> transformTransactResponse(
            ListenableFuture<List<JsonNode>> transactResponseFuture, final List<Operation> operations) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return Futures.transform(transactResponseFuture, new Function<List<JsonNode>, List<OperationResult>>() {
            @Override
            public List<OperationResult> apply(List<JsonNode> jsonNodes) {
                final List<OperationResult> operationResults = new ArrayList<>();
                for (int index = 0; index < jsonNodes.size(); index++) {
                    JsonNode jsonNode = jsonNodes.get(index);
                    OperationResult or;
                    if (jsonNode != null && jsonNode.size() > 0) {
                        /*
                         * As per RFC 7047, section 4.1.3 :
                         * "In general, "result" contains some number of successful results,
                         * possibly followed by an error, in turn followed by enough JSON null
                         * values to match the number of elements in "params".  There is one
                         * exception: if all of the operations succeed, but the results cannot
                         * be committed, then "result" will have one more element than "params",
                         * with the additional element being an <error>."
                         *
                         * Hence, it is possible for a transaction response to contain more
                         * json elements than the transaction operation request.
                         * Also handle that case by checking for i < operations.size().
                         */
                        if (index < operations.size()) {
                            Operation op = operations.get(index);
                            switch (op.getOp()) {
                                case "select":
                                    or = new OperationResult();
                                    or.setRows(op.getTableSchema().createRows(jsonNode));
                                    break;

                                default:
                                    or = objectMapper.convertValue(jsonNode, OperationResult.class);

                                    break;
                            }
                        } else {
                            or = objectMapper.convertValue(jsonNode, OperationResult.class);
                        }
                    } else {
                        or = new OperationResult();
                    }
                    operationResults.add(or);
                }

                return operationResults;
            }
        });
    }
}
