'use strict'

var mqtt = require('../..')
var client = mqtt.connect()

// or var client = mqtt.connect({ port: 1883, host: '192.168.1.100', keepalive: 10000});

client.subscribe('presence')
client.publish('presence', 'bin hier')
client.on('message', function (topic, message) {
  console.log(message)
})
client.end()
