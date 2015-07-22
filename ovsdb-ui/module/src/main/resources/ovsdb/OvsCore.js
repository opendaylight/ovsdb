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

    Topology.prototype.registerFlowNode = function (flowNode) {
      this._flowNodes[flowNode.nodeId] = flowNode;
    };

    Topology.prototype.registerBridgeNode = function (bridgeNode) {
      this._bridgeNodes[bridgeNode.nodeId] = bridgeNode;
    };

    Topology.prototype.registerOvsdbNode = function (ovsdbNode) {
      this._ovsdbNodes[ovsdbNode.nodeId] = ovsdbNode;
    };

    Topology.prototype.registerLink = function (link) {
      this._links[link.linkId] = link;
    };

    Topology.prototype.updateLink = function () {
      _.each(this._flowLinks, (function (value, key) {
        var bridgeSrc = _.filter(this._bridgeNodes, function (b) {
          return b.getFLowName() === value.linkSrcNode;
        });

        var bridgeDest = _.filter(this._bridgeNodes, function (b) {
          return b.getFLowName() === value.linkDestNode;
        });

        value.source = Object.keys(this._bridgeNodes).indexOf(bridgeSrc[0].nodeId);
        value.target = Object.keys(this._bridgeNodes).indexOf(bridgeDest[0].nodeId);
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

  var Link = (function () {
    function Link(linkId, linkSrc, linkDest, linkSrcNode, linkDestNode, linkType, color) {
      this.linkId = linkId;
      this.linkSrc = linkSrc;
      this.linkDest = linkDest;
      this.linkSrcNode = linkSrcNode;
      this.linkDestNode = linkDestNode;
      this.linkType  = linkType || 'link';
      this.color = color || 'black';
      // d3js needed values
      this.source = -1;
      this.target = -1;
    }

    return Link;
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
    Link: Link,
    Util:Util,
    Neutron: Neutron
  };
});
