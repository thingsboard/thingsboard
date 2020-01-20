'use strict'

var Server = require('../server')

new Server(function (client) {
  client.on('connect', function () {
    client.connack({ returnCode: 0 })
  })
}).listen(3000, 'localhost')
