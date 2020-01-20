'use strict'

var mqtt = require('../..')
var client = mqtt.connect()

client.publish('presence', 'hello!')
client.end()
