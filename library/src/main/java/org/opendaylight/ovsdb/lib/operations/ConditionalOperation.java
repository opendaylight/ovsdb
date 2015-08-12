/*
 * Copyright (c) 2014 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;

import org.opendaylight.ovsdb.lib.notation.Condition;

/**
 * Represents an Operation type that accepts a condition, for e.g Update, Select etc
 */
public interface ConditionalOperation {

    void addCondition(Condition condition);
}
