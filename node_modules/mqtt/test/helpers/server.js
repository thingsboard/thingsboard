'use strict'

var Server = require('../server')
var fs = require('fs')

module.exports.init_server = function (PORT) {
  var server = new Server(function (client) {
    client.on('connect', function () {
      client.connack(0)
    })

    client.on('publish', function (packet) {
      switch (packet.qos) {
        case 1:
          client.puback({messageId: packet.messageId})
          break
        case 2:
          client.pubrec({messageId: packet.messageId})
          break
        default:
          break
      }
    })

    client.on('pubrel', function (packet) {
      client.pubcomp({messageId: packet.messageId})
    })

    client.on('pingreq', function () {
      client.pingresp()
    })

    client.on('disconnect', function () {
      client.stream.end()
    })
  })
  server.listen(PORT)
  return server
}

module.exports.init_secure_server = function (port, key, cert) {
  var server = new Server.SecureServer({
    key: fs.readFileSync(key),
    cert: fs.readFileSync(cert)
  }, function (client) {
    client.on('connect', function () {
      client.connack({returnCode: 0})
    })
  })
  server.listen(port)
  return server
}
