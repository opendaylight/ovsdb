/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.ha.state;

public enum HAState {
    NonHA,
    D1Connected,
    D1ReConnected,
    D2Connected,
    D2Reconnected,
    D1Disconnected,
    D2Disconnected;
}
