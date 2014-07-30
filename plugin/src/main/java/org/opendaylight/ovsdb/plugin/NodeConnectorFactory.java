/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.opendaylight.controller.sal.core.Node;

public class NodeConnectorFactory implements INodeConnectorFactory {
    public NodeConnector fromStringNoNode(String typeStr, String IDStr,
              Node n){
          if(typeStr.equals("OVS")){
              try {
                  return new NodeConnector(typeStr, IDStr, n);
              } catch (Exception ex) {
                  return null;
              }
          }
          return null;
      }
}
