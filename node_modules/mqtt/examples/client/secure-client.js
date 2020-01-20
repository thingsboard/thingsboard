'use strict'

var mqtt = require('../..')
var path = require('path')
var fs = require('fs')
var KEY = fs.readFileSync(path.join(__dirname, '..', '..', 'test', 'helpers', 'tls-key.pem'))
var CERT = fs.readFileSync(path.join(__dirname, '..', '..', 'test', 'helpers', 'tls-cert.pem'))

var PORT = 8443

var options = {
  port: PORT,
  key: KEY,
  cert: CERT,
  rejectUnauthorized: false
}

var client = mqtt.connect(options)

client.subscribe('messages')
client.publish('messages', 'Current time is: ' + new Date())
client.on('message', function (topic, message) {
  console.log(message)
})
