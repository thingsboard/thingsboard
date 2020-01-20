'use strict'

var net = require('net')
var tls = require('tls')
var inherits = require('inherits')
var Connection = require('mqtt-connection')
var MqttServer
var FastMqttServer
var MqttSecureServer

function setupConnection (duplex) {
  var that = this
  var connection = new Connection(duplex, function () {
    that.emit('client', connection)
  })
}

/*
 * MqttServer
 *
 * @param {Function} listener - fired on client connection
 */
MqttServer = module.exports = function Server (listener) {
  if (!(this instanceof Server)) {
    return new Server(listener)
  }

  net.Server.call(this)

  this.on('connection', setupConnection)

  if (listener) {
    this.on('client', listener)
  }

  return this
}
inherits(MqttServer, net.Server)

/*
 * FastMqttServer(w/o waiting for initialization)
 *
 * @param {Function} listener - fired on client connection
 */
FastMqttServer = module.exports.FastMqttServer = function Server (listener) {
  if (!(this instanceof Server)) {
    return new Server(listener)
  }

  net.Server.call(this)

  this.on('connection', function (duplex) {
    var connection = new Connection(duplex)
    this.emit('client', connection)
  })

  if (listener) {
    this.on('client', listener)
  }

  return this
}
inherits(FastMqttServer, net.Server)

/**
 * MqttSecureServer
 *
 * @param {Object} opts - server options
 * @param {Function} listener
 */
MqttSecureServer = module.exports.SecureServer =
  function SecureServer (opts, listener) {
    if (!(this instanceof SecureServer)) {
      return new SecureServer(opts, listener)
    }

    // new MqttSecureServer(function(){})
    if (typeof opts === 'function') {
      listener = opts
      opts = {}
    }

    tls.Server.call(this, opts)

    if (listener) {
      this.on('client', listener)
    }

    this.on('secureConnection', setupConnection)

    return this
  }
inherits(MqttSecureServer, tls.Server)
