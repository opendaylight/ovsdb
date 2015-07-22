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
        get: function() {
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
          srcNode = _.filter(this._bridgeNodes, function(node) {
            return node.getFLowName() === link.srcNodeId;
          }),
          destNode = _.filter(this._bridgeNodes, function(node) {
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

    OvsNode.prototype.pretty = function() {
      return [
        { key: 'ID', value: this.nodeId},
        { key: 'InetMgr', value: this.inetMgr},
        { key: 'InetNode', value: this.inetNode},
        { key: 'Local IP', value: this.otherLocalIp},
        { key: 'OVS Version', value: this.ovsVersion}
      ];
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
      this.flowTable = {};
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
    }

    BridgeNode.prototype.getFLowName = function () {
      return (!this.dpIp) ? this.nodeId : dpToFlow(this.dpIp);
    };

    BridgeNode.prototype.addTerminationPoint = function (tp) {
      this._tpList.push(tp);
    };

    BridgeNode.prototype.pretty = function() {
      return [
        { key: 'ID', value: this.nodeId},
        { key: 'Name', value: this.name},
        { key: 'OpenFlow Name', value: this.getFLowName()},
        { key: 'Controller Target', value: this.controllerTarget},
        { key: 'Controller Connected', value: this.controllerConnected}
      ];
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

  var BaseLink = (function() {
    function BaseLink(linkId, srcNodeId, destNodeId, linkType, styles) {
      this.linkId = linkId;
      this.srcNodeId = srcNodeId;
      this.destNodeId = destNodeId;
      this.linkType  = linkType;

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

  var TunnelLink = (function() {
    function TunnelLink(linkId, srcNodeId, destNodeId, linkType, color) {
      var opt = {
        color: 'green',
        width: 2
      };
      BaseLink.call(this, linkId, srcNodeId, destNodeId, 'tunnel', opt);
    }

    TunnelLink.prototype = Object.create(BaseLink.prototype);
    TunnelLink.prototype.constructor = TunnelLink;

    return TunnelLink;
  })();

  var BridgeOvsLink = (function() {
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

  var Util = (function() {
    var String = (function() {

      function String() {

      }
      String.Format = function() {
        var s = arguments[0];
        for (var i = 0; i < arguments.length - 1; i++) {
            var reg = new RegExp("\\{" + i + "\\}", "gm");
            s = s.replace(reg, arguments[i + 1]);
        }
        return s;
      }

      return String;

    })();
    return {
      String: String
    };
  })();

  var Neutron = (function() {
    /*
    id" : "aa382ad8-f4eb-45e5-a256-0db68a83419c",
          "network_id" : "150f1bcb-eb5c-4459-bfef-ad3ecb50c7cb",
          "name" : "subext1",
          "ip_version" : 4,
          "cidr" : "192.168.111.0/24",
          "gateway_ip" : "192.168.111.254",
          "dns_nameservers" : [ ],
          "allocation_pools" : [ {
             "start" : "192.168.111.21",
             "end" : "192.168.111.40"
          } ],
          "host_routes" : [ ],
          "enable_dhcp" : false,
          "tenant_id" : "a9b6ee2eefd545c3bca0d3caf3a6c6e1",
          "ipv6_address_mode" : null,
          "ipv6_ra_mode" : null
    */
    var SubNet = (function() {
      function SubNet(id, networkId, name, ipVersion, cidr, gatewayIp) {
        this.id = id;
        this.networkId = networkId;
        this.name = name;
        this.ipVersion = ipVersion;
        this.cidr = cidr;
        this.gatewayIp = gatewayIp;
      }
      return SubNet;
    })();

    var Network = (function() {
      function Network(id, name, shared, status) {
        this.id = id;
        this.name = name;
        this.shared = shared;
        this.status = status;
        this.subnets = [];
      }

      Network.prototype.addSubNets = function(subnets) {
        if (_.isArray(subnets)) {
          var i = 0;
          for (; i < subnets.length; ++i) {
            this.subnets.push(subnets[i]);
          }
        }
        else {
          this.subnets.push(subnet);
        }
      };

      return Network;
    })();

    var Port = (function(id, networkId, name, tenantId) {
      function Port() {
        this.id = id;
        this.networkId = networkId;
        this.name = name;
        this.tenantId = tenantId | '';
      }
      return Port;
    })();

    return {
      Network: Network,
      Port: Port,
      SubNet: SubNet
    };
  })();

  return {
    OvsNode: OvsNode,
    BridgeNode: BridgeNode,
    TerminationPoint: TerminationPoint,
    Topology: Topology,
    BaseLink: BaseLink,
    Link: Link,
    TunnelLink: TunnelLink,
    BridgeOvsLink:BridgeOvsLink,
    Util:Util,
    Neutron: Neutron
  };
});
