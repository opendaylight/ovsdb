/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['underscore'], function (_) {

  var Topology = (function () {

    function Topology(topoId) {
      this.topoId = topoId || '';
      this._bridgeNodes = {};
      this._ovsdbNodes = {};
      this._links = {};
    }

    Object.defineProperties(Topology.prototype, {
      bridgeNodes: {
        get: function () {
          return this._bridgeNodes;
        }
      },
      nodes: {
        get: function () {
          return _.extend({}, this._bridgeNodes, this._ovsdbNodes);
        }
      },
      ovsdbNodes: {
        get: function () {
          return this._ovsdbNodes;
        }
      },
      links: {
        get: function () {
          return this._links;
        }
      }
    });

    Topology.prototype.registerBridgeNode = function (bridgeNode) {
      this._bridgeNodes[bridgeNode.nodeId] = bridgeNode;
    };

    Topology.prototype.registerOvsdbNode = function (ovsdbNode) {
      this._ovsdbNodes[ovsdbNode.nodeId] = ovsdbNode;
    };

    Topology.prototype.registerLink = function (link) {
      if (this._links[link.linkId]) {
        console.warn('Two links have the same id (' + link.linkId + '), the first one will be overrided');
      }
      this._links[link.linkId] = link;
    };

    Topology.prototype.updateLink = function () {
      _.each(this._links, (function (link, key) {
        if (link instanceof Link) {
          srcNode = _.filter(this._bridgeNodes, function (node) {
            return node.getFLowName() === link.srcNodeId;
          });
          destNode = _.filter(this._bridgeNodes, function (node) {
            return node.getFLowName() === link.destNodeId;
          });
          link.srcNodeId = srcNode[0].nodeId;
          link.destNodeId = destNode[0].nodeId;
        }
        link.source = Object.keys(this.nodes).indexOf(link.srcNodeId);
        link.target = Object.keys(this.nodes).indexOf(link.destNodeId);

      }).bind(this));
    };

    return Topology;
  })();

  var OvsNode = (function () {
    function OvsNode(nodeId, inetMgr, inetNode, otherLocalIp, ovsVersion) {
      this.nodeId = nodeId;
      this.inetMgr = inetMgr;
      this.inetNode = inetNode;
      this.otherLocalIp = otherLocalIp;
      this.ovsVersion = ovsVersion;
    }

    OvsNode.prototype.showIpAdress = function () {
      return this.otherLocalIp;
    };

    OvsNode.prototype.pretty = function () {
      return {
        'tabs': ['Info'],
        'containts': [{
          'hasHeader': false,
          'headers': [],
          'datas': [
            {
              key: 'ID',
              value: this.nodeId
            },
            {
              key: 'InetMgr',
              value: this.inetMgr
            },
            {
              key: 'InetNode',
              value: this.inetNode
            },
            {
              key: 'Local IP',
              value: this.otherLocalIp
            },
            {
              key: 'OVS Version',
              value: this.ovsVersion
            }
          ]
        }]
      };
    };

    return OvsNode;
  })();

  var BridgeNode = (function () {

    function BridgeNode(nodeId, dpIp, name, controllerTarget, controllerConnected) {
      this.nodeId = nodeId;
      this.dpIp = dpIp;
      this.name = name;
      this.controllerTarget = controllerTarget;
      this.controllerConnected = controllerConnected;
      this._tpList = [];
      this.flowInfo = {};
      this.flowTable = [];
    }

    Object.defineProperties(BridgeNode.prototype, {
      tPs: {
        get: function () {
          return this._tpList;
        }
      },
    });

    var dpToFlow = function (dpId) {
      return 'openflow:' + parseInt(dpId.replace(/:/g, ''), 16);
    };

    BridgeNode.prototype.getFLowName = function () {
      return (!this.dpIp) ? this.nodeId : dpToFlow(this.dpIp);
    };

    BridgeNode.prototype.addTerminationPoint = function (tp) {
      this._tpList.push(tp);
      this._tpList.sort(function (tp1, tp2) {
        return tp1.ofPort - tp2.ofPort;
      });
    };

    BridgeNode.prototype.addFlowTableInfo = function (flowTable) {
      this.flowTable.push(flowTable);
      this.flowTable.sort(function (ft1, ft2) {
        return ft1.key - ft2.key;
      });
    };

    BridgeNode.prototype.pretty = function () {
      return {
        'tabs': [
          'Basic Info',
          'Ports',
          'Flow Info',
          'Flow Tables'
        ],
        'containts': [
          {
            'hasHeader': false,
            'headers': [],
            'datas': [
              {
                key: 'ID',
                value: this.nodeId
              },
              {
                key: 'Name',
                value: this.name
              },
              {
                key: 'OpenFlow Name',
                value: this.getFLowName()
              },
              {
                key: 'Controller Target',
                value: this.controllerTarget
              },
              {
                key: 'Controller Connected',
                value: this.controllerConnected
              }
            ]
          },
          {
            'hasHeader': true,
            'header': ['Of Port', 'Name', 'Mac', 'IFace Id', ],
            'datas': this._tpList.map(function (s) {
              return [s.ofPort, s.name, s.mac, s.ifaceId];
            })
          },
          {
            'hasHeader': false,
            'headers': [],
            'datas': [
              {
                key: 'Manufacturer',
                value: this.flowInfo.manufacturer
              },
              {
                key: 'Hardware',
                value: this.flowInfo.hardware
              },
              {
                key: 'Software',
                value: this.flowInfo.software
              },
              {
                key: 'Feature',
                value: this.flowInfo.features
              },
              {
                key: 'Ip',
                value: this.flowInfo.ip
              }
              ]
          },
          {
            'hasHeader': true,
            'headers': ['Table Id', 'Value'],
            'datas': this.flowTable.map(function (t) {
              return [t.key, t.value];
            })
          }
        ]
      };
    };

    return BridgeNode;
  })();

  var TerminationPoint = (function () {
    function TerminationPoint(name, ofPort, tpType, mac, ifaceId) {
      this.name = name;
      this.ofPort = ofPort;
      this.tpType = tpType;
      this.mac = mac || '';
      this.ifaceId = ifaceId || '';
    }
    return TerminationPoint;
  })();

  var Tunnel = (function () {
    function Tunnel(name, ofPort, tpType, mac, ifaceId, localIp, remoteIp) {
      TerminationPoint.call(this, name, ofPort, tpType, mac, ifaceId);
      this.localIp = localIp;
      this.remoteIp = remoteIp;
    }
    Tunnel.prototype = Object.create(TerminationPoint.prototype);
    Tunnel.prototype.constructor = Tunnel;

    return Tunnel;
  })();

  var BaseLink = (function () {
    function BaseLink(linkId, srcNodeId, destNodeId, linkType, styles) {
      this.linkId = linkId;
      this.srcNodeId = srcNodeId;
      this.destNodeId = destNodeId;
      this.linkType = linkType;

      // styling
      styles = _.extend({}, styles);
      this.color = styles.color;
      this.width = styles.width || 1;
      this.dashArray = styles.dashArray || 'None';

      // d3js needed values
      this.source = -1;
      this.target = -1;
    }
    return BaseLink;
  })();

  var Link = (function () {
    function Link(linkId, srcNodeId, destNodeId) {
      var opt = {
        color: 'black'
      };

      BaseLink.call(this, linkId, srcNodeId, destNodeId, 'link', opt);
    }

    Link.prototype = Object.create(BaseLink.prototype);
    Link.prototype.constructor = Link;

    return Link;
  })();

  var TunnelLink = (function () {
    function TunnelLink(linkId, srcNodeId, destNodeId, linkType, color) {
      var opt = {
        color: 'green',
        width: 2,
        dashArray: '5,5'
      };
      BaseLink.call(this, linkId, srcNodeId, destNodeId, 'tunnel', opt);
    }

    TunnelLink.prototype = Object.create(BaseLink.prototype);
    TunnelLink.prototype.constructor = TunnelLink;

    return TunnelLink;
  })();

  var BridgeOvsLink = (function () {
    function BridgeOvsLink(linkId, srcNodeId, destNodeId, linkType, color) {
      var opt = {
        color: 'gray',
        dashArray: '10,10'
      };
      BaseLink.call(this, linkId, srcNodeId, destNodeId, 'bridgeOvsLink', opt);
    }

    BridgeOvsLink.prototype = Object.create(BaseLink.prototype);
    BridgeOvsLink.prototype.constructor = BridgeOvsLink;

    return BridgeOvsLink;
  })();

  var Util = (function () {
    var Maths = (function () {
      function Maths() {

      }
      // random function in javascript use timespan only
      Maths.Random = function (nseed) {
        var constant = Math.pow(2, 13) + 1,
          prime = 1987,
          maximum = 1000;

        if (nseed) {
          seed = nseed;
        }

        return {
          next: function (min, max) {
            seed *= constant;
            seed += prime;

            return min && max ? min + seed % maximum / maximum * (max - min) : seed % maximum / maximum;
          }
        };
      };
      return Maths;
    })();

    var String = (function () {

      function String() {

      }
      String.Format = function () {
        var s = arguments[0];
        for (var i = 0; i < arguments.length - 1; i++) {
          var reg = new RegExp("\\{" + i + "\\}", "gm");
          s = s.replace(reg, arguments[i + 1]);
        }
        return s;
      };

      return String;

    })();
    return {
      Math: Maths,
      String: String
    };
  })();

  var Neutron = (function () {

    var SubNet = (function () {
      function SubNet(id, networkId, name, ipVersion, cidr, gatewayIp, tenantId) {
        this.id = id;
        this.networkId = networkId;
        this.name = name;
        this.ipVersion = ipVersion;
        this.cidr = cidr;
        this.gatewayIp = gatewayIp;
        this.tenantId = tenantId;
      }
      return SubNet;
    })();

    var Network = (function () {
      function Network(id, name, shared, status, external, tenantId) {
        this.id = id;
        this.ip = '';
        this.name = name;
        this.shared = shared;
        this.status = status;
        this.external = external;
        this.tenantId = tenantId;
        this.subnets = [];
        this.instances = [];
        this.routers = [];
      }

      Network.prototype.addSubNets = function (subnets) {
        if (subnets) {
          if (_.isArray(subnets)) {
            var i = 0;
            for (; i < subnets.length; ++i) {
              this.subnets.push(subnets[i]);
            }
          } else {
            this.subnets.push(subnet);
          }
        }
      };

      Network.prototype.asSubnet = function (subnet) {
        return _.every(subnet, function (sub) {
          return _.some(this.subnets, function (s) {
            return s.id === sub;
          });
        }.bind(this));
      };

      Network.prototype.pretty = function () {
        return {
          'tabs': [
            'Info',
            'Subnets'
          ],
          'containts': [
            {
              'hasHeader': false,
              'headers': [],
              'datas': [
                {
                  key: 'ID',
                  value: this.id
                },
                {
                  key: 'Ip',
                  value: this.ip
                },
                {
                  key: 'Name',
                  value: this.name
                },
                {
                  key: 'Shared',
                  value: this.shared
                },
                {
                  key: 'Status',
                  value: this.status
                },
                {
                  key: 'External',
                  value: this.external
                },
                {
                  key: 'Tenant Id',
                  value: this.tenantId
                }
              ]
            },
            {
              'hasHeader': true,
              'header': ['ID', 'Name', 'Ip Version', 'Ip', 'Gateway Ip'],
              'datas': this.subnets.map(function (s) {
                return [s.id, s.name, s.ipVersion, s.cidr, s.gatewayIp];
              })
            }
          ]
        };
      };

      return Network;
    })();

    var Port = (function () {
      function Port(id, networkId, name, tenantId, deviceId, deviceOwner, fixed_ips, mac) {
        this.id = id;
        this.networkId = networkId;
        this.name = name;
        this.tenantId = '' + tenantId || '';
        this.deviceId = deviceId;
        this.deviceOwner = deviceOwner;
        this.fixed_ips = fixed_ips;
        this.mac = mac;
      }

      Port.prototype.pretty = function () {
        return [
          {
            key: 'ID',
            value: this.id
          },
          {
            key: 'Name',
            value: name
          },
          {
            key: 'Tenant Id',
            value: this.tenantId
          },
          {
            key: 'Device Id',
            value: this.deviceId
          },
          {
            key: 'Device Owner',
            value: this.deviceOwner
          },
          {
            key: 'MAC',
            value: this.mac
          }
        ];
      };

      return Port;
    })();

    var Router = (function () {
      function Router(id, name, status, tenantId, externalGateway) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.tenantId = tenantId;
        this.interfaces = [];
        this.externalGateway = externalGateway;
      }

      Router.prototype.pretty = function () {
        return {
          'tabs': [
            'Info',
            'Interfaces'
          ],
          'containts': [
            {
              'hasHeader': false,
              'headers': [],
              'datas': [
                {
                  key: 'ID',
                  value: this.id
                },
                {
                  key: 'Name',
                  value: this.name
                },
                {
                  key: 'status',
                  value: this.status
                },
                {
                  key: 'Tenant ID',
                  value: this.tenantId
                }
              ]
            },
            {
              'hasHeader': true,
              'header': ['ID', 'Type', 'Mac Address', 'Ip', 'Tenant Id'],
              'datas': this.interfaces.map(function (s) {
                return [s.id, s.type, s.mac, s.ip.ip_address, s.tenantId];
              })
            }
          ]
        };
      };

      return Router;
    })();

    var Instance = (function () {
      function Instance(id, networkId, name, ip, mac, deviceOwner, tenantId, topoInfo) {
        this.id = id;
        this.networkId = networkId;
        this.name = name;
        this.ip = ip;
        this.mac = '' + mac;
        this.type = deviceOwner;
        this.tenantId = tenantId;
        this.topoInfo = topoInfo || [];
        this.floatingIp = {};
      }

      Instance.prototype.extractFloatingIps = function (floatingIps) {
        var ctx = this;
        this.floatingIp = _.find(floatingIps, function (fIp) {
          return fIp.tenantId === ctx.tenantId &&
            fIp.fixedIp === ctx.ip;
        });
      };

      Instance.prototype.pretty = function () {
        return {
          'tabs': [
            'Info',
            'Ports'
          ],
          'containts': [
            {
              'hasHeader': false,
              'headers': [],
              'datas': [
                {
                  key: 'ID',
                  value: this.id
                },
                {
                  key: "Network Id",
                  value: this.networkId
                },
                {
                  key: 'Name',
                  value: this.name
                },
                {
                  key: 'Ip',
                  value: this.ip
                },
                {
                  key: 'Floating Ip',
                  value: (this.floatingIp) ? this.floatingIp.ip : 'Not found'
                },
                {
                  key: 'MAC',
                  value: this.mac
                },
                {
                  key: 'Type',
                  value: this.type
                },
                {
                  key: 'Tenant ID',
                  value: this.tenantId
                }
              ]
            },
            {
              'hasHeader': true,
              'header': ['Name', 'Of Port', 'Mac', 'Flow', 'Ovsdb Node', 'Ovsdb Node IP'],
              'datas': this.topoInfo.map(function (s) {
                return [s.name, s.ofPort, s.mac, s.bridge.getFLowName(), s.ovsNode.nodeId, s.ovsNode.showIpAdress()];
              })
            }
          ]
        };
      };

      return Instance;
    })();

    var FloatingIp = (function () {
      function FloatingIp(id, networkId, portId, fixedIp, floatingIp, tentantId, status) {
        this.id = id;
        this.networkId = networkId;
        this.portId = portId;
        this.fixedIp = fixedIp;
        this.ip = floatingIp;
        this.tenantId = tentantId;
        this.status = status;
      }

      return FloatingIp;
    })();

    return {
      Network: Network,
      Port: Port,
      Instance: Instance,
      FloatingIp: FloatingIp,
      Router: Router,
      SubNet: SubNet
    };
  })();

  return {
    OvsNode: OvsNode,
    BridgeNode: BridgeNode,
    TerminationPoint: TerminationPoint,
    Topology: Topology,
    Tunnel: Tunnel,
    BaseLink: BaseLink,
    Link: Link,
    TunnelLink: TunnelLink,
    BridgeOvsLink: BridgeOvsLink,
    Util: Util,
    Neutron: Neutron
  };
});
