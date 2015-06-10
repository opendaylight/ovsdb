define([], function() {
  var ovsdb = angular.module('app.ovsdb', []);

  ovsdb.register = {
    controller: ovsdb.controller,
    service: ovsdb.service,
    factory: ovsdb.factory
  };

  return ovsdb;

})
