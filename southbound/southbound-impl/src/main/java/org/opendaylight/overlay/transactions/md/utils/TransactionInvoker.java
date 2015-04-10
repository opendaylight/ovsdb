/*
* Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.overlay.transactions.md.utils;

public interface TransactionInvoker {

    public void invoke(TransactionCommand command);

}
