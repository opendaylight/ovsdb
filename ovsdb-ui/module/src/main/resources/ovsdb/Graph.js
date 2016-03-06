define(['app/ovsdb/lib/d3.min', 'app/ovsdb/OvsCore', 'app/ovsdb/matrix', 'underscore'], function(d3, OvsCore, Geom, _) {
  'use strict';

  var root = null,
    forceLayout = null,
    baseBB = null,
    nodes = null,
    links = null,
    nodeData = [],
    linkData = [],
    drag = null,
    nodePosCache = null;

  function Graph(id, width, height) {
    var x = d3.scale.linear()
      .domain([0, width])
      .range([0, width]);

    var y = d3.scale.linear()
      .domain([0, height])
      .range([height, 0]);

    var tmp = d3.select(id).append("svg")
    .attr('width', width)
    .attr('height', height)
    .append("svg:g")
    .attr('class', 'layer_0');

    tmp.append('svg:rect')
    .attr('width', width)
    .attr('height', height)
    .attr('fill', 'white');

    root = tmp.call(d3.behavior.zoom().x(x).y(y).scaleExtent([1, 6]).on("zoom", zoom))
      .append("g");

    this.matrix = new Geom.Matrix();

  //  this._bg = tmp.insert('svg:polygon', ':first-child');
  //  this._bg.attr('id', 'ttt').attr('fill', 'rgb(250, 220, 220)').attr('stroke', 'rgb(250,0,0)');

    drag = d3.behavior.drag()
      .origin(function(d) {
        return d;
      })
      .on("dragstart", dragstarted.bind(this))
      .on("drag", dragmove.bind(this))
      .on("dragend", dragend.bind(this));

    // a layout for the bridge and his link
    forceLayout = new d3.layout.force()
      .gravity(0.05)
      .charge(-400)
      .linkDistance(80)
      .size([width, height])
      .on('tick', this.update.bind(this));

    addDefs();
  }

  function zoom() {
    root.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
  }

  function dragstarted(d) {
    d3.event.sourceEvent.stopPropagation();
    forceLayout.stop();
  }

  function dragmove(d) {
    d.x += d3.event.dx;
    d.y += d3.event.dy;
    d.px += d3.event.dx;
    d.py += d3.event.dy;
    this.update();
  }

  function dragend(d) {
    d.fixed = true;
    this.update();
    forceLayout.resume();
    baseBB = null;
  }

  // Define reusable svg item like box-shadow
  function addDefs() {
    // box-shadow
    var defs = d3.select('svg').insert('svg:defs', ':first-child');
    var filter = defs.append('svg:filter').attr('id', 'selectNode').attr('x', '-20%').attr('y', '-20%').attr('width', '140%').attr('height', '140%');
    filter.append('feGaussianBlur').attr('stdDeviation', '2').attr('result', 'coloredBlur');
    var femerge = filter.append('feMerge');
    femerge.append('feMergeNode').attr('in', 'coloredBlur');
    femerge.append('feMergeNode').attr('in', 'SourceGraphic');
  }

  function sortLink(links) {
    links.sort(function(a,b) {
      if (a.source > b.source) {
        return 1;
      } else if (a.source < b.source) {
        return -1;
      }
      else {
        if (a.target > b.target) {
          return 1;
        }
        if (a.target < b.target) {
          return -1;
        } else {
          return 0;
        }
      }
    });
  }

  function setLinkIndexAndNum(links) {
    _.each(links, function(link, i) {
      if (i != 0 && links[i].source == links[i-1].source &&
        links[i].target == links[i-1].target) {
          links[i].linkindex = links[i-1].linkindex + 1;
        } else {
          links[i].linkindex = 1;
        }
    });
  }

  // Properties to quick access and set nodes and links
  Object.defineProperties(Graph.prototype, {
    links: {
      get: function() {
        return links;
      },
      set: function(value) {
        sortLink(value);
        setLinkIndexAndNum(value);

        forceLayout.links(value);
        links = root.selectAll('links')
          .data(value)
          .enter().append('svg:path')
          .attr('class', function(d) {
            return d.linkType;
          })
          .attr('fill', 'none')
          .attr('stroke-dasharray', function(d) {
            return d.dashArray;
          })
          .attr('stroke-width', function(d) {
            return d.width;
          })
          .attr('stroke', function(d) {
            return d.color;
          });
      }
    },
    nodes: {
      get: function() {
        return nodes;
      },
      set: function(value) {
        var _this = this; // context change in callback function and we need both

        forceLayout.nodes(value);
        nodes = root.selectAll('.nodes')
          .data(value)
          .enter().append('svg:g')
          .call(drag)
          .attr('class', function(d) {
            return (d.node instanceof OvsCore.BridgeNode) ? 'bridge' : 'switch';
          })
          .on('click', function(d) {
            if (d3.event.defaultPrevented) return;
            _this.onNodeClick(d, nodes, links, this);
          })
          .on("mouseover", function(d) {
            _this.onNodeOver(d, nodes, links, this);
          })
          .on("mouseout", function(d) {
            _this.onNodeOut(d, nodes, links, this);
          });

        nodes.append('text')
          .attr('x', 0)
          .attr('y', 30)
          .attr('fill', 'black')
          .attr('text-anchor', 'middle')
          .text(function(d) {
            if (d.node instanceof OvsCore.BridgeNode)
              return d.node.flowInfo.ip;
            else
              return d.node.otherLocalIp;
          });

        //svg node
        var layer = d3.selectAll('g.switch').append('svg:g').attr('class', 'switch').attr('transform', 'translate(-16 -16)');
        layer.append('svg:rect').style('fill-rule', 'evenodd').attr('ry', '3.6808').attr('height', '28.901')
          .attr('width', '28.784').style('stroke', '#002b69').attr('y', '0').attr('x', '0').style('stroke-width', "3px").style('fill', '#2a7fff');
        layer.append('svg:path').attr('d', 'm27.043 6.2-5.0754 3.3764-.01018-2.1082-5.9209-.022.01773-2.6018 5.9031-.037.08118-2.1164z').style('fill', "#002b69");
        layer.append('svg:path').attr('d', "m26.866 19.4-5.0754 3.3764-.01018-2.1082-5.9209-.022.01773-2.6018 5.9031-.037.08118-2.1164z").style('fill', "#002b69");
        layer.append('svg:path').attr('d', "m3.0872 11.6 5.0754 3.3764.01018-2.1082 5.9209-.022-.01773-2.6018-5.9031-.037-.08118-2.1164z").style('fill', "#002b69");
        layer.append('svg:path').attr('d', "m3.2639 24.8 5.0754 3.3764.01018-2.1082 5.9209-.022-.01773-2.6018-5.9031-.037-.08118-2.1164z").style('fill', "#002b69");

        //svg bridge
        var layer = d3.selectAll('g.bridge').append('svg:g').attr('transform', 'translate(-16 -16)');
        layer.append('svg:path').style('fill', '#d40000').style('stroke', '#ff0000').style('stroke-width', '0.10413094')
          .style('stroke-linecap', 'round') //stroke-linejoin:round
          .attr('d', 'm 2.9656662,3.4868 c 4.8978761,7.5117 16.2156478,6.1742 21.9929178,2.0807 l 2.154019,-1.5814 -0.08265,18.1941 -2.055088,2.0313 -24.17161713,0.055 -0.0255688,-18.6893 z');
        layer.append('svg:path').style('fill', '#ff5555').style('stroke', '#ff0000').style('stroke-width', '0.10413094')
          .style('stroke-linecap', 'round') //stroke-linejoin:round
          .attr('d', 'm 0.83642587,5.5637 c 4.89787603,7.5117 18.41115613,4.1109 24.18842613,0.018 l -0.06627,18.6546 -24.12215693,0 z');
      }
    }
  });

  // Update the node and link position on the canvas
  Graph.prototype.update = function(e) {
    var k = 1 , //.1 * e.alpha
      isDragging = (e !== null)
/*
    if (!isDragging) {
      k = .1 * e.alpha;
    }

    if (!isDragging) {
      forceLayout.nodes().forEach(function(o) {
        var i = (o.node instanceof OvsCore.OvsNode) ? 1 : 0;
        o.y += (layerAnchor[i].y - o.y) * k;
        o.x += (layerAnchor[i].x - o.x) * k;
      });
    }*/

    if (links) {
      links.attr('d', (function(d) {
        var srcT = this.matrix.transformPoint(d.source.x, d.source.y),
          targetT = this.matrix.transformPoint(d.target.x, d.target.y),
          src = {x:srcT.elements[0],y:srcT.elements[1]},
          tgt = {x:targetT.elements[0],y:targetT.elements[1]},
          anchor1 = {}, anchor2 = {};

        if (d.linkType === 'tunnel') {
          // curve the line and arc it on a direction of a 90 degree vector
          var perp = { x : -tgt.y, y: tgt.x};
          var srcNorm = Math.sqrt(src.x * src.x + src.y * src.y);
          var perpNorm = Math.sqrt(perp.x * perp.x + perp.y * perp.y);
          var dy = (perp.y/perpNorm - src.y/srcNorm);
          var dx = (perp.x/perpNorm - src.x/srcNorm);

          anchor1 = {x: src.x - dx * 30, y: src.y - dy * 30 };
          anchor2 = {x: tgt.x - dx * 30, y: tgt.y - dy * 30 };
        } else {
          // default strait line
          anchor1 = src;
          anchor2 = tgt;
        }

        return OvsCore.Util.String.Format('M{0},{1} C{2},{3} {4},{5} {6},{7}',
          srcT.elements[0], srcT.elements[1],
          anchor1.x, anchor1.y,
          anchor2.x, anchor2.y,
          targetT.elements[0], targetT.elements[1]
        );
      }).bind(this));
    }

    nodes.attr("transform", (function(d) {
      var a = new Geom.Matrix.fromString(root.attr('transform'));
      var tmp = Geom.Matrix.combine(a, this.matrix);

      var transV = this.matrix.transformPoint(d.x, d.y);
      var v = tmp.transformPoint(d.x, d.y);
      d.pos = {
        x: v.elements[0],
        y: v.elements[1]
      };
      nodePosCache[d.node.nodeId] =  { pos: d.pos, fixed: d.fixed};
      return "translate(" + transV.elements[0] + ',' + transV.elements[1] + ')';
    }).bind(this));
  };

  Graph.prototype.setPosCache = function(cache) {
    nodePosCache = cache;
  };

  // Apply few matrix transform to fake a perspective effet
  Graph.prototype.applyPerspective = function(value) {
    if (!baseBB)
      baseBB = root.node().getBBox();

    var padding = 50;
    var persMatrix = new Geom.Matrix()
      .translate(-(baseBB.x + baseBB.width / 2), -(baseBB.y + baseBB.height / 2))
      .skew(-25, 0)
      .scale(1, 0.6)
      .translate(baseBB.x + baseBB.width / 2, baseBB.y + baseBB.height / 2);

    this.matrix.transform = value ? this.matrix.transform.x(persMatrix.transform) : this.matrix.transform.x(persMatrix.transform.inverse());

    var p1 = this.matrix.transformPoint(baseBB.x - padding, baseBB.y - padding);
    var p2 = this.matrix.transformPoint(baseBB.x - padding, baseBB.y + baseBB.height + padding);
    var p3 = this.matrix.transformPoint(baseBB.x + baseBB.width + padding, baseBB.y + baseBB.height + padding);
    var p4 = this.matrix.transformPoint(baseBB.x + baseBB.width + padding, baseBB.y - padding);

    this._bg.attr('points', OvsCore.Util.String.Format("{0},{1} {2},{3} {4},{5} {6},{7}",
      p1.elements[0],
      p1.elements[1],
      //--
      p2.elements[0],
      p2.elements[1],
      //--
      p3.elements[0],
      p3.elements[1],
      //--
      p4.elements[0],
      p4.elements[1])).style('display', (value) ? 'block' : 'none');

    this.update();
  };

  function positionateBaseOnCache() {
    forceLayout.stop();
    forceLayout.nodes().forEach(function(d) {
      var nodeCached = nodePosCache[d.node.nodeId];
      d.px = d.x = nodeCached.pos.x;
      d.py = d.y = nodeCached.pos.y;
      d.fixed = nodeCached.fixed;
    });
    forceLayout.resume();
  }

  // start the force layout
  Graph.prototype.start = function() {
    forceLayout.linkStrength(function(link) {
      if (link.linkType === 'bridgeOvsLink') {
        return 0;
      }
      return 1;
    });
    forceLayout.start();
    if (_.size(nodePosCache) > 0) {
      positionateBaseOnCache();
    }
  };

  Graph.prototype.freeDOM = function() {
    nodes.remove();
    links.remove();
    forceLayout.nodes([]);
    forceLayout.links([]);
  };

  // Enable to manipulate attributes of the graph group node
  Graph.prototype.attr = function(name, value) {
    return root.attr(name, value);
  };

  // Enable the monipulate the css of the graph group node
  Graph.prototype.style = function(name, value) {
    return root.style(name, value);
  };

  Graph.prototype.selectAll = function(a) {
    return root.selectAll(a);
  };

  // callback function
  Graph.prototype.onNodeOver = _.noop;
  Graph.prototype.onNodeOut = _.noop;
  Graph.prototype.onNodeClick = _.noop;

  return Graph;
});
