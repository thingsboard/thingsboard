(function() {

  'use strict';

  function Mouseoverfactory() {
    return function(mouseoverscope, applyFunction) {
      var mouseoverservice = {};

      mouseoverscope.connector = null;
      mouseoverscope.edge = null;
      mouseoverscope.node = null;

      mouseoverservice.nodeMouseOver = function(node) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.node = node;
          });
        };
      };

      mouseoverservice.nodeMouseOut = function(node) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.node = null;
          });
        };
      };

      mouseoverservice.connectorMouseEnter = function(connector) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.connector = connector;
          });
        };
      };

      mouseoverservice.connectorMouseLeave = function(connector) {
        return function(event) {
          return applyFunction(function() {
            mouseoverscope.connector = null
          });
        };
      };

      mouseoverservice.edgeMouseEnter = function(event, edge) {
        mouseoverscope.edge = edge;
      };

      mouseoverservice.edgeMouseLeave = function(event, egde) {
        mouseoverscope.edge = null;
      };

      return mouseoverservice;
    };
  }

  angular.module('flowchart')
    .factory('Mouseoverfactory', Mouseoverfactory);
}());
