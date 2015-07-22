/*
 * Copyright (c) 2015 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['angularAMD', 'app/routingConfig', 'Restangular', 'angular-translate', 'angular-translate-loader-static-files', 'app/core/core.services', 'common/config/env.module'], function(ng) {
  'use strict';

  var ovsdb = angular.module('app.ovsdb', ['app.core', 'pascalprecht.translate', 'ui.router.state', 'restangular', 'config']);
  ovsdb.register = ovsdb; // for unit test

  // Filter to access neutron opendaylight.
  // This factory need to be to avoid circular dependencies.
  ovsdb.factory('NeutronInterceptor', ['$q', '$window', 'Base64', function($q, $window, Base64) {
    return {
      request : function(config) {
          // Use AAA basic authentication
        if (config.url.indexOf('controller/nb/v2') != -1) {
          config.headers = config.headers || {};
          if ($window.sessionStorage.odlUser && $window.sessionStorage.odlPass) {
            var encoded = Base64.encode($window.sessionStorage.odlUser + ':' + $window.sessionStorage.odlPass);
            config.headers.Authorization = 'Basic ' + encoded;
          }
        }
        return config;
      },
      response : function(response) {
        return response || $q.when(response);
      }
    };
  }]);

  ovsdb.config(function($stateProvider, $compileProvider, $controllerProvider, $provide, $httpProvider, NavHelperProvider) {
    ovsdb.register = {
      controller : $controllerProvider.register,
      directive : $compileProvider.directive,
      factory : $provide.factory,
      service : $provide.service,
      constant: $provide.constant

    };

    $httpProvider.interceptors.push('NeutronInterceptor');

    NavHelperProvider.addControllerUrl('src/app/ovsdb/ovsdb.controller.js');
    NavHelperProvider.addToMenu('Ovsdb', {
     "link" : "#/ovsdb/index",
     "active" : "main.ovsdb.*",
     "title" : "Network Virtualization",
     "icon" : "icon-sitemap",
     "page" : {
        "title" : "NetWork Virtualization",
        "description" : "OVSDB"
     }
    });

    var access = routingConfig.accessLevels;
    $stateProvider.state('main.ovsdb', {
      url: 'ovsdb',
      abstract: true,
      views : {
        'content' : {
          templateUrl: 'src/app/ovsdb/views/root.tpl.html',
          controller: 'RootOvsdbCtrl'
        }
      }
    });

    $stateProvider.state('main.ovsdb.index', {
      url: '/index',
      access: access.admin,
      views: {
        '': {
          templateUrl: 'src/app/ovsdb/views/index.tpl.html',
          controller: 'OvsdbCtrl'
        }
      }
    });
  });

  return ovsdb;
});
