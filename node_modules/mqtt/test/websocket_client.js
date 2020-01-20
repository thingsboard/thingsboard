'use strict'

var http = require('http')
var websocket = require('websocket-stream')
var WebSocketServer = require('ws').Server
var Connection = require('mqtt-connection')
var abstractClientTests = require('./abstract_client')
var mqtt = require('../')
var xtend = require('xtend')
var assert = require('assert')
var port = 9999
var server = http.createServer()

function attachWebsocketServer (wsServer) {
  var wss = new WebSocketServer({server: wsServer, perMessageDeflate: false})

  wss.on('connection', function (ws) {
    var stream = websocket(ws)
    var connection = new Connection(stream)

    wsServer.emit('client', connection)
    stream.on('error', function () {})
    connection.on('error', function () {})
  })

  return wsServer
}

attachWebsocketServer(server)

server.on('client', function (client) {
  client.on('connect', function (packet) {
    if (packet.clientId === 'invalid') {
      client.connack({ returnCode: 2 })
    } else {
      server.emit('connect', client)
      client.connack({returnCode: 0})
    }
  })

  client.on('publish', function (packet) {
    setImmediate(function () {
      switch (packet.qos) {
        case 0:
          break
        case 1:
          client.puback(packet)
          break
        case 2:
          client.pubrec(packet)
          break
      }
    })
  })

  client.on('pubrel', function (packet) {
    client.pubcomp(packet)
  })

  client.on('pubrec', function (packet) {
    client.pubrel(packet)
  })

  client.on('pubcomp', function () {
    // Nothing to be done
  })

  client.on('subscribe', function (packet) {
    client.suback({
      messageId: packet.messageId,
      granted: packet.subscriptions.map(function (e) {
        return e.qos
      })
    })
  })

  client.on('unsubscribe', function (packet) {
    client.unsuback(packet)
  })

  client.on('pingreq', function () {
    client.pingresp()
  })
}).listen(port)

describe('Websocket Client', function () {
  var baseConfig = { protocol: 'ws', port: port }

  function makeOptions (custom) {
    // xtend returns a new object. Does not mutate arguments
    return xtend(baseConfig, custom || {})
  }

  it('should use mqtt as the protocol by default', function (done) {
    server.once('client', function (client) {
      client.stream.socket.protocol.should.equal('mqtt')
    })
    mqtt.connect(makeOptions()).on('connect', function () {
      this.end(true, done)
    })
  })

  it('should be able transform the url (for e.g. to sign it)', function (done) {
    var baseUrl = 'ws://localhost:9999/mqtt'
    var sig = '?AUTH=token'
    var expected = baseUrl + sig
    var actual
    var opts = makeOptions({
      path: '/mqtt',
      transformWsUrl: function (url, opt, client) {
        assert.equal(url, baseUrl)
        assert.strictEqual(opt, opts)
        assert.strictEqual(client.options, opts)
        assert.strictEqual(typeof opt.transformWsUrl, 'function')
        assert(client instanceof mqtt.MqttClient)
        url += sig
        actual = url
        return url
      }})
    mqtt.connect(opts)
      .on('connect', function () {
        assert.equal(this.stream.socket.url, expected)
        assert.equal(actual, expected)
        this.end(true, done)
      })
  })

  it('should use mqttv3.1 as the protocol if using v3.1', function (done) {
    server.once('client', function (client) {
      client.stream.socket.protocol.should.equal('mqttv3.1')
    })

    var opts = makeOptions({
      protocolId: 'MQIsdp',
      protocolVersion: 3
    })

    mqtt.connect(opts).on('connect', function () {
      this.end(true, done)
    })
  })

  abstractClientTests(server, makeOptions())
})
