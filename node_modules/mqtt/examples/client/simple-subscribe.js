'use strict'

var mqtt = require('../..')
var client = mqtt.connect()

client.subscribe('presence')
client.on('message', function (topic, message) {
  console.log(message)
})
