/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/ovsdb/ovsdb.module', 'app/ovsdb/OvsCore', 'underscore', 'app/ovsdb/ovsdb.constant'], function (ovsdb, OvsCore, _) {
  'use strict';

  ovsdb.register.factory('OvsdbRestangular', ['Restangular', 'ENV', function (Restangular, ENV) {
    return Restangular.withConfig(function (RestangularConfig) {
      RestangularConfig.setBaseUrl(ENV.getBaseURL("MD_SAL"));
    });
  }]);

  // nbv2 support depricated in dlux
  ovsdb.register.factory('NeutronRestangular', ['Restangular', function (Restangular) {
    return Restangular.withConfig(function (RestangularConfig) {
      var baseUrl = window.location.protocol + '//' + window.location.hostname;
      RestangularConfig.setBaseUrl(baseUrl + ':8080/controller/nb/v2/neutron');
    });
  }]);

  ovsdb.register.factory('CacheFactory', function ($q) {
    var svc = {},
      ovsCache = {};
    /*BUG : Using the persistant cache make the physical
     * graph links to stop reacting with the force layout
     * algorithm. The current behavior is to use the cache
     * only the pile up the datas.
     */
    svc.obtainDataFromCache = function (key, fn, ctx) {
      var cacheDefer = $q.defer();

      if (angular.isUndefined(ovsCache[key])) {
        fn.call(ctx, function (data) {
          ovsCache[key] = {
            obj: data,
            timestamp: Date.now() + 2000 //300000 // 5 mintues
          };
          cacheDefer.resolve(data);
        });
      } else {
        var cacheObj = ovsCache[key];
        if (cacheObj.timestamp < Date.now() || _.isEmpty(cacheObj.obj)) {
          fn.call(ctx, function (data) {
            ovsCache[key] = {
              obj: data,
              timestamp: Date.now() + 2000 //300000 // 5 mintues
            };
            cacheDefer.resolve(data);
          });
        } else {
          cacheDefer.resolve(cacheObj.obj);
        }
      }

      return cacheDefer.promise;
    };

    svc.getCacheObj = function (key) {
      if (angular.isUndefined(ovsCache[key])) {
        ovsCache[key] = {};
      }
      return ovsCache[key];
    };

    return svc;
  });

  var TopologySvc = function (OvsdbRestangular, nodeIdentifier, ovsNodeKeys, bridgeNodeKeys, tpKeys, flowInfoKeys, linkIdentifier, OVSConstant, $q, $http, CacheFactory) {
    var svc = {
      base: function (type) {
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
        _.each(otherInfo, function (value) {
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

      _.each(controllerEntries, function (value) {
        controllerTarget = value[bridgeNodeKeys.TARGET];
        controllerEntries = value[bridgeNodeKeys.IS_CONNECTED];
        return false; // break the anonymus function
      });

      bridgeNode = new OvsCore.BridgeNode(node[bridgeNodeKeys.NODE_ID], node[bridgeNodeKeys.DATA_PATH], node[bridgeNodeKeys.BRIDGE_NAME], controllerTarget, controllerConnected);

      _.each(tp, function (value) {
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
        extInfo = tp['ovsdb:port-external-ids'] || tp['ovsdb:interface-external-ids'],
        type = tp[tpKeys.INTERFACE_TYPE];

      _.each(extInfo, function (ext) {
        if (ext[tpKeys.EXTERNAL_KEY_ID] === tpKeys.ATTACHED_MAC) {
          mac = ext[tpKeys.EXTERNAL_KEY_VALUE];
        }
        if (ext[tpKeys.EXTERNAL_KEY_ID] === tpKeys.IFACE_ID) {
          ifaceId = ext[tpKeys.EXTERNAL_KEY_VALUE] || '';
        }
      });
      if (type === OVSConstant.TP_TYPE.VXLAN) {
        var localIp = null,
          remoteIp = null;
        _.each(tp['ovsdb:options'], function (option) {
          switch (option.option) {
            case 'local_ip':
              localIp = option.value;
              break;
            case 'remote_ip':
              remoteIp = option.value;
              break;
          }
        });
        return new OvsCore.Tunnel(tp[tpKeys.NAME], tp[tpKeys.OF_PORT], type, mac, ifaceId, localIp, remoteIp);
      }
      return new OvsCore.TerminationPoint(tp[tpKeys.NAME], tp[tpKeys.OF_PORT], type, mac, ifaceId);

    }

    function fetchTopology(cb) {
      var invNodeDefer = this.base('operational').one('opendaylight-inventory:nodes').getList();
      var netTopoDefer = this.base('operational').one('network-topology:network-topology').getList();

      // be sure all data are loaded
      $q.all([invNodeDefer, netTopoDefer]).then(function (values) {
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

          _.each(topologies, function (topology, topo_index) {
            if (!topology.hasOwnProperty('topology-id')) {
              throw new Error('Invalide JSON format, no topology-id for the topology [' + topo_index + ']');
            }

            // if there no node it will be an empty array so noop
            (topology.node || []).forEach(function (node) {
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
            (topology.link || []).forEach(function (link) {

              var source = link[linkIdentifier.SRC]['source-node'],
                dest = link[linkIdentifier.DEST]['dest-node'];

              topo.registerLink(new OvsCore.Link(link[linkIdentifier.ID], source, dest));
            });

          });

          _.each(nodes, function (node, index) {
            if (!node.id) {
              return;
            }

            var bridgeId = node.id;

            var bridgeNode = _.filter(topo.bridgeNodes, function (bridgeNode) {
              return bridgeNode.getFLowName() === bridgeId;
            })[0];

            // match info for bridge node
            if (bridgeNode) {
              bridgeNode.flowInfo.features = node[flowInfoKeys.FEATURE];
              bridgeNode.flowInfo.software = node[flowInfoKeys.SOFTWARE];
              bridgeNode.flowInfo.hardware = node[flowInfoKeys.HARDWARE];
              bridgeNode.flowInfo.manufacturer = node[flowInfoKeys.MANUFACTURER];
              bridgeNode.flowInfo.ip = node[flowInfoKeys.IP];

              _.each(node[flowInfoKeys.TABLE], function (entry) {
                if (!_.isUndefined(entry.id)) {
                  _.each(entry.flow, function (flow) {
                    bridgeNode.addFlowTableInfo({
                      key: flow.table_id,
                      value: flow.id
                    });
                  });
                }
              });
            }
          });

          // show relation between ovsNode and switch with a link
          _.each(topo.ovsdbNodes, function (node, index) {
            var bridges = _.filter(topo.bridgeNodes, function (bnode) {
              return bnode.nodeId.indexOf(node.nodeId) > -1;
            });
            _.each(bridges, function (bridge) {
              var size = _.size(topo.links),
                link = new OvsCore.BridgeOvsLink(++size, node.nodeId, bridge.nodeId);
              topo.registerLink(link);
            });
          });

          function findVxlan(bridgeNode) {
            var tunnels = [];

            _.each(bridgeNode, function (node) {
              var ovsdbNode = _.find(topo.ovsdbNodes, function (oNode) {
                return node.nodeId.indexOf(oNode.nodeId) > -1;
              });
              if (!ovsdbNode) {
                return false;
              }
              _.each(node.tPs, function (tp, index) {
                if (tp instanceof OvsCore.Tunnel) {
                  tunnels.push({
                    port: tp,
                    bridge: node
                  });
                }
              });
            });

            return tunnels;
          }

          // extract all tunnel paired with their bridge
          var tunnels = findVxlan(topo.bridgeNodes);
          // loop over all pairs
          for (var index = 0; index < tunnels.length; ++index) {
            var tunnel = tunnels[index],
              currIp = tunnel.port.localIp,
              destIp = tunnel.port.remoteIp,
              pairIndex = 0,
              linkedBridge = _.find(tunnels, function (t, i) {
                pairIndex = i;
                return t.port.remoteIp === currIp && t.port.localIp == destIp;
              });

            if (linkedBridge) {
              tunnels.splice(pairIndex, 1);
              topo.registerLink(new OvsCore.TunnelLink(tunnel.port.name + linkedBridge.port.name, tunnel.bridge.nodeId, linkedBridge.bridge.nodeId));
            }
          }

          topo.updateLink();
          cb(topo);
        },
        function (err) {
          throw err;
        }
      );
    }

    svc.getTopologies = function () {
      return CacheFactory.obtainDataFromCache('topologies', fetchTopology, this);
    };

    return svc;
  };
  TopologySvc.$inject = ['OvsdbRestangular', 'nodeIdentifier', 'ovsNodeKeys', 'bridgeNodeKeys', 'tpKeys', 'flowInfoKeys', 'linkIdentifier', 'OVSConstant', '$q', '$http', 'CacheFactory'];

  var NeutronSvc = function (NeutronRestangular, CacheFactory, $q, $http) {
    var svc = {
        base: function (type) {
          return NeutronRestangular.one(type);
        }
      },
      tenant_hash = {};

    function fetchSubNetworks(cb) {
      var subnetskDefer = svc.base('subnets').getList();
      subnetskDefer.then(function (data) {
        var subnets = data,
          subnetHash = {};

        if (!subnets || !subnets.subnets) {
          throw new Error('Invalid format from neutron subnets');
        }

        _.each(subnets.subnets, function (subnet) {
          if (!subnetHash[subnet.network_id]) {
            subnetHash[subnet.network_id] = [];
          }
          tenant_hash[subnet.tenant_id] = {};
          subnetHash[subnet.network_id].push(new OvsCore.Neutron.SubNet(
            subnet.id,
            subnet.network_id,
            subnet.name,
            subnet.ip_version,
            subnet.cidr,
            subnet.gateway_ip,
            subnet.tenant_id
          ));
        });
        cb(subnetHash);
      });
    }

    function fetchNetworks(cb) {
      var networkDefer = svc.base('networks').getList();
      var subnetskDefer = svc.getSubNets();

      $q.all([subnetskDefer, networkDefer]).then(function (datas) {
        var subnetsHash = datas[0],
          networks = datas[1],
          networkArray = [];

        if (!networks || !networks.networks) {
          throw new Error('Invalid format from neutron networks');
        }

        _.each(networks.networks, function (network) {
          var net = new OvsCore.Neutron.Network(
            network.id,
            network.name,
            network.shared,
            network.status,
            network['router:external'],
            network.tenant_id
          );
          tenant_hash[net.tenantId] = {};
          net.addSubNets(subnetsHash[net.id]);
          networkArray.push(net);
        });
        cb(networkArray);
      });
    }

    function fetchRouters(cb) {
      var routerDefer = svc.base('routers').getList();
      routerDefer.then(function (data) {
        var routers = data.routers,
          routerArray = [];

        if (!routers) {
          throw new Error('Invalid format from neutron routers');
        }
        _.each(routers, function (router) {
          var id = router.id,
            name = router.name,
            status = router.status,
            tenantId = router.tenant_id,
            extGateWayInfo = router.external_gateway_info;
          tenant_hash[tenantId] = {};
          routerArray.push(new OvsCore.Neutron.Router(
            id, name, status, tenantId, extGateWayInfo
          ));
        });
        cb(routerArray);
      });
    }

    function fetchPorts(cb) {
      var portDefer = svc.base('ports').getList();
      portDefer.then(function (data) {
        var ports = data.ports,
          portArray = [];

        if (!ports) {
          throw new Error('Invalid format from neutron ports');
        }
        _.each(ports, function (port) {
          tenant_hash[port.tenant_id] = {};
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

    function fetchFloatingIps(cb) {
      var floatingIpDefer = svc.base('floatingips').getList();
      floatingIpDefer.then(function (data) {
        var floatingIps = data.floatingips,
          floatingIpArray = [];

        if (!floatingIps) {
          throw new Error('Invalid format from neutron floatingIps');
        }

        _.each(floatingIps, function (fIp) {
          tenant_hash[fIp.tenant_id] = {};
          floatingIpArray.push(new OvsCore.Neutron.FloatingIp(
            fIp.id,
            fIp.floating_network_id,
            fIp.port_id,
            fIp.fixed_ip_address,
            fIp.floating_ip_address,
            fIp.tenant_id,
            fIp.status
          ));
        });

        cb(floatingIpArray);
      });
    }

    svc.getNetworks = function () {
      return CacheFactory.obtainDataFromCache('networks', fetchNetworks, this);
    };

    svc.getSubNets = function () {
      return CacheFactory.obtainDataFromCache('subnet', fetchSubNetworks, this);
    };

    svc.getPorts = function () {
      return CacheFactory.obtainDataFromCache('ports', fetchPorts, this);
    };

    svc.getRouters = function () {
      return CacheFactory.obtainDataFromCache('routers', fetchRouters, this);
    };

    svc.getFloatingIps = function () {
      return CacheFactory.obtainDataFromCache('floatingips', fetchFloatingIps, this);
    };

    svc.getAllTenants = function () {
      return Object.keys(tenant_hash);
    };

    return svc;
  };
  NeutronSvc.$inject = ['NeutronRestangular', 'CacheFactory', '$q', '$http'];

  var OvsUtil = function (NeutronSvc, TopologySvc, CacheFactory, $q) {
    var svc = {};

    function findOvsdbNodeForBridge(ovsdbNodes, bridge) {
      return _.find(ovsdbNodes, function (node) {
        return bridge.nodeId.indexOf(node.nodeId) > -1;
      });
    }

    function pileUpTopologyData(cb) {
      var networksDefer = NeutronSvc.getNetworks(),
        routersDefer = NeutronSvc.getRouters(),
        portsDefer = NeutronSvc.getPorts(),
        floatingDefer = NeutronSvc.getFloatingIps(),
        netTopoDefer = TopologySvc.getTopologies();

      $q.all([networksDefer, routersDefer, portsDefer, floatingDefer, netTopoDefer]).then(function (datas) {
        var networks = datas[0],
          routers = datas[1],
          ports = datas[2],
          floatingIps = datas[3],
          topo = datas[4];

        // match ports with elements
        _.each(ports, function (port) {
          port.topoInfo = [];
          // corelate port.topoInfo data with network topology termination point
          _.each(topo.bridgeNodes, function (bridge) {
            _.each(bridge.tPs, function (tp) {
              if (tp.ifaceId === port.id) {
                port.topoInfo.push({
                  name: tp.name,
                  ofPort: tp.ofPort,
                  mac: bridge.dpIp,
                  bridge: bridge,
                  ovsNode: findOvsdbNodeForBridge(topo.ovsdbNodes, bridge)
                });
              }
            });
          });

          switch (port.deviceOwner) {
            case 'network:router_gateway':
            case 'network:router_interface':
              var router = _.find(routers, function (r) {
                return r.id === port.deviceId;
              });
              if (router) {
                router.interfaces.push({
                  id: port.id,
                  networkId: port.networkId,
                  ip: port.fixed_ips[0],
                  mac: port.mac,
                  type: port.deviceOwner.replace('network:', ''),
                  tenantId: port.tenantId,
                  topoInfo: port.topoInfo
                });
              }
              break;
            case 'compute:None':
            case 'compute:nova':
            case 'network:dhcp':
              var network = _.find(networks, function (n) {
                  return n.id === port.networkId;
                }),
                inst = null;

              if (network) {
                inst = new OvsCore.Neutron.Instance(port.id, port.networkId,
                  port.name, port.fixed_ips[0].ip_address, port.mac,
                  port.deviceOwner, port.tenantId, port.topoInfo);

                inst.extractFloatingIps(floatingIps);
                network.instances.push(inst);
              }
              break;
          }

        });

        // find all routers for a specific network
        _.each(networks, function (network) {
          network.routers = _.filter(routers, function (router) {
            return network.id === router.externalGateway.network_id;
          });

          // order instance by ip
          network.instances.sort(function (a, b) {
            var ipA = a.ip.slice(a.ip.lastIndexOf('.') + 1),
              ipB = b.ip.slice(b.ip.lastIndexOf('.') + 1);
            return ipA - ipB;
          });
        });

        cb(networks);
      });
    }

    svc.getLogicalTopology = function () {
      return CacheFactory.obtainDataFromCache('logicalTopology', pileUpTopologyData, this);
    };

    svc.extractLogicalByTenant = function (tenantId, subSet) {
      var lTopoDefer = svc.getLogicalTopology(),
        resultDefer = $q.defer();
      lTopoDefer.then(function () {
        var ports = CacheFactory.getCacheObj('ports').obj,
          filteredPorts = _.filter(ports, function (p) {
            return p.tenantId === tenantId;
          });

        if (!_.isEmpty(filteredPorts)) {
          var bridgeHash = {};
          _.each(filteredPorts, function (p) {
            if (!_.isEmpty(p.topoInfo) && !bridgeHash[p.topoInfo[0].bridge.nodeId]) {
              bridgeHash[p.topoInfo[0].bridge.nodeId] = {};
            }
          });
          var ovsdbHash = {};
          _.each(filteredPorts, function (p) {
            if (!_.isEmpty(p.topoInfo) && !ovsdbHash[p.topoInfo[0].ovsNode.nodeId]) {
              ovsdbHash[p.topoInfo[0].ovsNode.nodeId] = {};
            }
          });

          resultDefer.resolve([Object.keys(bridgeHash), Object.keys(ovsdbHash)]);
        } else {
          resultDefer.resolve([], []);
        }
      });
      return resultDefer.promise;
    };

    svc.extractLogicalBySubnet = function (subnets, subSet) {
      var lTopoDefer = svc.getLogicalTopology(),
        resultDefer = $q.defer();
      lTopoDefer.then(function () {
        var ports = CacheFactory.getCacheObj('ports').obj,
          networks = CacheFactory.getCacheObj('networks').obj;

        var filteredPorts = _.filter(ports, function (p) {
          var net = _.find(networks, function (d) {
            return d.id === p.networkId;
          });

          return net.asSubnet(subnets);
        });
        if (!_.isEmpty(filteredPorts)) {
          var bridgeHash = {};
          _.each(filteredPorts, function (p) {
            if (!_.isEmpty(p.topoInfo) && !bridgeHash[p.topoInfo[0].bridge.nodeId]) {
              bridgeHash[p.topoInfo[0].bridge.nodeId] = {};
            }
          });
          var ovsdbHash = {};
          _.each(filteredPorts, function (p) {
            if (!_.isEmpty(p.topoInfo) && !ovsdbHash[p.topoInfo[0].ovsNode.nodeId]) {
              ovsdbHash[p.topoInfo[0].ovsNode.nodeId] = {};
            }
          });
          resultDefer.resolve([Object.keys(bridgeHash), Object.keys(ovsdbHash)]);
        } else {
          resultDefer.resolve([], []);
        }
      });
      return resultDefer.promise;
    };

    return svc;
  };

  OvsUtil.$inject = ['NeutronSvc', 'TopologySvc', 'CacheFactory', '$q'];

  ovsdb.register.factory('TopologySvc', TopologySvc);
  ovsdb.register.factory('NeutronSvc', NeutronSvc);
  ovsdb.register.factory('OvsUtil', OvsUtil);
});
