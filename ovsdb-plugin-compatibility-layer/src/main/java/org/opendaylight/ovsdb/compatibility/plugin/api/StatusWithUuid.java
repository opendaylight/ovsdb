/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.compatibility.plugin.api;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.UUID;

/**
 * Extends the Status class to allow functions to return a uuid
 */
public class StatusWithUuid extends Status {
    private static final long serialVersionUID = -5413085099514964003L;
    private UUID uuid;

    public StatusWithUuid(StatusCode errorCode) {
        super(errorCode);
    }

    public StatusWithUuid(StatusCode errorCode, String description) {
        super(errorCode, description);
    }

    public StatusWithUuid(StatusCode errorCode, long requestId) {
        super(errorCode, requestId);
    }

    public StatusWithUuid(StatusCode errorCode, UUID uuid) {
        super(errorCode);
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

}
