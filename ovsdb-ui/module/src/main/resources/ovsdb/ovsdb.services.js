/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/ovsdb/ovsdb.module', 'app/ovsdb/OvsCore', 'underscore', 'app/ovsdb/ovsdb.constant'], function(ovsdb, OvsCore, _) {
  'use strict';

  ovsdb.register.factory('OvsdbRestangular', ['Restangular', 'ENV', function(Restangular, ENV) {
    return Restangular.withConfig(function(RestangularConfig) {
      RestangularConfig.setBaseUrl(ENV.getBaseURL("MD_SAL"));
    });
  }]);

  var RestConfRestangular = function(OvsdbRestangular, parent) {
    this.svc = {},
      parent.base = function(type) {
        return OvsdbRestangular.one('restconf').one(type);
      };
  };

  var NorthBoundRestangular = function(OvsdbRestangular, parent) {
    this.svc = {},
      parent.base = function(type) {
        return OvsdbRestangular.one('controller', 'nb').one('v2', type);
      };
  };

  var TopologySvc = function(OvsdbRestangular, nodeIdentifier, ovsNodeKeys, bridgeNodeKeys, tpKeys, flowInfoKeys, linkIdentifier, $q, $http) {
    var parent = {};
    RestConfRestangular.call(this, OvsdbRestangular, parent);

    function parseOvsdbNode(node) {
      var inetMgr = '',
        inetNode = '',
        otherLocalIp = '',
        otherInfo = null,
        connectionInfo = null;

      connectionInfo = node[ovsNodeKeys.CONNECTION_INFO];
      otherInfo = node[ovsNodeKeys.OTHER_CONFIG];

      if (_.isArray(connectionInfo)) {
        inetMgr = connectionInfo[ovsNodeKeys.LOCAL_IP] + ':' + connectionInfo[ovsNodeKeys.LOCAL_PORT];
        inetNode = connectionInfo[ovsNodeKeys.REMOTE_IP] + ':' + connectionInfo[ovsNodeKeys.REMOTE_PORT];
      }

      if (!_.isArray(otherInfo) && _.isObject(otherInfo)) {
        _.each(otherInfo, function(value) {
          if (value[ovsNodeKeys.OTHER_CONFIG_KEY] === 'local_ip') {
            otherLocalIp = value[ovsNodeKeys.OTHER_CONFIG_VALUE];
          }
        });
      }

      return new OvsCore.OvsNode(node[ovsNodeKeys.NODE_ID], inetMgr, inetNode, otherLocalIp, node[ovsNodeKeys.OVS_VERSION]);
    }

    function parseBridgeNode(node) {
      var bridgeNode = null,
        controllerTarget = '',
        controllerConnected = false,
        tp = node[bridgeNodeKeys.TP],
        controllerEntries = node[bridgeNodeKeys.CONTROLLER_ENTRY];

      _.each(controllerEntries, function(value) {
        controllerTarget = value[bridgeNodeKeys.TARGET];
        controllerEntries = value[bridgeNodeKeys.IS_CONNECTED];
        return false; // break the anonymus function
      });

      bridgeNode = new OvsCore.BridgeNode(node[bridgeNodeKeys.NODE_ID], node[bridgeNodeKeys.DATA_PATH], node[bridgeNodeKeys.BRIDGE_NAME], controllerTarget, controllerConnected);

      _.each(tp, function(value) {
        var tp = parseBridgeTP(value);

        if (tp.ofPort == '65534' && (tp.name === 'br-ex' || tp.name === 'br-int')) {
          return;
        } else {
          bridgeNode.addTerminationPoint(tp);
        }

      });

      return bridgeNode;
    }

    function parseBridgeTP(tp) {
      var mac = '',
        ifaceId = '',
        extInfo = tp['ovsdb:port-external-ids'] || tp['ovsdb:interface-external-ids'];

      _.each(extInfo, function(ext) {
        if (ext[tpKeys.EXTERNAL_KEY_ID] === tpKeys.ATTACHED_MAC) {
          mac = ext[tpKeys.EXTERNAL_KEY_VALUE];
        }
        if (ext[tpKeys.EXTERNAL_KEY_ID] === tpKeys.IFACE_ID) {
          ifaceId = extInfo[tpKeys.EXTERNAL_KEY_VALUE] || '';
        }
      });

      return new OvsCore.TerminationPoint(tp[tpKeys.NAME], tp[tpKeys.OF_PORT], tp[tpKeys.INTERFACE_TYPE], mac, ifaceId);
    }

    this.svc.getTopologies = function(cb) {

      var invNodeDefer = parent.base('operational').one('opendaylight-inventory:nodes').getList();
      var netTopoDefer = parent.base('operational').one('network-topology:network-topology').getList();

      // be sure all data are loaded
      $q.all([invNodeDefer, netTopoDefer]).then(function(values) {
          var invNode = values[0],
            netTopo = values[1];

          if (!netTopo['network-topology'] || !netTopo['network-topology']['topology']) {
            throw new Error('Invalid json format while parsing network-topology');
          }

          if (!invNode['nodes'] || !invNode['nodes']['node']) {
            throw new Error('Invalid JSON format while parsing inventory-node');
          }

          var topologies = netTopo['network-topology']['topology'],
            nodes = invNode['nodes']['node'],
            topo = new OvsCore.Topology();

          _.each(topologies, function(topology, topo_index) {
            if (!topology.hasOwnProperty('topology-id')) {
              throw new Error('Invalide JSON format, no topology-id for the topology [' + topo_index + ']');
            }

            var index_hash = [],
              i = 0;

            (topology['node'] || []).forEach(function(node) {
              if (!node[nodeIdentifier.ID]) {
                throw new Error('Unexpected node : undefined ' + nodeIdentifier.ID + ' key');
              }

              if (node['ovsdb:bridge-name']) {
                //bridge Node
                topo.registerBridgeNode(parseBridgeNode(node))
                index_hash[node[nodeIdentifier.ID]] = i++;
              } else if (node['ovsdb:connection-info']) {
                // obsvdb Node
                topo.registerOvsdbNode(parseOvsdbNode(node));
              }
            });

            (topology['link'] || []).forEach(function(link) {

              var source = link[linkIdentifier.SRC],
                dest = link[linkIdentifier.DEST];

              topo.registerFlowLink(new OvsCore.Link(link[linkIdentifier.ID], source[nodeIdentifier.SRC_TP], dest[nodeIdentifier.DEST_TP], source[nodeIdentifier.SRC_NODE],
                dest[nodeIdentifier.DEST_NODE]));
            });

            topo.registerFlowLink(new OvsCore.Link('openflow:1:1', 'openflow:1:1', 'openflow:2:3', 'openflow:1', 'openflow:2', 'tunnel'));

          });

          topo.updateLink();

          _.each(nodes, function(node, index) {
            if (!node['id']) {
              return;
            }

            var bridgeId = node['id'];

            var bridgeNode = _.filter(topo.bridgeNodes, function(bridgeNode) {
              return bridgeNode.getFLowName() === bridgeId;
            })[0];

            if (bridgeNode) {
              bridgeNode.flowInfo['features'] = node[flowInfoKeys.FEATURE];
              bridgeNode.flowInfo['software'] = node[flowInfoKeys.SOFTWARE];
              bridgeNode.flowInfo['hardware'] = node[flowInfoKeys.HARDWARE];
              bridgeNode.flowInfo['manufacturer'] = node[flowInfoKeys.MANUFACTURER];
              bridgeNode.flowInfo['ip'] = node[flowInfoKeys.IP];

              _.each(node[flowInfoKeys.TABLE], function(entry) {
                if (entry['id']) {
                  var tableId = entry['id'];
                  _.each(entry['flow'], function(flow) {
                    bridgeNode.flowTable[tableId] = flow['id'];
                  });
                }
              });
            }
          });

          cb(topo);
        },
        function(err) {
          throw err;
        }
      );
    }

    return this.svc;
  };
  TopologySvc.prototype = Object.create(RestConfRestangular.prototype);
  TopologySvc.$inject = ['OvsdbRestangular', 'nodeIdentifier', 'ovsNodeKeys', 'bridgeNodeKeys', 'tpKeys', 'flowInfoKeys', 'linkIdentifier', '$q', '$http'];

  var NeutronSvc = function(OvsdbRestangular, $q, $http) {
    var parent = {};
    NorthBoundRestangular.call(this, OvsdbRestangular, parent);

    this.svc.getNetworks = function(cb) {
      var networkDefer = parent.base('neutron').one('networks').getList();

      networkDefer.then(function(data) {
        var networks = data['networks'],
          networkArray = [];

        if (!networks) {
          throw new Error('Invalid format from neutron networks');
        }

        _.each(networks, function(network) {
          networkArray.push(new OvsCore.Neutron.Network(
            network['id'],
            network['name'],
            network['shared'],
            network['status']
          ));
        });
        cb(networkArray)
      });
    };

    return this.svc;
  };
  NeutronSvc.prototype = Object.create(NorthBoundRestangular.prototype);
  NeutronSvc.$inject = ['OvsdbRestangular', '$q', '$http'];

  ovsdb.register.factory('TopologySvc', TopologySvc);
  ovsdb.register.factory('NeutronSvc', NeutronSvc);
});
