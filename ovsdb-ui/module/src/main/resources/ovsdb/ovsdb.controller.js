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
    // transparent 1px gif picture
    $rootScope['section_logo'] = 'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=';

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
      physDataDefer = $q.defer(),
      filterTenant = {
        bridgeIds : [''],
        ovsdbIds : ['']
      },
      filterSubnet = {
        bridgeIds : [''],
        ovsdbIds : ['']
      };

    $scope.dataPromise = physDataDefer.promise;
    $scope.lgraphIsReadyPromise = lgraphDataDefer.promise;
    $scope.canvasWidth = $('#tabs').width();
    $scope.canvasHeight = 580;

    $scope.dialogData = ['d'];

    $scope.tenants = [];
    $scope.subnets = [];
    $scope.selectedTenant = '';
    $scope.selectedSubnet = '';

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

    function applyFilter(inverse) {
      var bridgeIds = _.uniq(filterTenant.bridgeIds.concat(filterSubnet.bridgeIds));
      var ovsdbIds = _.uniq(filterTenant.ovsdbIds.concat(filterSubnet.ovsdbIds));
      $scope.filterNode(bridgeIds, '.bridge');
      $scope.filterNode(ovsdbIds, '.switch');
      $scope.filterLink();
    }

    function removeFilter() {
      $scope.filterNode([''], '.bridge', false);
      $scope.filterNode([''], '.switch', false);
      $scope.filterLink();
    }

    $scope.fiterByTenant = function() {
      if ($scope.selectedTenant) {
        var tenant = $scope.selectedTenant;
        OvsUtil.extractLogicalByTenant(tenant.id).then(function(result) {
          var bridgeId = result[0],
            ovsdbId = result[1];

          filterTenant.bridgeIds = _.uniq(filterTenant.bridgeIds.concat(bridgeId));
          filterTenant.ovsdbIds = _.uniq(filterTenant.ovsdbIds.concat(ovsdbId));
          applyFilter();
        });
      } else {
        filterTenant.bridgeIds = [''];
        filterTenant.ovsdbIds = [''];
        applyFilter();
      }
    };

    $scope.filterBySubnet = function() {
      if (!_.isEmpty($scope.selectedSubnet)) {
        var subnets = _.map($scope.selectedSubnet, function(d) {
          return d.id;
        });
        OvsUtil.extractLogicalBySubnet(subnets).then(function(result) {
          var bridgeId = result[0],
            ovsdbId = result[1];
            filterSubnet.bridgeIds = _.uniq(filterSubnet.bridgeIds.concat(bridgeId));
            filterSubnet.ovsdbIds = _.uniq(filterSubnet.ovsdbIds.concat(ovsdbId));
            applyFilter();
        });
      } else {
        filterSubnet.bridgeIds = [''];
        filterSubnet.ovsdbIds = [''];
        applyFilter();
      }
    };

    $scope.onNodeClick = function (d, nodes, links) {
      $scope.pDialogData = d.node.pretty();
      $scope.$apply();
    };

    $('#tenantSelect').select2({
      width: "200",
      minimumResultsForSearch: Infinity
    }).next().children('span').children('span').css('width', '200'); //hack to have the arrow with the same background

    $("#tagPicker").select2({
      width: "230",
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
