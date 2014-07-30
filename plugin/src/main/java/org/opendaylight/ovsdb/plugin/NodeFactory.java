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

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.utils.INodeFactory;
import org.opendaylight.controller.sal.core.Node;

public class NodeFactory implements INodeFactory {

      public Node fromString(String nodeType, String nodeId){
          if(nodeType.equals("OVS"))
              try{
                  return new Node("OVS", nodeId);
              } catch(ConstructionException e)
              {
                  return null;
              }
          return null;
      }
}
