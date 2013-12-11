/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import org.opendaylight.ovsdb.lib.notation.json.Converter.UpdateNotificationConverter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(converter = UpdateNotificationConverter.class)
public class UpdateNotification {
    Object context;
    TableUpdates update;
    public Object getContext() {
        return context;
    }
    public void setContext(Object context) {
        this.context = context;
    }
    public TableUpdates getUpdate() {
        return update;
    }
    public void setUpdate(TableUpdates update) {
        this.update = update;
    }
}
