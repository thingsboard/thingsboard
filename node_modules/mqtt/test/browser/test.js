'use strict'

var mqtt = require('../../lib/connect')
var _URL = require('url')
var xtend = require('xtend')
var parsed = _URL.parse(document.URL)
var isHttps = parsed.protocol === 'https:'
var port = parsed.port || (isHttps ? 443 : 80)
var host = parsed.hostname
var protocol = isHttps ? 'wss' : 'ws'

function clientTests (buildClient) {
  var client

  beforeEach(function () {
    client = buildClient()
    client.on('offline', function () {
      console.log('client offline')
    })
    client.on('connect', function () {
      console.log('client connect')
    })
    client.on('reconnect', function () {
      console.log('client reconnect')
    })
  })

  afterEach(function (done) {
    client.once('close', function () {
      done()
    })
    client.end()
  })

  it('should connect', function (done) {
    client.on('connect', function () {
      done()
    })
  })

  it('should publish and subscribe', function (done) {
    client.subscribe('hello', function () {
      done()
    }).publish('hello', 'world')
  })
}

function suiteFactory (configName, opts) {
  function setVersion (base) {
    return xtend(base || {}, opts)
  }

  var suiteName = 'MqttClient(' + configName + '=' + JSON.stringify(opts) + ')'
  describe(suiteName, function () {
    this.timeout(10000)

    describe('specifying nothing', function () {
      clientTests(function () {
        return mqtt.connect(setVersion())
      })
    })

    if (parsed.hostname === 'localhost') {
      describe('specifying a port', function () {
        clientTests(function () {
          return mqtt.connect(setVersion({ protocol: protocol, port: port }))
        })
      })
    }

    describe('specifying a port and host', function () {
      clientTests(function () {
        return mqtt.connect(setVersion({ protocol: protocol, port: port, host: host }))
      })
    })

    describe('specifying a URL', function () {
      clientTests(function () {
        return mqtt.connect(protocol + '://' + host + ':' + port, setVersion())
      })
    })

    describe('specifying a URL with a path', function () {
      clientTests(function () {
        return mqtt.connect(protocol + '://' + host + ':' + port + '/mqtt', setVersion())
      })
    })
  })
}

suiteFactory('v3', {protocolId: 'MQIsdp', protocolVersion: 3})
suiteFactory('default', {})
