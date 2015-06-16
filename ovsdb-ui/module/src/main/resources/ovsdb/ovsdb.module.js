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

  ovsdb.config(function($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider) {
    ovsdb.register = {
      controller : $controllerProvider.register,
      directive : $compileProvider.directive,
      factory : $provide.factory,
      service : $provide.service

    };

    NavHelperProvider.addControllerUrl('src/app/ovsdb/ovsdb.controller.js');
    NavHelperProvider.addToMenu('Ovsdb', {
     "link" : "#/ovsdb/index",
     "active" : "main.ovsdb.*",
     "title" : "OVSDB",
     "icon" : "icon-sitemap",
     "page" : {
        "title" : "OVSDB",
        "description" : "OVSDB"
     }
    });

    var access = routingConfig.accessLevels;
    $stateProvider.state('main.ovsdb', {
      url: 'ovsdb',
      abstract: true,
      views : {
        'content' : {
          templateUrl: 'src/app/ovsdb/root.tpl.html',
          controller: 'RootOvsdbCtrl'
        }
      }
    });

    $stateProvider.state('main.ovsdb.index', {
      url: '/index',
      access: access.admin,
      views: {
        '': {
          templateUrl: 'src/app/ovsdb/index.tpl.html',
          controller: 'OvsdbCtrl'
        }
      }
    });
  });

  return ovsdb;
});
