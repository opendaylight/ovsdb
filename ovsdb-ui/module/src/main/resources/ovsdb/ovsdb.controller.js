/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['jquery', 'underscore', 'app/ovsdb/ovsdb.module', 'app/ovsdb/OvsCore', 'app/ovsdb/ovsdb.directives', 'app/ovsdb/ovsdb.services', 'app/ovsdb/lib/select2.full.min'], function ($, _, ovsdb, OvsCore) {
  'use strict';

  var RootOvsdbCtrl = function ($rootScope, cssInjector) {
    $rootScope['section_logo'] = 'logo_ovsdb';
    cssInjector.add('src/app/ovsdb/css/select2.min.css');
    cssInjector.add('src/app/ovsdb/css/toggle-switch.css');

    cssInjector.add('src/app/ovsdb/css/ovsdb.css');
  };
  RootOvsdbCtrl.$inject = ['$rootScope', 'cssInjector'];

  var BaseOvsdbCtrl = function ($scope) {
    $scope.err = {
      "message": "",
      "tag": "",
      "type": ""
    };
    $scope.showError = function () {
      $('#errorMessage').fadeIn().delay(3000).fadeOut();
    };
  };
  BaseOvsdbCtrl.$inject = ['$scope'];

  var OvsdbCtrl = function ($q, $scope, TopologySvc, NeutronSvc, OvsUtil) {
    BaseOvsdbCtrl.call(this, $scope);
    var lgraphDataDefer = $q.defer(),
      physDataDefer = $q.defer();

    $scope.dataPromise = physDataDefer.promise;
    $scope.lgraphIsReadyPromise = lgraphDataDefer.promise;
    $scope.canvasWidth = $('#tabs').width();
    $scope.canvasHeight = 580;

    $scope.tenants = [
      {id : '55', name: 'ppp'},
      {id : '3', name: 'ttt'}
    ];

    $scope.subnets = [];
    $scope.selectedTenant = '0';
    $scope.selectedSubnet = '';
    $scope.nodeInfo = [];

    $scope.networkInfo = [
      { key: 'Uuid', value: '123'},
      { key: 'IP', value: '10.0.0.1'},
      { key: 'Netmask', value: '255.255.255.0'},
      { key: 'OpenFlow Version', value: '1.3'}
    ];

    $scope.toggleLayer = function () {
      $scope.rotateGraph($scope.opt.layer);
    };

    $scope.resizeGraph = function () {
      var $row = $('#ovsdb_contain > div.row:first');
      var h = $row.height();
      $row.data('ph', h);
      $row.fadeOut();
      $('#nv_graph > svg').animate({
        height: '+=' + h
      });
    };

    $scope.minimizeGraph = function () {
      var $row = $('#ovsdb_contain > div.row:first');
      var h = $row.data('ph');
      $row.fadeIn();
      $('#nv_graph > svg').animate({
        height: '-=' + h
      });
    };

    $scope.fiterByTenant = function() {
      var tenant= $scope.selectedTenant;
      OvsUtil.extractLogicalByTenant(tenant.id).then(function(result) {

      });
    };

    $scope.filterBySubnet = function() {
      console.log($scope.selectedSubnet);
    };

    $scope.onNodeClick = function (d, nodes, links) {
      var dialogId = '#pDialog';
      $scope.pDialogData = d.node.pretty();
      $scope.$apply();

      var $dia = $(dialogId);
      $dia.css('left', /*e.left + */30);
      $dia.css('top', /*e.top + */35);
      $dia.show();

      $(dialogId).tabs();
      $(dialogId).draggable({
        containment: 'parent',
        cancel:'.window_content'
      });

      $scope.$apply();
    };

    $scope.toggleUnderlay = function () {
      console.log($scope.opt.underlay);
      $scope.filterNode(1, $scope.opt.underlay);
    };

    $('#tenantSelect').select2({
      width: "200",
      minimumResultsForSearch: Infinity
    }).next().children('span').children('span').css('width', '200'); //hack to have the arrow with the same background

    $("#tagPicker").select2({
      width: "170",
    });

    var $tabs = $('#tabs').tabs({selected: 0});

    $scope.goToPhysicalView = function(d) {
      $tabs.tabs("option", "active", 1);
      $('#tenantSelect').val(d.tenantId).change();

      if ( d instanceof OvsCore.Neutron.Network) {
        $('#tagPicker').val(d.subnets.map(function(d){return d.id;})).change();
      }
    };

    OvsUtil.getLogicalTopology().then(function(networks) {
      var tenantList = NeutronSvc.getAllTenants();
      _.each(tenantList, function(t) {
        $scope.tenants.push({id : t, name: t});
      });
      NeutronSvc.getSubNets().then(function(subHash) {
        $scope.subnets = _.values(subHash).map(function(n) { return n[0]; });
      });

      lgraphDataDefer.resolve(networks);
    });

    TopologySvc.getTopologies().then(function(d) {
      physDataDefer.resolve(d);
    });

  };

  OvsdbCtrl.$inject = ['$q', '$scope', 'TopologySvc', 'NeutronSvc', 'OvsUtil'];
  OvsdbCtrl.prototype = Object.create(BaseOvsdbCtrl.prototype);

  ovsdb.register.controller('RootOvsdbCtrl', RootOvsdbCtrl);
  ovsdb.register.controller('OvsdbCtrl', OvsdbCtrl);
});
