'use strict'

var fs = require('fs')
var path = require('path')
var mqtt = require('../')

describe('mqtt', function () {
  describe('#connect', function () {
    var sslOpts, sslOpts2
    it('should return an MqttClient when connect is called with mqtt:/ url', function () {
      var c = mqtt.connect('mqtt://localhost:1883')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.end()
    })

    it('should throw an error when called with no protocol specified', function () {
      (function () {
        var c = mqtt.connect('foo.bar.com')
        c.end()
      }).should.throw('Missing protocol')
    })

    it('should throw an error when called with no protocol specified - with options', function () {
      (function () {
        var c = mqtt.connect('tcp://foo.bar.com', { protocol: null })
        c.end()
      }).should.throw('Missing protocol')
    })

    it('should return an MqttClient with username option set', function () {
      var c = mqtt.connect('mqtt://user:pass@localhost:1883')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('username', 'user')
      c.options.should.have.property('password', 'pass')
      c.end()
    })

    it('should return an MqttClient with username and password options set', function () {
      var c = mqtt.connect('mqtt://user@localhost:1883')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('username', 'user')
      c.end()
    })

    it('should return an MqttClient with the clientid with random value', function () {
      var c = mqtt.connect('mqtt://user@localhost:1883')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('clientId')
      c.end()
    })

    it('should return an MqttClient with the clientid with empty string', function () {
      var c = mqtt.connect('mqtt://user@localhost:1883?clientId=')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('clientId', '')
      c.end()
    })

    it('should return an MqttClient with the clientid option set', function () {
      var c = mqtt.connect('mqtt://user@localhost:1883?clientId=123')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('clientId', '123')
      c.end()
    })

    it('should return an MqttClient when connect is called with tcp:/ url', function () {
      var c = mqtt.connect('tcp://localhost')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.end()
    })

    it('should return an MqttClient with correct host when called with a host and port', function () {
      var c = mqtt.connect('tcp://user:pass@localhost:1883')

      c.options.should.have.property('hostname', 'localhost')
      c.options.should.have.property('port', 1883)
      c.end()
    })

    sslOpts = {
      keyPath: path.join(__dirname, 'helpers', 'private-key.pem'),
      certPath: path.join(__dirname, 'helpers', 'public-cert.pem'),
      caPaths: [path.join(__dirname, 'helpers', 'public-cert.pem')]
    }

    it('should return an MqttClient when connect is called with mqtts:/ url', function () {
      var c = mqtt.connect('mqtts://localhost', sslOpts)

      c.options.should.have.property('protocol', 'mqtts')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
      c.end()
    })

    it('should return an MqttClient when connect is called with ssl:/ url', function () {
      var c = mqtt.connect('ssl://localhost', sslOpts)

      c.options.should.have.property('protocol', 'ssl')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
      c.end()
    })

    it('should return an MqttClient when connect is called with ws:/ url', function () {
      var c = mqtt.connect('ws://localhost', sslOpts)

      c.options.should.have.property('protocol', 'ws')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
      c.end()
    })

    it('should return an MqttClient when connect is called with wss:/ url', function () {
      var c = mqtt.connect('wss://localhost', sslOpts)

      c.options.should.have.property('protocol', 'wss')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
      c.end()
    })

    sslOpts2 = {
      key: fs.readFileSync(path.join(__dirname, 'helpers', 'private-key.pem')),
      cert: fs.readFileSync(path.join(__dirname, 'helpers', 'public-cert.pem')),
      ca: [fs.readFileSync(path.join(__dirname, 'helpers', 'public-cert.pem'))]
    }

    it('should throw an error when it is called with cert and key set but no protocol specified', function () {
      // to do rewrite wrap function
      (function () {
        var c = mqtt.connect(sslOpts2)
        c.end()
      }).should.throw('Missing secure protocol key')
    })

    it('should throw an error when it is called with cert and key set and protocol other than allowed: mqtt,mqtts,ws,wss,wxs', function () {
      (function () {
        sslOpts2.protocol = 'UNKNOWNPROTOCOL'
        var c = mqtt.connect(sslOpts2)
        c.end()
      }).should.throw()
    })

    it('should return a MqttClient with mqtts set when connect is called key and cert set and protocol mqtt', function () {
      sslOpts2.protocol = 'mqtt'
      var c = mqtt.connect(sslOpts2)

      c.options.should.have.property('protocol', 'mqtts')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
    })

    it('should return a MqttClient with mqtts set when connect is called key and cert set and protocol mqtts', function () {
      sslOpts2.protocol = 'mqtts'
      var c = mqtt.connect(sslOpts2)

      c.options.should.have.property('protocol', 'mqtts')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
    })

    it('should return a MqttClient with wss set when connect is called key and cert set and protocol ws', function () {
      sslOpts2.protocol = 'ws'
      var c = mqtt.connect(sslOpts2)

      c.options.should.have.property('protocol', 'wss')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
    })

    it('should return a MqttClient with wss set when connect is called key and cert set and protocol wss', function () {
      sslOpts2.protocol = 'wss'
      var c = mqtt.connect(sslOpts2)

      c.options.should.have.property('protocol', 'wss')

      c.on('error', function () {})

      c.should.be.instanceOf(mqtt.MqttClient)
    })

    it('should return an MqttClient with the clientid with option of clientId as empty string', function () {
      var c = mqtt.connect('mqtt://localhost:1883', {
        clientId: ''
      })

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('clientId', '')
    })

    it('should return an MqttClient with the clientid with option of clientId empty', function () {
      var c = mqtt.connect('mqtt://localhost:1883')

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('clientId')
      c.end()
    })

    it('should return an MqttClient with the clientid with option of with specific clientId', function () {
      var c = mqtt.connect('mqtt://localhost:1883', {
        clientId: '123'
      })

      c.should.be.instanceOf(mqtt.MqttClient)
      c.options.should.have.property('clientId', '123')
      c.end()
    })
  })
})
