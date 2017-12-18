/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.utils;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class TransactionLog extends ArrayList<TransactionElement> {

    private final int capacity;
    private final int watermark;

    public TransactionLog(int initialCapacity, int watermark) {
        super(initialCapacity);
        this.capacity = initialCapacity;
        this.watermark = watermark;
    }

    public void addToLog(TransactionType updateType, Object object) {
        add(new TransactionElement(updateType, object));
    }

    public void addToLog(TransactionType updateType, InstanceIdentifier iid, DataObject dataObject) {
        add(new TransactionElement(updateType, new MdsalObject(iid, dataObject)));
    }

    @Override
    public boolean add(TransactionElement element) {
        if (size() >= watermark) {
            removeRange(0, capacity - watermark);
        }
        return super.add(element);
    }

    public ArrayList<TransactionElement> getElements() {
        return new ArrayList<>(this);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}
