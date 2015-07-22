/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['jquery', 'app/ovsdb/ovsdb.module', 'app/ovsdb/ovsdb.directives', 'app/ovsdb/ovsdb.services', 'app/ovsdb/lib/select2.full.min'], function ($, ovsdb) {
  'use strict';

  function Command(key, desc) {
    this.key = key;
    this.desc = desc;
  }

  function ResetCommand(key, desc, scope) {
    Command.call(this, key, desc);
    this._scope = scope;
  }

  ResetCommand.prototype.react = function (keychar, keyEvent) {
    if (keychar === 'r') {
      this._scope.reset();
    }
  };

  function HelpCommand(key, desc) {
    Command.call(this, key, desc);
  }

  HelpCommand.prototype.react = function (keychar, keyEvent) {
    if (keychar === '/') {
      $('#shortcut_helper').toggle();
    }
  };

  function HideCommand(key, desc) {
    Command.call(this, key, desc);
  }

  HideCommand.prototype.react = function (keychar, keyEvent) {
    if (keychar === 'h') {
      $('.graph_window').toggle();
    }
  };

  var RootOvsdbCtrl = function ($rootScope, cssInjector) {
    $rootScope['section_logo'] = 'logo_ovsdb';
   /* cssInjector.add('src/app/ovsdb/css/select2.min.css');
    cssInjector.add('src/app/ovsdb/css/toggle-switch.css');
    cssInjector.add('src/app/ovsdb/css/ovsdb.css'); */
  };

  RootOvsdbCtrl.$inject = ['$rootScope', 'cssInjector'];

  var BaseOvsdbCtrl = function ($scope) {
    $scope['err'] = {
      "message": "",
      "tag": "",
      "type": ""
    };
    $scope.showError = function () {
      $('#errorMessage').fadeIn().delay(3000).fadeOut();
    };
  };
  BaseOvsdbCtrl.$inject = ['$scope'];

  var OvsdbCtrl = function ($q, $scope, TopologySvc, NeutronSvc) {
    BaseOvsdbCtrl.call(this, $scope);
    var dataDeffered = $q.defer();

    $scope.lstKeyCommand = [
      new ResetCommand('r', 'Reset the position and scale of the graph', $scope),
      new HelpCommand('/', 'Show the shortcup list'),
      new HideCommand('h', 'Hide all windows')
    ];

    $scope['networks'];
    NeutronSvc.getNetworks(function(data){
      $scope['networks'] = data;
    });

    $scope.dataPromise = dataDeffered.promise;

    $scope.opt = {
      intent: 0,
      network: null,
      layer: false,
      underlay: false
    };

    $scope.nodeInfo = [];

    $scope.networkInfo = [
      { key: 'Uuid', value: '123'},
      { key: 'IP', value: '10.0.0.1'},
      { key: 'Netmask', value: '255.255.255.0'},
      { key: 'OpenFlow Version', value: '1.3'}
    ];
    
    $scope.toggleLayer = function () {
      $scope.rotateGraph($scope.opt.layer);
    }

    $scope.resizeGraph = function () {
      var $row = $('#ovsdb_contain > div.row:first');
      var h = $row.height();
      $row.data('ph', h);
      $row.fadeOut();
      $('#nv_graph > svg').animate({
        height: '+=' + h
      });
    }

    $scope.minimizeGraph = function () {
      var $row = $('#ovsdb_contain > div.row:first');
      var h = $row.data('ph');
      $row.fadeIn();
      $('#nv_graph > svg').animate({
        height: '-=' + h
      });
    }

    $scope.onNodeClick = function (d, nodes, links) {
      $scope.nodeInfo = d.node.pretty();
      $scope.$apply();
    };

    $scope.toggleUnderlay = function () {
      console.log($scope.opt.underlay);
      $scope.filterNode(1, $scope.opt.underlay);
    }

    window.addEventListener('keypress', function (keyEvent) {
      var key = event.keyCode || event.which;
      var keychar = String.fromCharCode(key);
      $scope.lstKeyCommand.forEach(function (elem) {
        elem.react(keychar, keyEvent);
      });
    });

    $('#tenantSelect').select2({
      width: "120",
      minimumResultsForSearch: Infinity
    }).next().children('span').children('span').css('width', '120'); //hack to have the arrow with the same background

    $("#tagPicker").select2({
      width: "170",
    });

    TopologySvc.getTopologies(function (topologies) {
      dataDeffered.resolve(topologies);
    });
  };

  OvsdbCtrl.$inject = ['$q', '$scope', 'TopologySvc', 'NeutronSvc'];
  OvsdbCtrl.prototype = Object.create(BaseOvsdbCtrl.prototype);

  ovsdb.register.controller('RootOvsdbCtrl', RootOvsdbCtrl);
  ovsdb.register.controller('OvsdbCtrl', OvsdbCtrl);
});
