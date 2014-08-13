/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovs.nx.ofjava.codec.match;
public class NiciraMatchCodecs {
    public static final NspCodec NSP_CODEC = new NspCodec();
    public static final NsiCodec NSI_CODEC = new NsiCodec();
}
