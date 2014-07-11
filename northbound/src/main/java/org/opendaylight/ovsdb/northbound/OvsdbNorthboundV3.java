/*
 * Copyright (C) 2014 Red Hat, Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.northbound;

import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* OVSDB Northbound V3 REST API.<br>
*/

@Path("/v3/")
public class OvsdbNorthboundV3 {
    protected static final Logger logger = LoggerFactory.getLogger(OvsdbNorthboundV3.class);
}
