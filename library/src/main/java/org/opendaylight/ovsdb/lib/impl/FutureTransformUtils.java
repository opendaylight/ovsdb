/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Sudheendra Murthy
 */

package org.opendaylight.ovsdb.lib.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.operations.OperationResult;

import java.util.ArrayList;
import java.util.List;

public class FutureTransformUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public final static ListenableFuture<List<OperationResult>> transformTransactResponse
            (ListenableFuture<List<JsonNode>> transactResponseFuture, final List<Operation> operations) {
        return Futures.transform(transactResponseFuture, new Function<List<JsonNode>, List<OperationResult>>() {
            @Override
            public List<OperationResult> apply(List<JsonNode> jsonNodes) {
                final List<OperationResult> operationResults = new ArrayList<>();
                for (int i = 0; i < jsonNodes.size(); i++) {
                    JsonNode jsonNode = jsonNodes.get(i);
                    OperationResult or;
                    if (jsonNode.size() > 0) {
                        Operation op = operations.get(i);
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
                        or = new OperationResult();
                    }

                    operationResults.add(or);
                }

                return operationResults;
            }
        });
    }
}
