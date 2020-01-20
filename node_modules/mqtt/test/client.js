'use strict'

var mqtt = require('..')
var should = require('should')
var fork = require('child_process').fork
var path = require('path')
var abstractClientTests = require('./abstract_client')
var net = require('net')
var eos = require('end-of-stream')
var mqttPacket = require('mqtt-packet')
var Buffer = require('safe-buffer').Buffer
var Duplex = require('readable-stream').Duplex
var Connection = require('mqtt-connection')
var Server = require('./server')
var FastServer = require('./server').FastMqttServer
var port = 9876
var server

function connOnlyServer () {
  return new Server(function (client) {
    client.on('connect', function (packet) {
      client.connack({returnCode: 0})
    })
  })
}

/**
 * Test server
 */
function buildServer (fastFlag) {
  var handler = function (client) {
    client.on('auth', function (packet) {
      var rc = 'reasonCode'
      var connack = {}
      connack[rc] = 0
      client.connack(connack)
    })
    client.on('connect', function (packet) {
      var rc = 'returnCode'
      var connack = {}
      if (client.options && client.options.protocolVersion === 5) {
        rc = 'reasonCode'
        if (packet.clientId === 'invalid') {
          connack[rc] = 128
        } else {
          connack[rc] = 0
        }
      } else {
        if (packet.clientId === 'invalid') {
          connack[rc] = 2
        } else {
          connack[rc] = 0
        }
      }
      if (packet.properties && packet.properties.authenticationMethod) {
        return false
      } else {
        client.connack(connack)
      }
    })

    client.on('publish', function (packet) {
      setImmediate(function () {
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
      })
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
      client.suback({
        messageId: packet.messageId,
        granted: packet.subscriptions.map(function (e) {
          return e.qos
        })
      })
    })

    client.on('unsubscribe', function (packet) {
      packet.granted = packet.unsubscriptions.map(function () { return 0 })
      client.unsuback(packet)
    })

    client.on('pingreq', function () {
      client.pingresp()
    })
  }
  if (fastFlag) {
    return new FastServer(handler)
  } else {
    return new Server(handler)
  }
}

server = buildServer().listen(port)

describe('MqttClient', function () {
  describe('creating', function () {
    it('should allow instantiation of MqttClient without the \'new\' operator', function (done) {
      should(function () {
        var client
        try {
          client = mqtt.MqttClient(function () {
            throw Error('break')
          }, {})
          client.end()
        } catch (err) {
          if (err.message !== 'break') {
            throw err
          }
          done()
        }
      }).not.throw('Object #<Object> has no method \'_setupStream\'')
    })
  })

  var config = { protocol: 'mqtt', port: port }
  abstractClientTests(server, config)

  describe('message ids', function () {
    it('should increment the message id', function () {
      var client = mqtt.connect(config)
      var currentId = client._nextId()

      client._nextId().should.equal(currentId + 1)
      client.end()
    })

    it('should return 1 once the internal counter reached limit', function () {
      var client = mqtt.connect(config)
      client.nextId = 65535

      client._nextId().should.equal(65535)
      client._nextId().should.equal(1)
      client.end()
    })

    it('should return 65535 for last message id once the internal counter reached limit', function () {
      var client = mqtt.connect(config)
      client.nextId = 65535

      client._nextId().should.equal(65535)
      client.getLastMessageId().should.equal(65535)
      client._nextId().should.equal(1)
      client.getLastMessageId().should.equal(1)
      client.end()
    })

    it('should not throw an error if packet\'s messageId is not found when receiving a pubrel packet', function (done) {
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
          c.pubrel({ messageId: Math.floor(Math.random() * 9000) + 1000 })
        })
      })

      server2.listen(port + 49, function () {
        var client = mqtt.connect({
          port: port + 49,
          host: 'localhost'
        })

        client.on('packetsend', function (packet) {
          if (packet.cmd === 'pubcomp') {
            client.end()
            server2.close()
            done()
          }
        })
      })
    })

    it('should not go overflow if the TCP frame contains a lot of PUBLISH packets', function (done) {
      var parser = mqttPacket.parser()
      var count = 0
      var max = 1000
      var duplex = new Duplex({
        read: function (n) {},
        write: function (chunk, enc, cb) {
          parser.parse(chunk)
          cb() // nothing to do
        }
      })
      var client = new mqtt.MqttClient(function () {
        return duplex
      }, {})

      client.on('message', function (t, p, packet) {
        if (++count === max) {
          done()
        }
      })

      parser.on('packet', function (packet) {
        var packets = []

        if (packet.cmd === 'connect') {
          duplex.push(mqttPacket.generate({
            cmd: 'connack',
            sessionPresent: false,
            returnCode: 0
          }))

          for (var i = 0; i < max; i++) {
            packets.push(mqttPacket.generate({
              cmd: 'publish',
              topic: Buffer.from('hello'),
              payload: Buffer.from('world'),
              retain: false,
              dup: false,
              messageId: i + 1,
              qos: 1
            }))
          }

          duplex.push(Buffer.concat(packets))
        }
      })
    })
  })

  describe('flushing', function () {
    it('should attempt to complete pending unsub and send on ping timeout', function (done) {
      this.timeout(10000)
      var server3 = connOnlyServer().listen(port + 72)
      var pubCallbackCalled = false
      var unsubscribeCallbackCalled = false
      var client = mqtt.connect({
        port: port + 72,
        host: 'localhost',
        keepalive: 1,
        connectTimeout: 350,
        reconnectPeriod: 0
      })
      client.once('connect', () => {
        client.publish('fakeTopic', 'fakeMessage', {qos: 1}, (err, result) => {
          should.exist(err)
          pubCallbackCalled = true
        })
        client.unsubscribe('fakeTopic', (err, result) => {
          should.exist(err)
          unsubscribeCallbackCalled = true
        })
        setTimeout(() => {
          client.end(() => {
            should.equal(pubCallbackCalled && unsubscribeCallbackCalled, true, 'callbacks not invoked')
            server3.close()
            done()
          })
        }, 5000)
      })
    })
  })

  describe('reconnecting', function () {
    it('should attempt to reconnect once server is down', function (done) {
      this.timeout(15000)

      var innerServer = fork(path.join(__dirname, 'helpers', 'server_process.js'))
      var client = mqtt.connect({ port: 3000, host: 'localhost', keepalive: 1 })

      client.once('connect', function () {
        innerServer.kill('SIGINT') // mocks server shutdown

        client.once('close', function () {
          should.exist(client.reconnectTimer)
          client.end()
          done()
        })
      })
    })

    it('should reconnect to multiple host-ports-protocol combinations if servers is passed', function (done) {
      this.timeout(15000)

      var server = buildServer(true).listen(port + 41)
      var server2 = buildServer(true).listen(port + 42)

      server2.on('listening', function () {
        var client = mqtt.connect({
          protocol: 'wss',
          servers: [
            { port: port + 42, host: 'localhost', protocol: 'ws' },
            { port: port + 41, host: 'localhost' }
          ],
          keepalive: 50
        })
        server2.on('client', function (c) {
          should.equal(client.stream.socket.url, 'ws://localhost:9918/', 'Protocol for first connection should use ws.')
          c.stream.destroy()
          server2.close()
        })

        server.once('client', function () {
          should.equal(client.stream.socket.url, 'wss://localhost:9917/', 'Protocol for second client should use the default protocol: wss, on port: port + 42.')
          client.end()
          done()
        })

        client.once('connect', function () {
          client.stream.destroy()
        })
      })
    })

    it('should reconnect if a connack is not received in an interval', function (done) {
      this.timeout(2000)

      var server2 = net.createServer().listen(port + 43)

      server2.on('connection', function (c) {
        eos(c, function () {
          server2.close()
        })
      })

      server2.on('listening', function () {
        var client = mqtt.connect({
          servers: [
            { port: port + 43, host: 'localhost_fake' },
            { port: port, host: 'localhost' }
          ],
          connectTimeout: 500
        })

        server.once('client', function () {
          client.end()
          done()
        })

        client.once('connect', function () {
          client.stream.destroy()
        })
      })
    })

    it('should not be cleared by the connack timer', function (done) {
      this.timeout(4000)

      var server2 = net.createServer().listen(port + 44)

      server2.on('connection', function (c) {
        c.destroy()
      })

      server2.once('listening', function () {
        var reconnects = 0
        var connectTimeout = 1000
        var reconnectPeriod = 100
        var expectedReconnects = Math.floor(connectTimeout / reconnectPeriod)
        var client = mqtt.connect({
          port: port + 44,
          host: 'localhost',
          connectTimeout: connectTimeout,
          reconnectPeriod: reconnectPeriod
        })

        client.on('reconnect', function () {
          reconnects++
          if (reconnects >= expectedReconnects) {
            client.end()
            done()
          }
        })
      })
    })

    it('should not keep requeueing the first message when offline', function (done) {
      this.timeout(2500)

      var server2 = buildServer().listen(port + 45)
      var client = mqtt.connect({
        port: port + 45,
        host: 'localhost',
        connectTimeout: 350,
        reconnectPeriod: 300
      })

      server2.on('client', function (c) {
        client.publish('hello', 'world', { qos: 1 }, function () {
          c.destroy()
          server2.close()
          client.publish('hello', 'world', { qos: 1 })
        })
      })

      setTimeout(function () {
        if (client.queue.length === 0) {
          client.end(true)
          done()
        } else {
          client.end(true)
        }
      }, 2000)
    })

    it('should not send the same subscribe multiple times on a flaky connection', function (done) {
      this.timeout(3500)

      var KILL_COUNT = 4
      var killedConnections = 0
      var subIds = {}
      var client = mqtt.connect({
        port: port + 46,
        host: 'localhost',
        connectTimeout: 350,
        reconnectPeriod: 300
      })

      var server2 = new Server(function (client) {
        client.on('error', function () {})
        client.on('connect', function (packet) {
          if (packet.clientId === 'invalid') {
            client.connack({returnCode: 2})
          } else {
            client.connack({returnCode: 0})
          }
        })
      }).listen(port + 46)

      server2.on('client', function (c) {
        client.subscribe('topic', function () {
          done()
          client.end()
          c.destroy()
          server2.close()
        })

        c.on('subscribe', function (packet) {
          if (killedConnections < KILL_COUNT) {
            // Kill the first few sub attempts to simulate a flaky connection
            killedConnections++
            c.destroy()
          } else {
            // Keep track of acks
            if (!subIds[packet.messageId]) {
              subIds[packet.messageId] = 0
            }
            subIds[packet.messageId]++
            if (subIds[packet.messageId] > 1) {
              done(new Error('Multiple duplicate acked subscriptions received for messageId ' + packet.messageId))
              client.end(true)
              c.destroy()
              server2.destroy()
            }

            c.suback({
              messageId: packet.messageId,
              granted: packet.subscriptions.map(function (e) {
                return e.qos
              })
            })
          }
        })
      })
    })

    it('should not fill the queue of subscribes if it cannot connect', function (done) {
      this.timeout(2500)

      var port2 = port + 48

      var server2 = net.createServer(function (stream) {
        var client = new Connection(stream)

        client.on('error', function () {})
        client.on('connect', function (packet) {
          client.connack({returnCode: 0})
          client.destroy()
        })
      })

      server2.listen(port2, function () {
        var client = mqtt.connect({
          port: port2,
          host: 'localhost',
          connectTimeout: 350,
          reconnectPeriod: 300
        })

        client.subscribe('hello')

        setTimeout(function () {
          client.queue.length.should.equal(1)
          client.end()
          done()
        }, 1000)
      })
    })

    it('should not send the same publish multiple times on a flaky connection', function (done) {
      this.timeout(3500)

      var KILL_COUNT = 4
      var killedConnections = 0
      var pubIds = {}
      var client = mqtt.connect({
        port: port + 47,
        host: 'localhost',
        connectTimeout: 350,
        reconnectPeriod: 300
      })

      var server2 = net.createServer(function (stream) {
        var client = new Connection(stream)
        client.on('error', function () {})
        client.on('connect', function (packet) {
          if (packet.clientId === 'invalid') {
            client.connack({returnCode: 2})
          } else {
            client.connack({returnCode: 0})
          }
        })

        this.emit('client', client)
      }).listen(port + 47)

      server2.on('client', function (c) {
        client.publish('topic', 'data', { qos: 1 }, function () {
          done()
          client.end()
          c.destroy()
          server2.destroy()
        })

        c.on('publish', function onPublish (packet) {
          if (killedConnections < KILL_COUNT) {
            // Kill the first few pub attempts to simulate a flaky connection
            killedConnections++
            c.destroy()

            // to avoid receiving inflight messages
            c.removeListener('publish', onPublish)
          } else {
            // Keep track of acks
            if (!pubIds[packet.messageId]) {
              pubIds[packet.messageId] = 0
            }

            pubIds[packet.messageId]++

            if (pubIds[packet.messageId] > 1) {
              done(new Error('Multiple duplicate acked publishes received for messageId ' + packet.messageId))
              client.end(true)
              c.destroy()
              server2.destroy()
            }

            c.puback(packet)
          }
        })
      })
    })
  })

  it('check emit error on checkDisconnection w/o callback', function (done) {
    this.timeout(15000)
    var server118 = new Server(function (client) {
      client.on('connect', function (packet) {
        client.connack({
          reasonCode: 0
        })
      })
      client.on('publish', function (packet) {
        setImmediate(function () {
          packet.reasonCode = 0
          client.puback(packet)
        })
      })
    }).listen(port + 118)
    var opts = {
      host: 'localhost',
      port: port + 118,
      protocolVersion: 5
    }
    var client = mqtt.connect(opts)
    client.on('error', function (error) {
      should(error.message).be.equal('client disconnecting')
      server118.close()
      done()
    })
    client.on('connect', function () {
      client.end(function () {
        client._checkDisconnecting()
      })
      server118.close()
    })
  })

  describe('MQTT 5.0', function () {
    var server = buildServer().listen(port + 115)
    var config = { protocol: 'mqtt', port: port + 115, protocolVersion: 5, properties: { maximumPacketSize: 200 } }
    abstractClientTests(server, config)
    it('should has Auth method with Auth data', function (done) {
      this.timeout(5000)
      var opts = {host: 'localhost', port: port + 115, protocolVersion: 5, properties: { authenticationData: Buffer.from([1, 2, 3, 4]) }}
      try {
        mqtt.connect(opts)
      } catch (error) {
        should(error.message).be.equal('Packet has no Authentication Method')
      }
      done()
    })
    it('auth packet', function (done) {
      this.timeout(15000)
      server.once('client', function (client) {
        client.on('auth', function (packet) {
          done()
        })
      })
      var opts = {host: 'localhost', port: port + 115, protocolVersion: 5, properties: { authenticationMethod: 'json' }, authPacket: {}}
      mqtt.connect(opts)
    })
    it('Maximum Packet Size', function (done) {
      this.timeout(15000)
      var opts = {host: 'localhost', port: port + 115, protocolVersion: 5, properties: { maximumPacketSize: 1 }}
      var client = mqtt.connect(opts)
      client.on('error', function (error) {
        should(error.message).be.equal('exceeding packets size connack')
        done()
      })
    })
    describe('Topic Alias', function () {
      it('topicAlias > topicAliasMaximum', function (done) {
        this.timeout(15000)
        var maximum = 15
        var current = 22
        server.once('client', function (client) {
          client.on('publish', function (packet) {
            if (packet.properties && packet.properties.topicAlias) {
              done(new Error('Packet should not have topicAlias'))
              return false
            }
            done()
          })
        })
        var opts = {host: 'localhost', port: port + 115, protocolVersion: 5, properties: { topicAliasMaximum: maximum }}
        var client = mqtt.connect(opts)
        client.publish('t/h', 'Message', { properties: { topicAlias: current } })
      })
      it('topicAlias w/o topicAliasMaximum in settings', function (done) {
        this.timeout(15000)
        server.once('client', function (client) {
          client.on('publish', function (packet) {
            if (packet.properties && packet.properties.topicAlias) {
              done(new Error('Packet should not have topicAlias'))
              return false
            }
            done()
          })
        })
        var opts = {host: 'localhost', port: port + 115, protocolVersion: 5}
        var client = mqtt.connect(opts)
        client.publish('t/h', 'Message', { properties: { topicAlias: 22 } })
      })
    })
    it('Change values of some properties by server response', function (done) {
      this.timeout(15000)
      var server116 = new Server(function (client) {
        client.on('connect', function (packet) {
          client.connack({
            reasonCode: 0,
            properties: {
              topicAliasMaximum: 15,
              serverKeepAlive: 16,
              maximumPacketSize: 95
            }
          })
        })
      }).listen(port + 116)
      var opts = {
        host: 'localhost',
        port: port + 116,
        protocolVersion: 5,
        properties: {
          topicAliasMaximum: 10,
          serverKeepAlive: 11,
          maximumPacketSize: 100
        }
      }
      var client = mqtt.connect(opts)
      client.on('connect', function () {
        should(client.options.keepalive).be.equal(16)
        should(client.options.properties.topicAliasMaximum).be.equal(15)
        should(client.options.properties.maximumPacketSize).be.equal(95)
        server116.close()
        done()
      })
    })

    it('should resubscribe when reconnecting with protocolVersion 5 and Session Present flag is false', function (done) {
      this.timeout(15000)
      var tryReconnect = true
      var reconnectEvent = false
      var server316 = new Server(function (client) {
        client.on('connect', function (packet) {
          client.connack({
            reasonCode: 0,
            sessionPresent: false
          })
          client.on('subscribe', function () {
            if (!tryReconnect) {
              client.end()
              server316.close()
              done()
            }
          })
        })
      }).listen(port + 316)
      var opts = {
        host: 'localhost',
        port: port + 316,
        protocolVersion: 5
      }
      var client = mqtt.connect(opts)

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function (connack) {
        should(connack.sessionPresent).be.equal(false)
        if (tryReconnect) {
          client.subscribe('hello', function () {
            client.stream.end()
          })

          tryReconnect = false
        } else {
          reconnectEvent.should.equal(true)
        }
      })
    })

    it('should resubscribe when reconnecting with protocolVersion 5 and properties', function (done) {
      this.timeout(15000)
      var tryReconnect = true
      var reconnectEvent = false
      var server326 = new Server(function (client) {
        client.on('connect', function (packet) {
          client.on('subscribe', function (packet) {
            if (!reconnectEvent) {
              client.suback({
                messageId: packet.messageId,
                granted: packet.subscriptions.map(function (e) {
                  return e.qos
                })
              })
            } else {
              if (!tryReconnect) {
                should(packet.properties.userProperties.test).be.equal('test')
                client.end()
                server326.close()
                done()
              }
            }
          })
          client.connack({
            reasonCode: 0,
            sessionPresent: false
          })
        })
      }).listen(port + 326)
      var opts = {
        host: 'localhost',
        port: port + 326,
        protocolVersion: 5
      }
      var client = mqtt.connect(opts)

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function (connack) {
        should(connack.sessionPresent).be.equal(false)
        if (tryReconnect) {
          client.subscribe('hello', { properties: { userProperties: { test: 'test' } } }, function () {
            client.stream.end()
          })

          tryReconnect = false
        } else {
          reconnectEvent.should.equal(true)
        }
      })
    })

    var serverErr = new Server(function (client) {
      client.on('connect', function (packet) {
        client.connack({
          reasonCode: 0
        })
      })
      client.on('publish', function (packet) {
        setImmediate(function () {
          switch (packet.qos) {
            case 0:
              break
            case 1:
              packet.reasonCode = 142
              delete packet.cmd
              client.puback(packet)
              break
            case 2:
              packet.reasonCode = 142
              delete packet.cmd
              client.pubrec(packet)
              break
          }
        })
      })

      client.on('pubrel', function (packet) {
        packet.reasonCode = 142
        delete packet.cmd
        client.pubcomp(packet)
      })
    })
    it('Subscribe properties', function (done) {
      this.timeout(15000)
      var opts = {
        host: 'localhost',
        port: port + 119,
        protocolVersion: 5
      }
      var subOptions = { properties: { subscriptionIdentifier: 1234 } }
      var server119 = new Server(function (client) {
        client.on('connect', function (packet) {
          client.connack({
            reasonCode: 0
          })
        })
        client.on('subscribe', function (packet) {
          should(packet.properties.subscriptionIdentifier).be.equal(subOptions.properties.subscriptionIdentifier)
          server119.close()
          done()
        })
      }).listen(port + 119)

      var client = mqtt.connect(opts)
      client.on('connect', function () {
        client.subscribe('a/b', subOptions)
      })
    })

    it('puback handling errors check', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5
      }
      var client = mqtt.connect(opts)
      client.once('connect', () => {
        client.publish('a/b', 'message', {qos: 1}, function (err, packet) {
          should(err.message).be.equal('Publish error: Session taken over')
          should(err.code).be.equal(142)
        })
        serverErr.close()
        done()
      })
    })
    it('pubrec handling errors check', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 118)
      var opts = {
        host: 'localhost',
        port: port + 118,
        protocolVersion: 5
      }
      var client = mqtt.connect(opts)
      client.once('connect', () => {
        client.publish('a/b', 'message', {qos: 2}, function (err, packet) {
          should(err.message).be.equal('Publish error: Session taken over')
          should(err.code).be.equal(142)
        })
        serverErr.close()
        done()
      })
    })
    it('puback handling custom reason code', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5,
        customHandleAcks: function (topic, message, packet, cb) {
          var code = 0
          if (topic === 'a/b') {
            code = 128
          }
          cb(code)
        }
      }

      serverErr.once('client', function (c) {
        c.once('subscribe', function () {
          c.publish({ topic: 'a/b', payload: 'payload', qos: 1, messageId: 1 })
        })

        c.on('puback', function (packet) {
          should(packet.reasonCode).be.equal(128)
          client.end()
          c.destroy()
          serverErr.close()
          done()
        })
      })

      var client = mqtt.connect(opts)
      client.once('connect', function () {
        client.subscribe('a/b', {qos: 1})
      })
    })
    it('server side disconnect', function (done) {
      this.timeout(15000)
      var server327 = new Server(function (client) {
        client.on('connect', function (packet) {
          client.connack({
            reasonCode: 0
          })
          client.disconnect({reasonCode: 128})
          server327.close()
        })
      })
      server327.listen(port + 327)
      var opts = {
        host: 'localhost',
        port: port + 327,
        protocolVersion: 5
      }

      var client = mqtt.connect(opts)
      client.once('disconnect', function (disconnectPacket) {
        should(disconnectPacket.reasonCode).be.equal(128)
        done()
      })
    })
    it('pubrec handling custom reason code', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5,
        customHandleAcks: function (topic, message, packet, cb) {
          var code = 0
          if (topic === 'a/b') {
            code = 128
          }
          cb(code)
        }
      }

      serverErr.once('client', function (c) {
        c.once('subscribe', function () {
          c.publish({ topic: 'a/b', payload: 'payload', qos: 2, messageId: 1 })
        })

        c.on('pubrec', function (packet) {
          should(packet.reasonCode).be.equal(128)
          client.end()
          c.destroy()
          serverErr.close()
          done()
        })
      })

      var client = mqtt.connect(opts)
      client.once('connect', function () {
        client.subscribe('a/b', {qos: 1})
      })
    })
    it('puback handling custom reason code with error', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5,
        customHandleAcks: function (topic, message, packet, cb) {
          var code = 0
          if (topic === 'a/b') {
            cb(new Error('a/b is not valid'))
          }
          cb(code)
        }
      }

      serverErr.once('client', function (c) {
        c.once('subscribe', function () {
          c.publish({ topic: 'a/b', payload: 'payload', qos: 1, messageId: 1 })
        })
      })

      var client = mqtt.connect(opts)
      client.on('error', function (error) {
        should(error.message).be.equal('a/b is not valid')
        client.end()
        serverErr.close()
        done()
      })
      client.once('connect', function () {
        client.subscribe('a/b', {qos: 1})
      })
    })
    it('pubrec handling custom reason code with error', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5,
        customHandleAcks: function (topic, message, packet, cb) {
          var code = 0
          if (topic === 'a/b') {
            cb(new Error('a/b is not valid'))
          }
          cb(code)
        }
      }

      serverErr.once('client', function (c) {
        c.once('subscribe', function () {
          c.publish({ topic: 'a/b', payload: 'payload', qos: 2, messageId: 1 })
        })
      })

      var client = mqtt.connect(opts)
      client.on('error', function (error) {
        should(error.message).be.equal('a/b is not valid')
        client.end()
        serverErr.close()
        done()
      })
      client.once('connect', function () {
        client.subscribe('a/b', {qos: 1})
      })
    })
    it('puback handling custom invalid reason code', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5,
        customHandleAcks: function (topic, message, packet, cb) {
          var code = 0
          if (topic === 'a/b') {
            code = 124124
          }
          cb(code)
        }
      }

      serverErr.once('client', function (c) {
        c.once('subscribe', function () {
          c.publish({ topic: 'a/b', payload: 'payload', qos: 1, messageId: 1 })
        })
      })

      var client = mqtt.connect(opts)
      client.on('error', function (error) {
        should(error.message).be.equal('Wrong reason code for puback')
        client.end()
        serverErr.close()
        done()
      })
      client.once('connect', function () {
        client.subscribe('a/b', {qos: 1})
      })
    })
    it('pubrec handling custom invalid reason code', function (done) {
      this.timeout(15000)
      serverErr.listen(port + 117)
      var opts = {
        host: 'localhost',
        port: port + 117,
        protocolVersion: 5,
        customHandleAcks: function (topic, message, packet, cb) {
          var code = 0
          if (topic === 'a/b') {
            code = 34535
          }
          cb(code)
        }
      }

      serverErr.once('client', function (c) {
        c.once('subscribe', function () {
          c.publish({ topic: 'a/b', payload: 'payload', qos: 2, messageId: 1 })
        })
      })

      var client = mqtt.connect(opts)
      client.on('error', function (error) {
        should(error.message).be.equal('Wrong reason code for pubrec')
        client.end()
        serverErr.close()
        done()
      })
      client.once('connect', function () {
        client.subscribe('a/b', {qos: 1})
      })
    })
  })
})
