/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovs.nx.ofjava.codec.action;
public class NiciraActionCodecs {
    public static final ResubmitCodec RESUBMIT_CODEC = new ResubmitCodec();
    public static final SetNspCodec SET_NSP_CODEC = new SetNspCodec();
    public static final SetNsiCodec SET_NSI_CODEC = new SetNsiCodec();
    public static final MultipathCodec MULTIPATH_CODEC = new MultipathCodec();
}
