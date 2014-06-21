/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.ovsdb.lib.error;

/**
 * Created by dave on 22/06/2014.
 */
public class BadSchemaException extends RuntimeException {
    public BadSchemaException(String message){
        super(message);
    }

    public BadSchemaException(String message, Throwable cause){
        super(message, cause);
    }

}
