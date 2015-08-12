/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.operations;


public class Comment extends Operation {

    public static final String COMMENT = "comment";
    String comment;

    public Comment(String comment) {
        super(null, COMMENT);
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }
}
