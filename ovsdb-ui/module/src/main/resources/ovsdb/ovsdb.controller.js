/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['jquery', 'app/ovsdb/ovsdb.module','app/ovsdb/ovsdb.services'], function($, ovsdb) {
  'use strict';

  var RootOvsdbCtrl = function($rootScope) {
      $rootScope['section_logo'] = 'logo_ovsdb';
  };
  RootOvsdbCtrl.$inject = ['$rootScope'];

  var BaseOvsdbCtrl = function($scope) {
      $scope['err'] = {
        "message": "",
        "tag": "",
        "type": ""
      };

      $scope.showError = function() {
        $('#errorMessage').fadeIn().delay(3000).fadeOut();
      };
  };
  BaseOvsdbCtrl.$inject = ['$scope'];

  var OvsdbCtrl = function($scope, TopologyNetworkSvc) {
      BaseOvsdbCtrl.call(this, $scope);

  };
  OvsdbCtrl.$inject = ['$scope', 'TopologyNetworkSvc'];
  OvsdbCtrl.prototype = Object.create(BaseOvsdbCtrl.prototype);

  ovsdb.register.controller('RootOvsdbCtrl', RootOvsdbCtrl);
  ovsdb.register.controller('OvsdbCtrl', OvsdbCtrl);
});
