/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.annotations.Beta;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionHistory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Beta
public interface HwvtepSouthboundProviderInfo {

    @NonNull Map<InstanceIdentifier<Node>, HwvtepDeviceInfo> getAllConnectedInstances();

    @NonNull Map<InstanceIdentifier<Node>, TransactionHistory> getControllerTxHistory();

    @NonNull Map<InstanceIdentifier<Node>, TransactionHistory> getDeviceUpdateHistory();
}
