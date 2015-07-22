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

  // nbv2 support depricated in dlux
  ovsdb.register.factory('NeutronRestangular', ['Restangular', function(Restangular) {
    return Restangular.withConfig(function(RestangularConfig) {
      RestangularConfig.setBaseUrl('http://localhost:8080/controller/nb/v2/neutron');
    });
  }]);

  function obtainDataFromCache($cacheFactory, $q, key, fn, ctx) {
    var ovsCache = null,
      cacheDefer = $q.defer();

    if (angular.isUndefined($cacheFactory.get('OvsCache'))) {
      ovsCache = $cacheFactory('OvsCache');
    } else {
      ovsCache = $cacheFactory.get('OvsCache');
    }

    if (angular.isUndefined(ovsCache.get(key))) {
      fn.call(ctx, function(data) {
          ovsCache.put(key, {
            obj : data,
            timestamp : Date.now() + 300000 // 5 mintues
          });
          cacheDefer.resolve(data);
      });
    } else {
      var cacheObj = ovsCache.get(key);
      if (cacheObj.timestamp < Date.now()) {
        fn.call(ctx, function(data) {
            ovsCache.put(key, {
              obj : data,
              timestamp : Date.now() + 300000 // 5 mintues
            });
            cacheDefer.resolve(networks);
        });
      } else {
        cacheDefer.resolve(cacheObj.obj);
      }
    }
    return cacheDefer.promise;
  }

  var TopologySvc = function(OvsdbRestangular, nodeIdentifier, ovsNodeKeys, bridgeNodeKeys, tpKeys, flowInfoKeys, linkIdentifier, $q, $http, $cacheFactory) {
    var svc = {
      base: function(type) {
        return OvsdbRestangular.one('restconf').one(type);
      }
    };

    function parseOvsdbNode(node) {
      var inetMgr = '',
        inetNode = '',
        otherLocalIp = '',
        otherInfo = null,
        connectionInfo = null;

      connectionInfo = node[ovsNodeKeys.CONNECTION_INFO];
      otherInfo = node[ovsNodeKeys.OTHER_CONFIG];

      if (_.isObject(connectionInfo)) {
        inetMgr = connectionInfo[ovsNodeKeys.LOCAL_IP] + ':' + connectionInfo[ovsNodeKeys.LOCAL_PORT];
        inetNode = connectionInfo[ovsNodeKeys.REMOTE_IP] + ':' + connectionInfo[ovsNodeKeys.REMOTE_PORT];
      }

      if (_.isArray(otherInfo)) {
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
          ifaceId = ext[tpKeys.EXTERNAL_KEY_VALUE] || '';
        }
      });

      return new OvsCore.TerminationPoint(tp[tpKeys.NAME], tp[tpKeys.OF_PORT], tp[tpKeys.INTERFACE_TYPE], mac, ifaceId);
    }

    function fetchTopology(cb) {
      var invNodeDefer = this.base('operational').one('opendaylight-inventory:nodes').getList();
      var netTopoDefer = this.base('operational').one('network-topology:network-topology').getList();

      // be sure all data are loaded
      $q.all([invNodeDefer, netTopoDefer]).then(function(values) {
          var invNode = values[0],
            netTopo = values[1],
            index_hash = [],
            i = 0;

          // check if the data look fine in network topology
          if (!netTopo || !netTopo['network-topology'] || !netTopo['network-topology'].topology) {
            throw new Error('Invalid json format while parsing network-topology');
          }

          // check if the data look fine in inventory node
          if (!invNode || !invNode.nodes || !invNode.nodes.node) {
            throw new Error('Invalid JSON format while parsing inventory-node');
          }

          // get all topologies and start looping
          var topologies = netTopo['network-topology'].topology,
            nodes = invNode.nodes.node,
            topo = new OvsCore.Topology();

          _.each(topologies, function(topology, topo_index) {
            if (!topology.hasOwnProperty('topology-id')) {
              throw new Error('Invalide JSON format, no topology-id for the topology [' + topo_index + ']');
            }

            // if there no node it will be an empty array so noop
            (topology.node || []).forEach(function(node) {
              if (!node[nodeIdentifier.ID]) {
                throw new Error('Unexpected node : undefined ' + nodeIdentifier.ID + ' key');
              }
              index_hash[node[nodeIdentifier.ID]] = i++;

              if (node['ovsdb:bridge-name']) {
                //bridge Node
                topo.registerBridgeNode(parseBridgeNode(node));
              } else if (node['ovsdb:connection-info']) {
                // obsvdb Node
                topo.registerOvsdbNode(parseOvsdbNode(node));
              }
            });

            // if there no link it will be an empty array so noop
            (topology.link || []).forEach(function(link) {

              var source = link[linkIdentifier.SRC]['source-node'],
                dest = link[linkIdentifier.DEST]['dest-node'];

              topo.registerLink(new OvsCore.Link(link[linkIdentifier.ID], source, dest));
            });

          });

          // match info for bridge node
          _.each(nodes, function(node, index) {
            if (!node.id) {
              return;
            }

            var bridgeId = node.id;

            var bridgeNode = _.filter(topo.bridgeNodes, function(bridgeNode) {
              return bridgeNode.getFLowName() === bridgeId;
            })[0];

            if (bridgeNode) {
              bridgeNode.flowInfo.features = node[flowInfoKeys.FEATURE];
              bridgeNode.flowInfo.software = node[flowInfoKeys.SOFTWARE];
              bridgeNode.flowInfo.hardware = node[flowInfoKeys.HARDWARE];
              bridgeNode.flowInfo.manufacturer = node[flowInfoKeys.MANUFACTURER];
              bridgeNode.flowInfo.ip = node[flowInfoKeys.IP];

              _.each(node[flowInfoKeys.TABLE], function(entry) {
                if (entry.id) {
                  var tableId = entry.id;
                  _.each(entry.flow, function(flow) {
                    bridgeNode.flowTable[tableId] = flow.id;
                  });
                }
              });
            }
          });
/*
          // extract tunnel from tp of OvsNode
          _.each(topo.bridgeNodes, function(node, index) {
            _.each(node.tPs,function(tp, index) {
              // tunnel
              if (tp.name.indexOf('vxlan') != -1) {
                var destIP = tp.name.replace('vxlan-', '');
                var destNode = _.filter(topo.bridgeNode, function(node) {
                  return node.flowInfo['ip']  === destIP;
                });

                if (_.isEmpty(destNode)) {
                  return false;
                }
                topo.registerLink(new OvsCore.TunnelLink(tp.name, node.nodeId, destNode.nodeId));
              }
            });
          });*/



          topo.updateLink();
          cb(topo);
        },
        function(err) {
          throw err;
        }
      );
    }

    svc.getTopologies = function() {
      return obtainDataFromCache($cacheFactory, $q, 'topologies', fetchTopology, this);
    };

    return svc;
  };
  TopologySvc.$inject = ['OvsdbRestangular', 'nodeIdentifier', 'ovsNodeKeys', 'bridgeNodeKeys', 'tpKeys', 'flowInfoKeys', 'linkIdentifier', '$q', '$http', '$cacheFactory'];

  var NeutronSvc = function(NeutronRestangular, $cacheFactory, $q, $http) {

    var svc = {
      base: function(type) {
        return NeutronRestangular.one(type);
      }
    };

    function fetchNetworks(cb) {
      var networkDefer = svc.base('networks').getList();
      var subnetskDefer = svc.base('subnets').getList();

      $q.all([subnetskDefer, networkDefer]).then(function(datas) {
        var subnets = datas[0],
          networks = datas[1],
          networkArray = [],
          subnetHash = {};

        if (!networks || !networks.networks) {
          throw new Error('Invalid format from neutron networks');
        }

        if (!subnets || !subnets.subnets) {
          throw new Error('Invalid format from neutron subnets');
        }

        _.each(subnets.subnets, function(subnet) {
            if (!subnetHash[subnet.network_id]) {
              subnetHash[subnet.network_id] = [];
            }
            subnetHash[subnet.network_id].push(new OvsCore.Neutron.SubNet(
              subnet.id,
              subnet.network_id,
              subnet.name,
              subnet.ip_version,
              subnet.cidr,
              subnet.gateway_ip
            ));
        });

        _.each(networks.networks, function(network) {
          var net = new OvsCore.Neutron.Network(
            network.id,
            network.name,
            network.shared,
            network.status,
            network['router:external']
          );

          net.addSubNets(subnetHash[net.id]);
          networkArray.push(net);
        });
          cb(networkArray);
      });
    }

    function fetchRouters(cb) {
      var routerDefer = svc.base('routers').getList();
      routerDefer.then(function(data) {
        var routers = data.routers,
          routerArray = [];

        if (!routers) {
          throw new Error('Invalid format from neutron routers');
        }
        _.each(routers, function(router) {
            var id = router.id,
            name = router.name,
            status = router.status,
            tenantId = router.tenant_id;

            var extGateWayInfo = router.external_gateway_info;

            routerArray.push(new OvsCore.Neutron.Router(
              id, name, status, tenantId, extGateWayInfo
            ));
        });
        cb(routerArray);
      });
    }

    function fetchPorts(cb) {
      var portDefer = svc.base('ports').getList();
      portDefer.then(function(data){
        var ports = data.ports,
          portArray = [];

        if (!ports) {
          throw new Error('Invalid format from neutron ports');
        }

        _.each(ports, function(port) {
          portArray.push(new OvsCore.Neutron.Port(
            port.id,
            port.network_id,
            port.name,
            port.tenant_id,
            port.device_id,
            port.device_owner,
            port.fixed_ips,
            port.mac_address
          ));
        });
        cb(portArray);
      });
    }

    svc.getNetworks = function() {
        return obtainDataFromCache($cacheFactory, $q, 'network', fetchNetworks, this);
    };

    svc.getPorts = function() {
      return obtainDataFromCache($cacheFactory, $q, 'ports', fetchPorts, this);
    };

    svc.getRouters = function() {
      return obtainDataFromCache($cacheFactory, $q, 'routers', fetchRouters, this);
    };

    return svc;
  };
  NeutronSvc.$inject = ['NeutronRestangular', '$cacheFactory', '$q', '$http'];

  var OvsUtil = function(NeutronSvc, TopologySvc, $q) {
    var svc = {};

    svc.extractLogicalTopology = function(cb) {
      var networksDefer = NeutronSvc.getNetworks(),
        routersDefer = NeutronSvc.getRouters(),
        portsDefer = NeutronSvc.getPorts(),
        netTopoDefer = TopologySvc.getTopologies();

      $q.all([networksDefer, routersDefer, portsDefer, netTopoDefer]).then(function (datas) {
        var networks = datas[0],
         routers = datas[1],
         ports = datas[2],
         topo = datas[3];

        // match ports with elements
         _.each(ports, function(port) {
           port.topoInfo = [];
           // corelate port.topoInfo data with network topology termination point
           _.each(topo.bridgeNodes, function(bridge) {
             _.each(bridge.tPs, function(tp) {
               if (tp.ifaceId === port.id) {
                 port.topoInfo.push({
                   name : tp.name,
                   ofPort : tp.ofPort,
                   mac : bridge.dpIp,
                   bridgeName : bridge.getFLowName(),
                   ovsNode : findOvsdbNodeForBridge(topo.ovsdbNodes, bridge)
                 });
               }
             });
           });

           switch(port.deviceOwner) {
             case 'network:router_gateway':
             case 'network:router_interface':
              var router = _.find(routers, function(r) { return r.id === port.deviceId; });
              if (router) {
                router.interfaces.push({
                  id: port.id,
                  networkId : port.networkId,
                  ip : port.fixed_ips[0],
                  mac : port.mac,
                  type: port.deviceOwner.replace('network:', ''),
                  tenantId : port.tenantId,
                  topoInfo: port.topoInfo
                });
              }
              break;
            case 'compute:None':
            case 'compute:nova':
            case 'network:dhcp':
              var network = _.find(networks, function(n) { return n.id === port.networkId;});
              if (network) {
                network.instances.push(new OvsCore.Neutron.Instance(
                  port.id,
                  port.networkId,
                  port.name,
                  port.fixed_ips[0].ip_address,
                  port.mac,
                  port.deviceOwner,
                  port.tenantId,
                  port.topoInfo
                ));
              }
              break;
           }

         });

         // find all routers for a specific networksDefer
         _.each(networks, function(network) {
           network.routers = _.filter(routers, function(router) {
             return network.id === router.externalGateway.network_id;
           });
         });

         cb(networks);
      });
    };

    return svc;
  };

  function findOvsdbNodeForBridge(ovsdbNodes, bridge) {
    return _.find(ovsdbNodes, function(node) {
      return bridge.nodeId.indexOf(node.nodeId) > -1;
    });
  }

  OvsUtil.$inject = ['NeutronSvc', 'TopologySvc', '$q'];

  ovsdb.register.factory('TopologySvc', TopologySvc);
  ovsdb.register.factory('NeutronSvc', NeutronSvc);
  ovsdb.register.factory('OvsUtil', OvsUtil);
});
