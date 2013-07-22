package org.opendaylight.ovsdb.internal;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.INodeConnectorFactory;
import org.opendaylight.controller.sal.core.Node;

public class NodeConnectorFactory implements INodeConnectorFactory
    {
      void init() {
      }

      /**
       * Function called by the dependency manager when at least one dependency
       * become unsatisfied or when the component is shutting down because for
       * example bundle is being stopped.
       *
       */
      void destroy() {
      }

      /**
       * Function called by dependency manager after "init ()" is called and after
       * the services provided by the class are registered in the service registry
       *
       */
      void start() {
      }

      /**
       * Function called by the dependency manager before the services exported by
       * the component are unregistered, this will be followed by a "destroy ()"
       * calls
       *
       */
      void stop() {
      }

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
