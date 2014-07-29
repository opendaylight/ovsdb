/*
 * (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.util.event;

/**
 * Facade for posting events for asynchronous dispatching.
 * 
 * @author Thomas Vachuska
 */
public interface EventDispatcher {

    /**
     * Posts an opaque event for asynchronous dispatching.
     * 
     * @param event event data
     */
    void post(Event event);

}
