/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.plugin;

import java.util.Random;

public class OvsdbMessage {
    String methodName;
    Object[] argument;
    String id;

    public OvsdbMessage(String method, Object[] arg){
        this.methodName = method;
        this.argument = arg;
        Random x = new Random();
        this.id = Integer.toString(x.nextInt(10000));
    }
}
