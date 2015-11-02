/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import java.util.List;

import org.opendaylight.ovsdb.lib.operations.OperationResult;

public class TransactResponse extends Response {
    List<OperationResult> result;

    public List<OperationResult> getResult() {
        return result;
    }

    public void setResult(List<OperationResult> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "TransactResponse [result=" + result + "]";
    }
}
