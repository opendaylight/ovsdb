define(['app/ovsdb/lib/d3.min', 'app/ovsdb/OvsCore', 'underscore'], function (d3, OvsCore, _) {
  'use strict';

  var root = null,
    canvasWidth = -1,
    canvasHeight = -1,
    bbox = {
      x: 0,
      y: 15,
      width: 0,
      height: 0
    },
    // config
    nodeWidth = 15,
    nodeHeight = -1,
    defaultRouterWidth = 52,
    defaultRouterHeight = 52,
    networkMargin = {
      width: 120,
      height: 15
    },
    routerMargin = {
      width: 120,
      height: 40
    },
    vmMargin = {
      width: 90,
      height: 30
    },
    defaultVmsWidth = 48,
    defaultVmsHeight = 48,
    ipNetworkTextMaxLength = 60,
    networkOffset = 15,
    linkHeight = 5,
    // datas
    networkData = [],
    routerData = [],
    vmData = [],
    linkData = [],
    tmpNetHolder = {},
    // d3 layer over datas
    d3Node = null,
    d3Link = null,
    d3Vm = null,
    d3Router = null,
    randomize = OvsCore.Util.Math.Random(42);

  function LogicalGraph(id, width, height) {
    canvasWidth = width;
    canvasHeight = height;

    nodeHeight = height - 15;

    var tmp = d3.select(id).append("svg")
      .attr('width', width)
      .attr('height', height)
      .append("svg:g")
      .attr('class', 'layer_0');

    tmp.append('svg:rect')
      .attr('width', width)
      .attr('height', height)
      .attr('fill', 'white');

    root = tmp.call(d3.behavior.zoom().scaleExtent([1, 8]).on("zoom", zoom))
      .append("g");
    tmp.on("dblclick.zoom", null);
    addDefs();
  }

  // Define reusable svg item like box-shadow
  function addDefs() {
    // box-shadow
    var defs = d3.select('svg').insert('svg:defs', ':first-child');
    var filter = defs.append('svg:filter').attr('id', 'boxShadow').attr('x', '0').attr('y', '0').attr('width', '200%').attr('height', '200%');
    filter.append('feOffset').attr('in', 'SourceAlpha').attr('result', 'offOut').attr('dx', 0).attr('dy', 0);
    filter.append('feGaussianBlur').attr('stdDeviation', '5').attr('in', 'offOut').attr('result', 'blurOut');
    filter.append('feOffset').attr('in', 'SourceGraphic').attr('in2', 'blurOut').attr('mode', 'normal');
  }

  function zoom() {
    root.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
  }

  Object.defineProperties(LogicalGraph.prototype, {
    networks: {
      set: function (value) {
        networkData = value;
        value.forEach(function (net) {
          routerData = routerData.concat(net.routers);
        });
        value.forEach(function (net) {
          vmData = vmData.concat(net.instances);
        });
      }
    }
  });

  LogicalGraph.prototype.start = function () {
    setTopologyPosition.call(this, networkData);
    addLinksToDom.call(this, linkData);
    addNetWorkRouterVmsToDom.call(this, networkData, routerData, vmData);
    update.call(this);
  };

  LogicalGraph.prototype.freeDOM = function () {
    d3Node.remove();
    d3Link.remove();
    d3Vm.remove();
    d3Router.remove();

    networkData = [];
    routerData = [];
    vmData = [];
    linkData = [];
    bbox = {
      x: 0,
      y: 15,
      width: 0,
      height: 0
    };
  };

  function addLinksToDom(linksData) {
    d3Link = root.selectAll('.llink')
      .data(linksData).enter().append('svg:g');

    d3Link.append('rect')
      .attr('width', function (d) {
        return d.target.x - d.source.x;
      })
      .attr('height', linkHeight)
      .style('fill', function (d) {
        return d.color;
      });

    d3Link.append('text')
      .attr('x', 40)
      .attr('y', -3)
      .text(function (d) {
        return d.text;
      });
  }

  function addNetWorkRouterVmsToDom(networks, routers, vms) {
    var ctx = this,
      timer = null;

    d3Node = root.selectAll('.network')
      .data(networks).enter()
      .append('svg:g');
    d3Router = root.selectAll('.routers')
      .data(routers).enter()
      .append('svg:g');
    d3Vm = root.selectAll('.vm')
      .data(vms).enter()
      .append('svg:g');

    // append coresponding form
    d3Node.append('svg:rect')
      .attr('width', nodeWidth)
      .attr('height', nodeHeight)
      .attr('rx', 10)
      .attr('ry', 10)
      .style('fill', function (d) {
        return d.color;
      }).on('click', function (d) {
        if (d3.event.defaultPrevented) return;
        timer = setTimeout(function () {
          ctx.onClick(d);
        }.bind(this), 150);
      }).on('dblclick', function (d) {
        clearTimeout(timer);
        ctx.dblClick(d);
      });

    // append the network name text
    d3Node.append('text')
      .attr('x', nodeWidth / 2)
      .attr('y', nodeHeight / 2)
      .style('text-anchor', 'middle')
      .style('writing-mode', 'tb')
      .style('font-size', '12px')
      .style('glyph-orientation-vertical', '0')
      .text(function (d) {
        return d.name;
      });

    // text info for the network ip
    d3Node.append('text')
      .attr('x', nodeWidth + 10)
      .attr('y', nodeHeight - 15)
      .attr('transform',
        OvsCore.Util.String.Format('translate({0} {1}) rotate(-90) translate(-{0} -{1})', nodeWidth + 10, nodeHeight - 15))
      .attr('class', 'linfolabel')
      .text(function (d) {
        return d.ip;
      });

    // vm
    d3Vm.append('svg:image')
      .attr('width', defaultVmsWidth)
      .attr('height', defaultVmsHeight)
      .attr('filter', 'url(#boxShadow)')
      .attr('xlink:href', function (d) {
        return d.type === 'network:dhcp' ?
          'src/app/ovsdb/assets/dhcp.png' : 'src/app/ovsdb/assets/vm.png';
      })
      .on('click', function (d) {
        if (d3.event.defaultPrevented) return;
        timer = setTimeout(function () {
          ctx.onClick(d);
        }.bind(this), 150);
      }).on('dblclick', function (d) {
        clearTimeout(timer);
        ctx.dblClick(d);
      });

    // router
    d3Router.append('svg:image')
      .attr('width', defaultRouterWidth)
      .attr('height', defaultRouterHeight)
      .attr('xlink:href', 'src/app/ovsdb/assets/router.png')
      .on('click', function (d) {
        if (d3.event.defaultPrevented) return;
        timer = setTimeout(function () {
          ctx.onClick(d);
        }.bind(this), 150);
      }).on('dblclick', function (d) {
        clearTimeout(timer);
        ctx.dblClick(d);
      });

    // router name label
    d3Router.append('text')
      .attr('x', defaultRouterWidth * 0.5)
      .attr('y', defaultRouterHeight + 15)
      .attr('text-anchor', 'middle')
      .attr('class', 'linfolabel')
      .text(function (d) {
        return d.name;
      });

    // vm name label
    d3Vm.append('text')
      .attr('x', defaultVmsWidth * 0.5)
      .attr('y', defaultVmsHeight + 15)
      .attr('text-anchor', 'middle')
      .attr('class', 'linfolabel')
      .text(function (d) {
        return d.name;
      });

    // vm floating ip label
    d3Vm.append('text')
      .attr('x', -35)
      .attr('y', 40)
      .attr('text-anchor', 'middle')
      .text(function (d) {
        return (d.floatingIp) ? d.floatingIp.ip : '';
      });

  }

  function findNetworkWithRouter(router) {
    var result = [];
    _.each(router.interfaces, function (inter) {
      if (inter.type === 'router_interface') {
        var net = tmpNetHolder[inter.networkId] || null;

        if (net) {
          result.push({
            network: net,
            interface: inter
          });
        }
      }
    });

    return result;
  }

  function positionateNetwork(network, x, y, margin) {
    network.x = x;
    network.y = y;
    margin = margin || 0;
    network.color = d3.hsl(randomize.next() * 360, 1, 0.6).toString();

    // look is the network is the highest
    bbox.height = network.y > bbox.height ? network.y : bbox.height;
    bbox.width = network.x > bbox.width ? network.x : bbox.width;

    // get the number of "childs" (router, vm)
    var nbRouter = network.routers.length;
    var nbVm = network.instances.length;

    if (!network.external) {
      _.each(network.subnets, function (subnet, i) {
        network.ip += subnet.cidr;
        if (i < network.subnets.length - 1) {
          network.ip += ', ';
        }
      });
    }

    // if needed, ajust the height of the network
    // to be able to display all children
    ajustHeighBaseOnChilds(nbRouter, nbVm);

    var py = positionateRouter(network, x + routerMargin.width, y + margin);

    positionateVm(network, x + vmMargin.width, py + 35 + margin);
    delete tmpNetHolder[network.id];
  }

  function positionateRouter(network, x, y) {
    var px = x,
      py = y;

    // loop over all routers
    _.each(network.routers, function (router, i) {
      router.x = getRouterCentroid(x, py).x;
      router.y = py;
      py += getRouterMarginHeight();

      if (network.external) {
        // find network ip with the gateway ip
        var gateway = router.externalGateway.external_fixed_ips[0].ip_address;
        var netIp = gateway.slice(0, gateway.lastIndexOf('.')) + '.0';
        network.ip = netIp;
      }

      // look is the router is the highest
      bbox.height = router.y > bbox.height ? router.y : bbox.height;
      bbox.width = router.x > bbox.width ? router.x : bbox.width;

      linkData.push({
        source: {
          x: network.x + (nodeWidth * 0.5),
          y: router.y + (defaultRouterHeight * 0.5)
        },
        target: {
          x: router.x + (defaultRouterWidth * 0.5),
          y: router.y + (nodeWidth * 0.5)
        },
        color: network.color,
        text: router.externalGateway.external_fixed_ips[0].ip_address
      });

      // go to the next layer
      var nets = findNetworkWithRouter(router),
        step = defaultRouterHeight / (nets.length + 1);

      _.forEach(nets, function (net, i) {
        var netPos = getNetworkLayerPosition(bbox.width + defaultRouterWidth);

        positionateNetwork(net.network, netPos.x, netPos.y);
        linkData.push({
          source: {
            x: router.x + (2 * nodeWidth),
            y: router.y + step * (i + 1)
          },
          target: {
            x: net.network.x + (nodeWidth * 0.5),
            y: router.y + (nodeWidth * 0.5)
          },
          color: net.network.color,
          text: net.interface.ip.ip_address
        });
      });
    });
    return py;
  }

  function positionateVm(network, x, y) {

    // I do vm before router because router
    // will step to another BUS
    _.each(network.instances, function (vm) {
      vm.x = x;
      vm.y = y;

      // look is the network is the highest
      bbox.height = vm.y > bbox.height ? vm.y : bbox.height;
      bbox.width = vm.x > bbox.width ? vm.x : bbox.width;

      y += getVmMarginHeight();
      linkData.push({
        source: {
          x: network.x + (nodeWidth * 0.5),
          y: vm.y + (defaultVmsHeight * 0.5)
        },
        target: {
          x: vm.x + (defaultVmsWidth * 0.5),
          y: vm.y + (nodeWidth * 0.5)
        },
        color: network.color,
        text: vm.ip
      });
    });
  }

  /*
   *  Scan the whole "BUS" to display it properly
   * ------------------------------------------------
   *  I build it in a virtual space, if it need to be
   *  resize it at the end when the overal bounding
   *  box is known
   */
  function setTopologyPosition(networks) {
    _.each(networks, function (net) {
      tmpNetHolder[net.id] = net;
    });

    var i = 0;
    for (var key in tmpNetHolder) {
      var margin = (i === 0) ? 5 : networkMargin.width,
        net = tmpNetHolder[key];
      if (net.routers.length > 0) {
        positionateNetwork(net, bbox.x + bbox.width + margin, bbox.y);
        ++i;
      }
    }

    for (var key in tmpNetHolder) {
      var margin = networkMargin.width,
        net = tmpNetHolder[key];
      positionateNetwork(net, bbox.x + bbox.width + margin, bbox.y);
    }
  }

  /*
   * Check and ajust the height for a network.
   */
  function ajustHeighBaseOnChilds(nbRouter, nbVm) {
    // calculate the height for the number of childs
    var childHeight = nbRouter * (getRouterMarginHeight()) +
      nbVm * (getVmMarginHeight()) + ipNetworkTextMaxLength;

    // if heigh bigger than the default network height resize it
    if (childHeight > nodeHeight) {
      nodeHeight = childHeight + networkOffset;
    }
  }

  /*
   * Set the view to the modal position
   */
  function update() {

    d3Node.attr('transform', function (d) {
      return OvsCore.Util.String.Format("translate({0}, {1})",
        d.x, d.y
      );
    });

    d3Router.attr('transform', function (d, i) {
      return OvsCore.Util.String.Format("translate({0}, {1})",
        d.x, d.y
      );
    });

    d3Vm.attr('transform', function (d) {
      return OvsCore.Util.String.Format("translate({0}, {1})",
        d.x, d.y
      );
    });

    d3Link.attr('transform', function (d) {
      return OvsCore.Util.String.Format("translate({0}, {1})",
        d.source.x, d.source.y
      );
    });

    // resize the graph if bigger than the canvas
    var bbox = root.node().getBBox();
    if (bbox.width > canvasWidth || bbox.height > canvasHeight) {
      var sx = (canvasWidth - 30) / bbox.width,
        sy = (canvasHeight - 30) / bbox.height,
        s = sx < sy ? sx : sy;
      d3.select('.layer_0').attr('transform', 'scale(' + s + ')');
      console.log(root.node().getBBox());
    }
  }

  function getRouterCentroid(x, y) {
    return {
      x: x + defaultRouterWidth * 0.5,
      y: y + defaultRouterHeight * 0.5
    };
  }

  function getRouterMarginHeight() {
    return (defaultRouterHeight + routerMargin.height);
  }

  function getVmMarginHeight() {
    return (defaultVmsHeight + vmMargin.height);
  }

  function getNetworkLayerPosition(x) {
    return {
      x: x + networkMargin.width,
      y: networkMargin.height
    };
  }

  function getVmLayerPosition(nbRouter, x) {
    var t = {
      x: x + vmMargin.width * 2,
      y: getRoutersDim(nbRouter).height + getVmMarginHeight()
    };
    return t;
  }

  LogicalGraph.prototype.onClick = _.noop;
  LogicalGraph.prototype.dblClick = _.noop;

  return LogicalGraph;
});
