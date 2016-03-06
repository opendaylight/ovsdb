/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/ovsdb/ovsdb.module', 'app/ovsdb/lib/d3.min', 'app/ovsdb/Graph', 'app/ovsdb/LogicalGraph', 'app/ovsdb/OvsCore', 'underscore', 'jquery', 'jquery-ui'], function (ovsdb, d3, Graph, LogicalGraph, OvsCore, _, $) {
  'use strict';

  ovsdb.register.directive('logicalGraph', function () {
    return {
      restrict: 'EA',
      scope: false,
      link: function (scope, elem, attr) {
        var lgraph = null,
          tabCreated = false,
          width = scope.canvasWidth, //ele[0].clientWidth,
          height = scope.canvasHeight;

        scope.lDialogData = {};

        scope.lgraphIsReadyPromise.then(function (ltopo) {
          if (!lgraph) {
            lgraph = new LogicalGraph(elem[0], width, height);
          }

          lgraph.networks = ltopo;

          lgraph.start();

          lgraph.onClick = function (d) {
            var dialogId = '#lDialog';
            scope.lDialogData = d.pretty();
            scope.$apply();

            if (!tabCreated) {
              $(dialogId).tabs();
              $(dialogId).draggable({
                containment: 'parent',
                cancel: '.window_content'
              });
              tabCreated = true;
            } else {
              $(dialogId).tabs('refresh');
            }

            var $dia = $(dialogId),
              left = $dia.css('left'),
              top = $dia.css('top') || e.top + 35;
            $dia.css('left', left !== 'auto' ? left : 10);
            $dia.css('top', top !== 'auto' ? top : 10);
            $dia.show();

          };

          lgraph.dblClick = function (d) {
            scope.goToPhysicalView(d);
          };
        });
        scope.hideLogicalDialog = function () {
          $('#lDialog').tabs("option", "active", 0)
            .hide();
        };
        elem.on('$destroy', function () {
          if (lgraph) {
            lgraph.freeDOM();
          }
        });
      }
    };
  });

  ovsdb.register.directive('physicalGraph', function (CacheFactory) {
    return {
      restrict: 'EA',
      scope: false,
      //templateUrl: 'src/app/ovsdb/views/graph_header.tpl.html',
      link: function (scope, ele, attr) {

        var graph = null,
          tabCreated = false,
          width = scope.canvasWidth,
          height = scope.canvasHeight;

        scope.reset = function () {
          var transform = d3.transform(vis.attr('transform')),
            ix = d3.interpolate(x.domain(), [-width / 2, width / 2]),
            iy = d3.interpolate(y.domain(), [-height / 2, height / 2]),
            px = x.domain(ix(1)),
            py = y.domain(iy(1));

          vis.transition().duration(750).call(zoom.x(px).y(py).scale(1).event);
        };

        scope.dataPromise.then(function (topo) {
          if (!graph) {
            console.log('physical graph created');
            graph = new Graph(ele[0], width, height);
          }
          var nodes = _.clone(topo.nodes);
          var links = _.clone(topo.links);

          graph.setPosCache(CacheFactory.getCacheObj('nodePos'));

          graph.links = _.values(links);
          graph.nodes = _.map(nodes, function (value) {
            return {
              node: value
            };
          });
          graph.start();

          scope.nbOpenFlowSwitch = _.size(topo.bridgeNodes);
          scope.nbOvsNode = _.size(topo.ovsdbNodes);

          var linkedByIndex = {};
          _.each(topo.flowLinks, function (d) {
            linkedByIndex[d.source + ',' + d.target] = true;
          });

          function isConnected(a, b) {
            return linkedByIndex[a.index + "," + b.index] || linkedByIndex[b.index + "," + a.index] || a.index == b.index;
          }

          function hasConnections(a) {
            for (var property in linkedByIndex) {
              var s = property.split(",");
              if ((s[0] == a.index || s[1] == a.index) && linkedByIndex[property])
                return true;
            }
            return false;
          }

          graph.onNodeOver = function (d, nodes, links) {
            nodes.selectAll('.switch > rect').style("stroke", function (o) {
              return isConnected(d, o) ? "blue" : "black";
            });

            links.style("stroke", function (o) {
              return ((o.source.index == d.index || o.target.index == d.index) && o.linkType != 'tunnel') ? "blue" : o.color;
            });
          };

          graph.onNodeOut = function (d, nodes, links) {
            nodes.selectAll('.switch > rect').style("stroke", "black");
            links.style("stroke", function (o) {
              return o.color;
            });
          };

          graph.onNodeClick = function (d, nodes, links, ctx) {
            /*var node = d3.select(ctx);
            d3.select('.node_selected').classed('node_selected', false).attr('filter', 'none');
            node.classed('node_selected', true).attr('filter', 'url(#selectNode)');*/
            scope.onNodeClick(d);
            var dialogId = '#pDialog',
              $dia = $(dialogId);

            if (!tabCreated) {
              $(dialogId).tabs();
              $(dialogId).draggable({
                containment: 'parent',
                cancel: '.window_content'
              });
              tabCreated = true;
            } else {
              $(dialogId).tabs('refresh');
            }

            $dia.css('left', /*e.left + */ 30);
            $dia.css('top', /*e.top + */ 35);
            $dia.show();
          };

        });

        ele.on('$destroy', function () {
          graph.freeDOM();
        });

        scope.hidePhysicalDialog = function () {
          $('#pDialog').tabs("option", "active", 0)
            .hide();
        };

        scope.rotateGraph = function (value) {
          var b = value ? 1 : -1;
          graphs.applyPerspective(value);
          graphs.update();
          $('path.tunnel').toggle();
        };

        scope.filterNode = function (nodeIds, tags, exclude) {
          exclude = (exclude === null) ? true : exclude;
          var nodes = d3.selectAll(tags);
          nodes.each(function (d) {
            if (nodeIds.indexOf(d.node.nodeId) < 0) {
              d.hidden = exclude;
            } else {
              d.hidden = !exclude;
            }
          });
          nodes.transition().duration(200).style('opacity', function (d) {
            return d.hidden ? '0.3' : '1';
          });

        };

        scope.filterLink = function () {
          var links = d3.selectAll(".tunnel, .link, .bridgeOvsLink");

          links.each(function (d, i) {
            d.hidden = d.source.hidden || d.target.hidden;
          });
          links.transition().duration(200).style('opacity', function (d) {
            return d.hidden ? '0.3' : '1';
          });
        };
      }
    };
  });

});
