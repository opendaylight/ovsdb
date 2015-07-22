/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/ovsdb/ovsdb.module', 'app/ovsdb/lib/d3.min', 'app/ovsdb/Graph', 'app/ovsdb/LogicalGraph','app/ovsdb/OvsCore', 'underscore', 'jquery', 'jquery-ui'], function (ovsdb, d3, Graph, LogicalGraph, OvsCore, _, $) {
  'use strict';

  ovsdb.register.directive('logicalGraph', function() {
    return {
      restrict: 'EA',
      scope: false,
      link : function (scope, elem, attr) {
        var lgraph = null,
        width = elem[0].clientWidth,
        height = 580;

        scope.lDialogData = {};

        scope.lgraphIsReadyPromise.then(function(ltopo) {
          if (!lgraph) {
            lgraph = new LogicalGraph(elem[0], width, height);
          }

          lgraph.networks = ltopo;

          lgraph.start();

          lgraph.onClick = function(e, d) {
            var dialogId = '#lDialog';
            scope.lDialogData = d.pretty();
            scope.$apply();

            var $dia = $(dialogId);
            $dia.css('left', e.left + 30);
            $dia.css('top', e.top + 35);
            $dia.show();

            $(dialogId).tabs();
            $(dialogId).draggable({
              containment: 'parent',
              cancel:'.window_content'
            });
          };

          lgraph.dblClick = function(d) {
            scope.goToPhysicalView(d);
          };
        });
        scope.hideLogicalDialog = function() {
            $('#lDialog').hide();
        };

      }
    };
  });

  ovsdb.register.directive('physicalGraph', function () {
    return {
      restrict: 'EA',
      scope: false,
      //templateUrl: 'src/app/ovsdb/views/graph_header.tpl.html',
      link: function (scope, ele, attr) {

        var graph = null,
          width = scope.canvasWidth, //ele[0].clientWidth,
          height = scope.canvasHeight;

        scope.setPhysicalView = function() {

        };

      /*  function rescale() {
          var trans = d3.event.translate,
            scale = d3.event.scale;

          vis.attr("transform",
            "translate(" + trans + ") " +
            "scale(" + scale + ") "
          );
        }*/

        scope.reset = function () {
          var transform = d3.transform(vis.attr('transform')),
            ix = d3.interpolate(x.domain(), [-width / 2, width / 2]),
            iy = d3.interpolate(y.domain(), [-height / 2, height / 2]),
            px = x.domain(ix(1)),
            py = y.domain(iy(1));

          vis.transition().duration(750).call(zoom.x(px).y(py).scale(1).event);
        };

        scope.dataPromise.then(function (topo) {
          graph = new Graph(ele[0], width, height);

          graph.links = _.values(topo.links);
          graph.nodes = _.map(topo.nodes, function (value) {
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
                return ((o.source.index == d.index || o.target.index == d.index) && o.linkType != 'tunnel')  ? "blue" : o.color;
            });
          };

          graph.onNodeOut = function (d, nodes, links) {
            nodes.selectAll('.switch > rect').style("stroke", "black");
            links.style("stroke", function(o) { return o.color; });
          };

          graph.onNodeClick = function (d, nodes, links, ctx) {
            /*var node = d3.select(ctx);
            d3.select('.node_selected').classed('node_selected', false).attr('filter', 'none');
            node.classed('node_selected', true).attr('filter', 'url(#selectNode)');*/
            scope.onNodeClick(d);
          };

        });

        scope.hidePhysicalDialog = function() {
            $('#pDialog').hide();
        };

        scope.rotateGraph = function (value) {
          var b = value ? 1 : -1;
          graphs.applyPerspective(value);
          graphs.update();
          $('path.tunnel').toggle();
        };

        scope.filterNode = function (group, b) {
          var opa = b ? 0 : 1;
          var eles = d3.selectAll("circle")
            .filter(function (d, i) {
              return d.group == group;
            });
          eles.transition().duration(200).style('opacity', opa);
        };
      }
    };
  });

  ovsdb.register.directive('windowOption', function () {
    return {
      restrit: 'A',
      scope: false,
      template: '<i data-ng-click="toggleSoftHide($event)" class="window-icon icon-minus"></i>' +
        '<i data-ng-click="toggleCollapse($event)" class="window-icon icon-collapse"></i>',
      link: function (scope, ele, attr) {
        var offset = 12; // to have a gaps between the resize icon and the soft hide icon
        var baseHeight = {}; // the directive is shared by instances. A hashmap will help to keep trace of each one

        scope.toggleCollapse = function (event) {
          var $div = $(event.target).parent().parent().parent();
          var h = $div.children('div.window_header').height();

          if (!baseHeight[$div.attr('id')] || baseHeight[$div.attr('id')] == -1) {
            baseHeight[$div.attr('id')] = $div.height();
          }

          if (h + offset < $div.height()) {
            $div.resizable("disable")
              .resizable("option", "minHeight", 0)
              .resizable("option", "minWidth", 0)
              .children('div.window_body').hide();
            $div.animate({
              'height': (h + offset) + 'px'
            }, 750);
          } else {
            $div.animate({
                'height': (baseHeight[$div.attr('id')] + offset) + 'px'
              }, 750)
              .resizable("enable")
              .resizable("option", "minHeight", 200)
              .resizable("option", "minWidth", 300)
              .children('div.window_body').show();
            baseHeight[$div.attr('id')] = -1;
          }
        };

        scope.toggleSoftHide = function (el) {
          console.log('soft hide');
        };
      }
    };
  });

  ovsdb.register.directive('nvWindow', function () {
    return {
      restrict: 'EA',
      scope: false,
      compile: function (ele, attr) {
        var resizable = attr['resizable'] === 'true';
        var draggable = !(attr['draggable'] === 'false');
        var noOption = !(attr['option'] === 'false');
        var childs = ele.children().detach();

        ele.addClass('graph_window window_resizable form-control');
        ele.append(angular.element('<div class="window_header"><label>' + (attr['title'] || '') + '</label>' + (noOption ? '<span window-option></span>' : '') + '<hr/></div>'));
        var parent = angular.element('<div class="window_body"></div>');
        parent.append(angular.element('<div class="window_content"></div>').append(childs));
        ele.append(parent);

        if (draggable) {
          $(ele).draggable({
            containment: 'parent',
            cancel:'.window_content'
          });
        }

        if (resizable) {
          $(ele).resizable();
        }
      }
    };
  });
});
