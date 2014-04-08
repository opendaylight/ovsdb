/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib;

/**
 *  Callback that can be registered with {@link OvsDBClient} to
 *  get notified of a lock stolen.
 *  @see <a href="http://tools.ietf.org/html/draft-pfaff-ovsdb-proto-04#section-4.1.10">ovsdb spec</a>
 *  <p/>
 *
 *  @see OvsDBClient
 */
public interface LockAquisitionCallback {

    public void lockAcquired();

}
