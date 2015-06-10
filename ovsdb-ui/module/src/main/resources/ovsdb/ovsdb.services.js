/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['app/ovsdb/ovsdb.module'],function(ovsdb) {
  'use strict';

  ovsdb.register.factory('TopologyNetworkRestangular', function(Restangular, ENV) {
    return Restangular.withConfig(function(RestangularConfig) {
      RestangularConfig.setBaseUrl(ENV.getBaseURL("AD_SAL"));
    });
  });

  ovsdb.register.factory('TopologyNetworkSvc', function(TopologyNetworkRestangular) {
    var svc = {
      base: function(name) {
        return TopologyNetworkRestangular.one('restconf', name).one('network-topology:network-topology');
      },
      data : null
    };

    svc.getCurrentData = function() {
      return svc.data;
    };

    svc.getTopologiesIds = function() {
      svc.data = svc.base('operational').getList();
      return svc.data;
    };

    svc.getConfigNode = function(topologyId, nodeId) {
      return svc.base('config').one('topology', topologyId).one('node', nodeId).get();
    };

    svc.addConfigNode = function(topologyId, nodeId, data) {
      return svc.base('config').one('topology', topologyId).one('node').put(nodeId, data);
    };

    svc.addConfigBridge = function(topologyId, nodeId, data) {
        return svc.base('config').one('topology', topologyId).put(nodeId, data);
    };

    svc.removeConfigNode = function(topologyId, nodeId) {
      return svc.base('config').one('topology', topologyId).one('node', nodeId).remove();
    };

    svc.addTerminationPointConfig = function(topologyId, nodeId, terminationId, data) {
      return svc.base('config').one('topology', topologyId).one('node', nodeId).one('termination-point').put(terminationId, data);
    };

    svc.getTerminationPointConfig = function(topologyId, nodeId, terminationId) {
      return svc.base('config').one('topology', topologyId).one('node', nodeId).one('termination-point', terminationId).get();
    };

    svc.removeTerminationPointConfig = function(topologyId, nodeId, terminationId) {
      return svc.base('config').one('topology', topologyId).one('node', nodeId).one('termination-point', terminationId).remove();
    };
    return svc;
  });


  ovsdb.register.factory('TopologyNetworkFactory', function() {

    var factory = {
        createOvsdbNodeObject: function(nodeId, nodePort, nodeRemoteIp) {
            return {
                "network-topology:node": [
                    {
                        "node-id": nodeId,
                        "connection-info": {
                            "ovsdb:remote-port": nodePort,
                            "ovsdb:remote-ip": nodeRemoteIp
                        }
                    }
                ]
            };
        },
        createConfigNode: function(nodeId, bridgeName,datapathId, protocolEntries, controllerEntries, managedBy) {
            var configNode = {
                "network-topology:node": [
                    {
                        "node-id": nodeId,
                        "ovsdb:bridge-name": bridgeName,
                        "ovsdb:datapath-id": datapathId,
                        "ovsdb:protocol-entry": [ ],
                        "ovsdb:controller-entry": [ ],
                        "ovsdb:managed-by": managedBy
                    }
                ]
            };

            for (var protocolEntry in protocolEntries) {
                configNode[0]['ovsdb:protocal-entry'].push({
                    "protocol": protocolEntry
                });
            }

            for (var controllerEntry in controllerEntries) {
                configNode[0]['ovsdb:controller-entry'].push({
                    "protocol" : controllerEntry
                });
            }

            return configNode;

        },
        createEndPoint: function(ovsdb_options, name, interface_type, tp_id, vlan_tag, trunks, vlan_mode) {
            var termination_point = {
                "network-topology:termination-point": [
                    {
                        "ovsdb:options": [ ],
                        "ovsdb:name": name,
                        "ovsdb:interface-type": interface_type,
                        "tp-id": tp_id,
                        "vlan-tag": vlan_tag,
                        "trunks": [ ],
                        "vlan-mode":vlan_mode
                    }
                ]
            };

            for (var ovsdb_option in ovsdb_options) {
                   termination_point[0]['ovsdb:options'].push({
                        "ovsdb:option": ovsdb_option.option,
                        "ovsdb:value" : ovsdb_option.value
                   });
            }

            for (var trunk in trunks) {
                termination_point[0]['trunks'].push({
                    "trunk":trunk
                });
            }

            return termination_point;
        }
    };

    return factory;

  });

});
