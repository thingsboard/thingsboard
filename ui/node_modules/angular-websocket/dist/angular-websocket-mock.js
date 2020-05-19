(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(['module', 'angular'], factory);
  } else if (typeof exports !== "undefined") {
    factory(module, require('angular'));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod, global.angular);
    global.angularWebsocketMock = mod.exports;
  }
})(this, function (module, _angular) {
  'use strict';

  var _angular2 = _interopRequireDefault(_angular);

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  function $WebSocketBackend() {
    var connectQueue = [];
    var pendingConnects = [];
    var closeQueue = [];
    var pendingCloses = [];
    var sendQueue = [];
    var pendingSends = [];
    var mock = false;

    function $MockWebSocket(url, protocols) {
      this.protocols = protocols;
      this.ssl = /(wss)/i.test(this.url);
    }

    $MockWebSocket.prototype.send = function (msg) {
      pendingSends.push(msg);
    };

    this.mockSend = function () {
      if (mock) {
        return sendQueue.shift();
      }
    };

    this.mock = function () {
      mock = true;
    };

    this.isMocked = function () {
      return mock;
    };

    this.isConnected = function (url) {
      return connectQueue.indexOf(url) > -1;
    };

    $MockWebSocket.prototype.close = function () {
      pendingCloses.push(true);
    };

    function createWebSocketBackend(url, protocols) {
      pendingConnects.push(url);
      // pendingConnects.push({
      //   url: url,
      //   protocols: protocols
      // });

      if (protocols) {
        return new $MockWebSocket(url, protocols);
      }
      return new $MockWebSocket(url);
    }
    this.create = createWebSocketBackend;
    this.createWebSocketBackend = createWebSocketBackend;

    this.flush = function () {
      var url, msg, config;
      while (url = pendingConnects.shift()) {
        var i = connectQueue.indexOf(url);
        if (i > -1) {
          connectQueue.splice(i, 1);
        }
        // if (config && config.url) {
        // }
      }

      while (pendingCloses.shift()) {
        closeQueue.shift();
      }

      while (msg = pendingSends.shift()) {
        var j;
        sendQueue.forEach(function (pending, i) {
          if (pending.message === msg.message) {
            j = i;
          }
        });

        if (j > -1) {
          sendQueue.splice(j, 1);
        }
      }
    };

    this.expectConnect = function (url, protocols) {
      connectQueue.push(url);
      // connectQueue.push({url: url, protocols: protocols});
    };

    this.expectClose = function () {
      closeQueue.push(true);
    };

    this.expectSend = function (msg) {
      sendQueue.push(msg);
    };

    this.verifyNoOutstandingExpectation = function () {
      if (connectQueue.length || closeQueue.length || sendQueue.length) {
        throw new Error('Requests waiting to be flushed');
      }
    };

    this.verifyNoOutstandingRequest = function () {
      if (pendingConnects.length || pendingCloses.length || pendingSends.length) {
        throw new Error('Requests waiting to be processed');
      }
    };
  } // end $WebSocketBackend

  _angular2.default.module('ngWebSocketMock', []).service('WebSocketBackend', $WebSocketBackend).service('$websocketBackend', $WebSocketBackend);

  _angular2.default.module('angular-websocket-mock', ['ngWebSocketMock']);

  module.exports = _angular2.default.module('ngWebSocketMock');
});