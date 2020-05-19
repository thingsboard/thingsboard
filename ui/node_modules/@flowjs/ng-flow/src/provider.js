/**
 * @description
 * var app = angular.module('App', ['flow.provider'], function(flowFactoryProvider){
 *    flowFactoryProvider.defaults = {target: '/'};
 * });
 * @name flowFactoryProvider
 */
angular.module('flow.provider', [])
.provider('flowFactory', function() {
  'use strict';
  /**
   * Define the default properties for flow.js
   * @name flowFactoryProvider.defaults
   * @type {Object}
   */
  this.defaults = {};

  /**
   * Flow, MaybeFlow or NotFlow
   * @name flowFactoryProvider.factory
   * @type {function}
   * @return {Flow}
   */
  this.factory = function (options) {
    return new Flow(options);
  };

  /**
   * Define the default events
   * @name flowFactoryProvider.events
   * @type {Array}
   * @private
   */
  this.events = [];

  /**
   * Add default events
   * @name flowFactoryProvider.on
   * @function
   * @param {string} event
   * @param {Function} callback
   */
  this.on = function (event, callback) {
    this.events.push([event, callback]);
  };

  this.$get = function() {
    var fn = this.factory;
    var defaults = this.defaults;
    var events = this.events;
    return {
      'create': function(opts) {
        // combine default options with global options and options
        var flow = fn(angular.extend({}, defaults, opts));
        angular.forEach(events, function (event) {
          flow.on(event[0], event[1]);
        });
        return flow;
      }
    };
  };
});