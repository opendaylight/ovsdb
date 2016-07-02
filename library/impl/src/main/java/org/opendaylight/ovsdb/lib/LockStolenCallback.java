/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib;

/**
 *  Callback that can be registered with {@link org.opendaylight.ovsdb.lib.OvsdbClient} to
 *  get notified of a lock stolen.
 *  @see <a href="http://tools.ietf.org/html/draft-pfaff-ovsdb-proto-04#section-4.1.10">ovsdb spec</a>
 *
 *  @see org.opendaylight.ovsdb.lib.OvsdbClient#lock(String, LockAquisitionCallback, LockStolenCallback)
 */
public interface LockStolenCallback {

    void lockStolen();

}
