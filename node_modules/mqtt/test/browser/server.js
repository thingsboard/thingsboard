'use strict'

var handleClient
var websocket = require('websocket-stream')
var WebSocketServer = require('ws').Server
var Connection = require('mqtt-connection')
var http = require('http')

handleClient = function (client) {
  var self = this

  if (!self.clients) {
    self.clients = {}
  }

  client.on('connect', function (packet) {
    if (packet.clientId === 'invalid') {
      client.connack({returnCode: 2})
    } else {
      client.connack({returnCode: 0})
    }
    self.clients[packet.clientId] = client
    client.subscriptions = []
  })

  client.on('publish', function (packet) {
    var i, k, c, s, publish
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

    for (k in self.clients) {
      c = self.clients[k]
      publish = false

      for (i = 0; i < c.subscriptions.length; i++) {
        s = c.subscriptions[i]

        if (s.test(packet.topic)) {
          publish = true
        }
      }

      if (publish) {
        try {
          c.publish({topic: packet.topic, payload: packet.payload})
        } catch (error) {
          delete self.clients[k]
        }
      }
    }
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
    var qos
    var topic
    var reg
    var granted = []

    for (var i = 0; i < packet.subscriptions.length; i++) {
      qos = packet.subscriptions[i].qos
      topic = packet.subscriptions[i].topic
      reg = new RegExp(topic.replace('+', '[^/]+').replace('#', '.+') + '$')

      granted.push(qos)
      client.subscriptions.push(reg)
    }

    client.suback({messageId: packet.messageId, granted: granted})
  })

  client.on('unsubscribe', function (packet) {
    client.unsuback(packet)
  })

  client.on('pingreq', function () {
    client.pingresp()
  })
}

function start (startPort, done) {
  var server = http.createServer()
  var wss = new WebSocketServer({server: server})

  wss.on('connection', function (ws) {
    var stream, connection

    if (!(ws.protocol === 'mqtt' ||
          ws.protocol === 'mqttv3.1')) {
      return ws.close()
    }

    stream = websocket(ws)
    connection = new Connection(stream)
    handleClient.call(server, connection)
  })
  server.listen(startPort, done)
  server.on('request', function (req, res) {
    res.statusCode = 404
    res.end('Not Found')
  })
  return server
}

if (require.main === module) {
  start(process.env.PORT || process.env.ZUUL_PORT, function (err) {
    if (err) {
      console.error(err)
      return
    }
    console.log('tunnelled server started on port', process.env.PORT || process.env.ZUUL_PORT)
  })
}
