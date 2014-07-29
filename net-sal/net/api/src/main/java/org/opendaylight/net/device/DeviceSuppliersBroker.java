/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.net.device;

import org.opendaylight.net.supplier.SuppliersBroker;

/**
 * Broker of infrastructure device suppliers.
 *
 * @author Steve Dean
 */
public interface DeviceSuppliersBroker
        extends SuppliersBroker<DeviceSupplier, DeviceSupplierService> {

}
