'use strict'

var mqtt = require('mqtt')
var HttpsProxyAgent = require('https-proxy-agent')
var url = require('url')
/*
host: host of the endpoint you want to connect e.g. my.mqqt.host.com
path: path to you endpoint e.g. '/foo/bar/mqtt'
*/
var endpoint = 'wss://<host><path>'
/* create proxy agent
proxy: your proxy e.g. proxy.foo.bar.com
port: http proxy port e.g. 8080
*/
var proxy = process.env.http_proxy || 'http://<proxy>:<port>'
var parsed = url.parse(endpoint)
var proxyOpts = url.parse(proxy)
// true for wss
proxyOpts.secureEndpoint = parsed.protocol ? parsed.protocol === 'wss:' : true
var agent = new HttpsProxyAgent(proxyOpts)
var wsOptions = {
  agent: agent
  // other wsOptions
  // foo:'bar'
}
var mqttOptions = {
  keepalive: 60,
  reschedulePings: true,
  protocolId: 'MQTT',
  protocolVersion: 4,
  reconnectPeriod: 1000,
  connectTimeout: 30 * 1000,
  clean: true,
  clientId: 'testClient',
  wsOptions: wsOptions
}

var client = mqtt.connect(parsed, mqttOptions)

client.on('connect', function () {
  console.log('connected')
})

client.on('error', function (a) {
  console.log('error!' + a)
})

client.on('offline', function (a) {
  console.log('lost connection!' + a)
})

client.on('close', function (a) {
  console.log('connection closed!' + a)
})

client.on('message', function (topic, message) {
  console.log(message.toString())
})
