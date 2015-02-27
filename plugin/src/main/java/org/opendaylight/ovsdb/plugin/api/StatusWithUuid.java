/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Hugo Trippaers
 */
package org.opendaylight.ovsdb.plugin.api;

import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;
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
