'use strict'

/**
 * Testing dependencies
 */
var should = require('should')
var sinon = require('sinon')
var mqtt = require('../')
var xtend = require('xtend')
var Server = require('./server')
var Store = require('./../lib/store')
var port = 9876

module.exports = function (server, config) {
  var version = config.protocolVersion || 4
  function connect (opts) {
    opts = xtend(config, opts)
    return mqtt.connect(opts)
  }

  describe('closing', function () {
    it('should emit close if stream closes', function (done) {
      var client = connect()

      client.once('connect', function () {
        client.stream.end()
      })
      client.once('close', function () {
        client.end()
        done()
      })
    })

    it('should mark the client as disconnected', function (done) {
      var client = connect()

      client.once('close', function () {
        client.end()
        if (!client.connected) {
          done()
        } else {
          done(new Error('Not marked as disconnected'))
        }
      })
      client.once('connect', function () {
        client.stream.end()
      })
    })

    it('should stop ping timer if stream closes', function (done) {
      var client = connect()

      client.once('close', function () {
        should.not.exist(client.pingTimer)
        client.end()
        done()
      })

      client.once('connect', function () {
        should.exist(client.pingTimer)
        client.stream.end()
      })
    })

    it('should emit close after end called', function (done) {
      var client = connect()

      client.once('close', function () {
        done()
      })

      client.once('connect', function () {
        client.end()
      })
    })

    it('should emit end after end called and client must be disconnected', function (done) {
      var client = connect()

      client.once('end', function () {
        if (client.disconnected) {
          return done()
        }
        done(new Error('client must be disconnected'))
      })

      client.once('connect', function () {
        client.end()
      })
    })

    it('should pass store close error to end callback but not to end listeners', function (done) {
      var store = new Store()
      var client = connect({outgoingStore: store})

      store.close = function (cb) {
        cb(new Error('test'))
      }
      client.once('end', function () {
        if (arguments.length === 0) {
          return done()
        }
        throw new Error('no argument shoould be passed to event')
      })

      client.once('connect', function () {
        client.end(function (test) {
          if (test && test.message === 'test') {
            return
          }
          throw new Error('bad argument passed to callback')
        })
      })
    })

    it('should return `this` if end called twice', function (done) {
      var client = connect()

      client.once('connect', function () {
        client.end()
        var value = client.end()
        if (value === client) {
          done()
        } else {
          done(new Error('Not returning client.'))
        }
      })
    })

    it('should emit end only on first client end', function (done) {
      var client = connect()

      client.once('end', function () {
        var timeout = setTimeout(done.bind(null), 200)
        client.once('end', function () {
          clearTimeout(timeout)
          done(new Error('end was emitted twice'))
        })
        client.end()
      })

      client.once('connect', client.end.bind(client))
    })

    it('should stop ping timer after end called', function (done) {
      var client = connect()

      client.once('connect', function () {
        should.exist(client.pingTimer)
        client.end()
        should.not.exist(client.pingTimer)
        done()
      })
    })

    it('should be able to end even on a failed connection', function (done) {
      var client = connect({host: 'this_hostname_should_not_exist'})

      var timeout = setTimeout(function () {
        done(new Error('Failed to end a disconnected client'))
      }, 500)

      setTimeout(function () {
        client.end(function () {
          clearTimeout(timeout)
          done()
        })
      }, 200)
    })

    it('should emit end even on a failed connection', function (done) {
      var client = connect({host: 'this_hostname_should_not_exist'})

      var timeout = setTimeout(function () {
        done(new Error('Disconnected client has failed to emit end'))
      }, 500)

      client.once('end', function () {
        clearTimeout(timeout)
        done()
      })

      setTimeout(client.end.bind(client), 200)
    })

    it('should emit end only once for a reconnecting client', function (done) {
      var client = connect({host: 'this_hostname_should_not_exist', connectTimeout: 10, reconnectPeriod: 10})

      client.once('end', function () {
        var timeout = setTimeout(done.bind(null))
        client.once('end', function () {
          clearTimeout(timeout)
          done(new Error('end emitted twice'))
        })
      })

      setTimeout(client.end.bind(client), 300)
    })
  })

  describe('connecting', function () {
    it('should connect to the broker', function (done) {
      var client = connect()
      client.on('error', done)

      server.once('client', function () {
        client.end()
        done()
      })
    })

    it('should send a default client id', function (done) {
      var client = connect()
      client.on('error', done)

      server.once('client', function (serverClient) {
        serverClient.once('connect', function (packet) {
          packet.clientId.should.match(/mqttjs.*/)
          serverClient.disconnect()
          done()
        })
      })
    })

    it('should send be clean by default', function (done) {
      var client = connect()
      client.on('error', done)

      server.once('client', function (serverClient) {
        serverClient.once('connect', function (packet) {
          packet.clean.should.be.true()
          serverClient.disconnect()
          done()
        })
      })
    })

    it('should connect with the given client id', function (done) {
      var client = connect({clientId: 'testclient'})
      client.on('error', function (err) {
        throw err
      })

      server.once('client', function (serverClient) {
        serverClient.once('connect', function (packet) {
          packet.clientId.should.match(/testclient/)
          serverClient.disconnect()
          done()
        })
      })
    })

    it('should connect with the client id and unclean state', function (done) {
      var client = connect({clientId: 'testclient', clean: false})
      client.on('error', function (err) {
        throw err
      })

      server.once('client', function (serverClient) {
        serverClient.once('connect', function (packet) {
          packet.clientId.should.match(/testclient/)
          packet.clean.should.be.false()
          serverClient.disconnect()
          done()
        })
      })
    })

    it('should require a clientId with clean=false', function (done) {
      try {
        var client = connect({ clean: false })
        client.on('error', function (err) {
          done(err)
          // done(new Error('should have thrown'));
        })
      } catch (err) {
        done()
      }
    })

    it('should default to localhost', function (done) {
      var client = connect({clientId: 'testclient'})
      client.on('error', function (err) {
        throw err
      })

      server.once('client', function (serverClient) {
        serverClient.once('connect', function (packet) {
          packet.clientId.should.match(/testclient/)
          serverClient.disconnect()
          done()
        })
      })
    })

    it('should emit connect', function (done) {
      var client = connect()
      client.once('connect', function () {
        client.end()
        done()
      })
      client.once('error', done)
    })

    it('should provide connack packet with connect event', function (done) {
      var connack = version === 5 ? {reasonCode: 0} : {returnCode: 0}
      server.once('client', function (serverClient) {
        connack.sessionPresent = true
        serverClient.connack(connack)
        server.once('client', function (serverClient) {
          connack.sessionPresent = false
          serverClient.connack(connack)
        })
      })

      var client = connect()
      client.once('connect', function (packet) {
        should(packet.sessionPresent).be.equal(true)
        client.once('connect', function (packet) {
          should(packet.sessionPresent).be.equal(false)
          client.end()
          done()
        })
      })
    })

    it('should mark the client as connected', function (done) {
      var client = connect()
      client.once('connect', function () {
        client.end()
        if (client.connected) {
          done()
        } else {
          done(new Error('Not marked as connected'))
        }
      })
    })

    it('should emit error', function (done) {
      var client = connect({clientId: 'invalid'})
      client.once('connect', function () {
        done(new Error('Should not emit connect'))
      })
      client.once('error', function (error) {
        var value = version === 5 ? 128 : 2
        should(error.code).be.equal(value) // code for clientID identifer rejected
        client.end()
        done()
      })
    })

    it('should have different client ids', function (done) {
      var client1 = connect()
      var client2 = connect()

      client1.options.clientId.should.not.equal(client2.options.clientId)
      client1.end(true)
      client2.end(true)
      setImmediate(done)
    })
  })

  describe('handling offline states', function () {
    it('should emit offline events once when the client transitions from connected states to disconnected ones', function (done) {
      var client = connect({reconnectPeriod: 20})

      client.on('connect', function () {
        this.stream.end()
      })

      client.on('offline', function () {
        client.end(true, done)
      })
    })

    it('should emit offline events once when the client (at first) can NOT connect to servers', function (done) {
      // fake a port
      var client = connect({ reconnectPeriod: 20, port: 4557 })

      client.on('offline', function () {
        client.end(true, done)
      })
    })
  })

  describe('topic validations when subscribing', function () {
    it('should be ok for well-formated topics', function (done) {
      var client = connect()
      client.subscribe(
        [
          '+', '+/event', 'event/+', '#', 'event/#', 'system/event/+',
          'system/+/event', 'system/registry/event/#', 'system/+/event/#',
          'system/registry/event/new_device', 'system/+/+/new_device'
        ],
        function (err) {
          client.end(function () {
            if (err) {
              return done(new Error(err))
            }
            done()
          })
        }
      )
    })

    it('should return an error (via callbacks) for topic #/event', function (done) {
      var client = connect()
      client.subscribe(['#/event', 'event#', 'event+'], function (err) {
        client.end(false, function () {
          if (err) {
            return done()
          }
          done(new Error('Validations do NOT work'))
        })
      })
    })

    it('should return an empty array for duplicate subs', function (done) {
      var client = connect()
      client.subscribe('event', function (err, granted1) {
        if (err) {
          return done(err)
        }
        client.subscribe('event', function (err, granted2) {
          if (err) {
            return done(err)
          }
          granted2.should.Array()
          granted2.should.be.empty()
          done()
        })
      })
    })

    it('should return an error (via callbacks) for topic #/event', function (done) {
      var client = connect()
      client.subscribe('#/event', function (err) {
        client.end(function () {
          if (err) {
            return done()
          }
          done(new Error('Validations do NOT work'))
        })
      })
    })

    it('should return an error (via callbacks) for topic event#', function (done) {
      var client = connect()
      client.subscribe('event#', function (err) {
        client.end(function () {
          if (err) {
            return done()
          }
          done(new Error('Validations do NOT work'))
        })
      })
    })

    it('should return an error (via callbacks) for topic system/#/event', function (done) {
      var client = connect()
      client.subscribe('system/#/event', function (err) {
        client.end(function () {
          if (err) {
            return done()
          }
          done(new Error('Validations do NOT work'))
        })
      })
    })

    it('should return an error (via callbacks) for empty topic list', function (done) {
      var client = connect()
      client.subscribe([], function (err) {
        client.end()
        if (err) {
          return done()
        }
        done(new Error('Validations do NOT work'))
      })
    })

    it('should return an error (via callbacks) for topic system/+/#/event', function (done) {
      var client = connect()
      client.subscribe('system/+/#/event', function (err) {
        client.end(true, function () {
          if (err) {
            return done()
          }
          done(new Error('Validations do NOT work'))
        })
      })
    })
  })

  describe('offline messages', function () {
    it('should queue message until connected', function (done) {
      var client = connect()

      client.publish('test', 'test')
      client.subscribe('test')
      client.unsubscribe('test')
      client.queue.length.should.equal(3)

      client.once('connect', function () {
        client.queue.length.should.equal(0)
        setTimeout(function () {
          client.end(true, done)
        }, 10)
      })
    })

    it('should not queue qos 0 messages if queueQoSZero is false', function (done) {
      var client = connect({queueQoSZero: false})

      client.publish('test', 'test', {qos: 0})
      client.queue.length.should.equal(0)
      client.on('connect', function () {
        setTimeout(function () {
          client.end(true, done)
        }, 10)
      })
    })

    it('should queue qos != 0 messages', function (done) {
      var client = connect({queueQoSZero: false})

      client.publish('test', 'test', {qos: 1})
      client.publish('test', 'test', {qos: 2})
      client.subscribe('test')
      client.unsubscribe('test')
      client.queue.length.should.equal(2)
      client.on('connect', function () {
        setTimeout(function () {
          client.end(true, done)
        }, 10)
      })
    })

    it('should not interrupt messages', function (done) {
      var client = null
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var publishCount = 0
      var server2 = new Server(function (c) {
        c.on('connect', function () {
          c.connack({returnCode: 0})
        })
        c.on('publish', function (packet) {
          if (packet.qos !== 0) {
            c.puback({messageId: packet.messageId})
          }
          switch (publishCount++) {
            case 0:
              packet.payload.toString().should.equal('payload1')
              break
            case 1:
              packet.payload.toString().should.equal('payload2')
              break
            case 2:
              packet.payload.toString().should.equal('payload3')
              break
            case 3:
              packet.payload.toString().should.equal('payload4')
              server2.close()
              done()
              break
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore,
          queueQoSZero: true
        })
        client.on('packetreceive', function (packet) {
          if (packet.cmd === 'connack') {
            setImmediate(
              function () {
                client.publish('test', 'payload3', {qos: 1})
                client.publish('test', 'payload4', {qos: 0})
              }
            )
          }
        })
        client.publish('test', 'payload1', {qos: 2})
        client.publish('test', 'payload2', {qos: 2})
      })
    })

    it('should call cb if an outgoing QoS 0 message is not sent', function (done) {
      var client = connect({queueQoSZero: false})
      var called = false

      client.publish('test', 'test', {qos: 0}, function () {
        called = true
      })

      client.on('connect', function () {
        called.should.equal(true)
        setTimeout(function () {
          client.end(true, done)
        }, 10)
      })
    })

    it('should delay ending up until all inflight messages are delivered', function (done) {
      var client = connect()
      var subscribeCalled = false

      client.on('connect', function () {
        client.subscribe('test', function () {
          subscribeCalled = true
        })
        client.publish('test', 'test', function () {
          client.end(false, function () {
            subscribeCalled.should.be.equal(true)
            done()
          })
        })
      })
    })

    it('wait QoS 1 publish messages', function (done) {
      var client = connect()
      var messageReceived = false

      client.on('connect', function () {
        client.subscribe('test')
        client.publish('test', 'test', { qos: 1 }, function () {
          client.end(false, function () {
            messageReceived.should.equal(true)
            done()
          })
        })
        client.on('message', function () {
          messageReceived = true
        })
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.on('publish', function (packet) {
            serverClient.publish(packet)
          })
        })
      })
    })

    it('does not wait acks when force-closing', function (done) {
      // non-running broker
      var client = connect('mqtt://localhost:8993')
      client.publish('test', 'test', { qos: 1 })
      client.end(true, done)
    })

    it('should call cb if store.put fails', function (done) {
      const store = new Store()
      store.put = function (packet, cb) {
        process.nextTick(cb, new Error('oops there is an error'))
      }
      var client = connect({ incomingStore: store, outgoingStore: store })
      client.publish('test', 'test', { qos: 2 }, function (err) {
        if (err) {
          client.end(true, done)
        }
      })
    })
  })

  describe('publishing', function () {
    it('should publish a message (offline)', function (done) {
      var client = connect()
      var payload = 'test'
      var topic = 'test'

      client.publish(topic, payload)

      server.on('client', onClient)

      function onClient (serverClient) {
        serverClient.once('connect', function () {
          server.removeListener('client', onClient)
        })

        serverClient.once('publish', function (packet) {
          packet.topic.should.equal(topic)
          packet.payload.toString().should.equal(payload)
          packet.qos.should.equal(0)
          packet.retain.should.equal(false)
          client.end(true, done)
        })
      }
    })

    it('should publish a message (online)', function (done) {
      var client = connect()
      var payload = 'test'
      var topic = 'test'

      client.on('connect', function () {
        client.publish(topic, payload)
      })

      server.once('client', function (serverClient) {
        serverClient.once('publish', function (packet) {
          packet.topic.should.equal(topic)
          packet.payload.toString().should.equal(payload)
          packet.qos.should.equal(0)
          packet.retain.should.equal(false)
          client.end()
          done()
        })
      })
    })

    it('should publish a message (retain, offline)', function (done) {
      var client = connect({ queueQoSZero: true })
      var payload = 'test'
      var topic = 'test'
      var called = false

      client.publish(topic, payload, { retain: true }, function () {
        called = true
      })

      server.once('client', function (serverClient) {
        serverClient.once('publish', function (packet) {
          packet.topic.should.equal(topic)
          packet.payload.toString().should.equal(payload)
          packet.qos.should.equal(0)
          packet.retain.should.equal(true)
          called.should.equal(true)
          client.end()
          done()
        })
      })
    })

    it('should emit a packetsend event', function (done) {
      var client = connect()
      var payload = 'test_payload'
      var testTopic = 'testTopic'

      client.on('packetsend', function (packet) {
        if (packet.cmd === 'publish') {
          packet.qos.should.equal(0)
          packet.topic.should.equal(testTopic)
          packet.payload.should.equal(payload)
          packet.retain.should.equal(false)
          client.end()
          done()
        }
      })

      client.publish(testTopic, payload)
    })

    it('should accept options', function (done) {
      var client = connect()
      var payload = 'test'
      var topic = 'test'
      var opts = {
        retain: true,
        qos: 1
      }

      client.once('connect', function () {
        client.publish(topic, payload, opts)
      })

      server.once('client', function (serverClient) {
        serverClient.once('publish', function (packet) {
          packet.topic.should.equal(topic)
          packet.payload.toString().should.equal(payload)
          packet.qos.should.equal(opts.qos, 'incorrect qos')
          packet.retain.should.equal(opts.retain, 'incorrect ret')
          packet.dup.should.equal(false, 'incorrect dup')
          client.end()
          done()
        })
      })
    })

    it('should publish with the default options for an empty parameter', function (done) {
      var client = connect()
      var payload = 'test'
      var topic = 'test'
      var defaultOpts = {qos: 0, retain: false, dup: false}

      client.once('connect', function () {
        client.publish(topic, payload, {})
      })

      server.once('client', function (serverClient) {
        serverClient.once('publish', function (packet) {
          packet.topic.should.equal(topic)
          packet.payload.toString().should.equal(payload)
          packet.qos.should.equal(defaultOpts.qos, 'incorrect qos')
          packet.retain.should.equal(defaultOpts.retain, 'incorrect ret')
          packet.dup.should.equal(defaultOpts.dup, 'incorrect dup')
          client.end()
          done()
        })
      })
    })

    it('should mark a message as  duplicate when "dup" option is set', function (done) {
      var client = connect()
      var payload = 'duplicated-test'
      var topic = 'test'
      var opts = {
        retain: true,
        qos: 1,
        dup: true
      }

      client.once('connect', function () {
        client.publish(topic, payload, opts)
      })

      server.once('client', function (serverClient) {
        serverClient.once('publish', function (packet) {
          packet.topic.should.equal(topic)
          packet.payload.toString().should.equal(payload)
          packet.dup.should.equal(opts.dup, 'incorrect dup')
          client.end()
          done()
        })
      })
    })

    it('should fire a callback (qos 0)', function (done) {
      var client = connect()

      client.once('connect', function () {
        client.publish('a', 'b', function () {
          client.end()
          done()
        })
      })
    })

    it('should fire a callback (qos 1)', function (done) {
      var client = connect()
      var opts = { qos: 1 }

      client.once('connect', function () {
        client.publish('a', 'b', opts, function () {
          client.end()
          done()
        })
      })
    })

    it('should fire a callback (qos 2)', function (done) {
      var client = connect()
      var opts = { qos: 2 }

      client.once('connect', function () {
        client.publish('a', 'b', opts, function () {
          client.end()
          done()
        })
      })
    })

    it('should support UTF-8 characters in topic', function (done) {
      var client = connect()

      client.once('connect', function () {
        client.publish('中国', 'hello', function () {
          client.end()
          done()
        })
      })
    })

    it('should support UTF-8 characters in payload', function (done) {
      var client = connect()

      client.once('connect', function () {
        client.publish('hello', '中国', function () {
          client.end()
          done()
        })
      })
    })

    it('should publish 10 QoS 2 and receive them', function (done) {
      var client = connect()
      var count = 0

      client.on('connect', function () {
        client.subscribe('test')
        client.publish('test', 'test', { qos: 2 })
      })

      client.on('message', function () {
        if (count >= 10) {
          client.end()
          done()
        } else {
          client.publish('test', 'test', { qos: 2 })
        }
      })

      server.once('client', function (serverClient) {
        serverClient.on('offline', function () {
          client.end()
          done('error went offline... didnt see this happen')
        })

        serverClient.on('subscribe', function () {
          serverClient.on('publish', function (packet) {
            serverClient.publish(packet)
          })
        })

        serverClient.on('pubrel', function () {
          count++
        })
      })
    })

    function testQosHandleMessage (qos, done) {
      var client = connect()

      var messageEventCount = 0
      var handleMessageCount = 0

      client.handleMessage = function (packet, callback) {
        setTimeout(function () {
          handleMessageCount++
          // next message event should not emit until handleMessage completes
          handleMessageCount.should.equal(messageEventCount)
          if (handleMessageCount === 10) {
            setTimeout(function () {
              client.end()
              done()
            })
          }
          callback()
        }, 100)
      }

      client.on('message', function (topic, message, packet) {
        messageEventCount++
      })

      client.on('connect', function () {
        client.subscribe('test')
      })

      server.once('client', function (serverClient) {
        serverClient.on('offline', function () {
          client.end()
          done('error went offline... didnt see this happen')
        })

        serverClient.on('subscribe', function () {
          for (var i = 0; i < 10; i++) {
            serverClient.publish({
              messageId: i,
              topic: 'test',
              payload: 'test' + i,
              qos: qos
            })
          }
        })
      })
    }

    it('should publish 10 QoS 0 and receive them only when `handleMessage` finishes', function (done) {
      testQosHandleMessage(0, done)
    })

    it('should publish 10 QoS 1 and receive them only when `handleMessage` finishes', function (done) {
      testQosHandleMessage(1, done)
    })

    it('should publish 10 QoS 2 and receive them only when `handleMessage` finishes', function (done) {
      testQosHandleMessage(2, done)
    })

    it('should not send a `puback` if the execution of `handleMessage` fails for messages with QoS `1`', function (done) {
      var client = connect()

      client.handleMessage = function (packet, callback) {
        callback(new Error('Error thrown by the application'))
      }

      client._sendPacket = sinon.spy()

      client._handlePublish({
        messageId: Math.floor(65535 * Math.random()),
        topic: 'test',
        payload: 'test',
        qos: 1
      }, function (err) {
        should.exist(err)
      })

      client._sendPacket.callCount.should.equal(0)
      client.end()
      client.on('connect', function () { done() })
    })

    it('should silently ignore errors thrown by `handleMessage` and return when no callback is passed ' +
      'into `handlePublish` method', function (done) {
      var client = connect()

      client.handleMessage = function (packet, callback) {
        callback(new Error('Error thrown by the application'))
      }

      try {
        client._handlePublish({
          messageId: Math.floor(65535 * Math.random()),
          topic: 'test',
          payload: 'test',
          qos: 1
        })
        done()
      } catch (err) {
        done(err)
      } finally {
        client.end()
      }
    })

    it('should handle error with async incoming store in QoS 2 `handlePublish` method', function (done) {
      function AsyncStore () {
        if (!(this instanceof AsyncStore)) {
          return new AsyncStore()
        }
      }
      AsyncStore.prototype.put = function (packet, cb) {
        process.nextTick(function () {
          cb(new Error('Error'))
        })
      }
      var store = new AsyncStore()
      var client = connect({incomingStore: store})

      client._handlePublish({
        messageId: 1,
        topic: 'test',
        payload: 'test',
        qos: 2
      }, function () {
        done()
        client.end()
      })
    })

    it('should handle error with async incoming store in QoS 2 `handlePubrel` method', function (done) {
      function AsyncStore () {
        if (!(this instanceof AsyncStore)) {
          return new AsyncStore()
        }
      }
      AsyncStore.prototype.del = function (packet, cb) {
        process.nextTick(function () {
          cb(new Error('Error'))
        })
      }
      AsyncStore.prototype.get = function (packet, cb) {
        process.nextTick(function () {
          cb(null, {cmd: 'publish'})
        })
      }
      var store = new AsyncStore()
      var client = connect({incomingStore: store})

      client._handlePubrel({
        messageId: 1,
        qos: 2
      }, function () {
        done()
        client.end()
      })
    })

    it('should handle success with async incoming store in QoS 2 `handlePubrel` method', function (done) {
      var delComplete = false
      function AsyncStore () {
        if (!(this instanceof AsyncStore)) {
          return new AsyncStore()
        }
      }
      AsyncStore.prototype.del = function (packet, cb) {
        process.nextTick(function () {
          delComplete = true
          cb(null)
        })
      }
      AsyncStore.prototype.get = function (packet, cb) {
        process.nextTick(function () {
          cb(null, {cmd: 'publish'})
        })
      }
      var store = new AsyncStore()
      var client = connect({incomingStore: store})

      client._handlePubrel({
        messageId: 1,
        qos: 2
      }, function () {
        delComplete.should.equal(true)
        done()
        client.end()
      })
    })

    it('should handle error with async incoming store in QoS 1 `handlePublish` method', function (done) {
      function AsyncStore () {
        if (!(this instanceof AsyncStore)) {
          return new AsyncStore()
        }
      }
      AsyncStore.prototype.put = function (packet, cb) {
        process.nextTick(function () {
          cb(null, 'Error')
        })
      }
      var store = new AsyncStore()
      var client = connect({incomingStore: store})

      client._handlePublish({
        messageId: 1,
        topic: 'test',
        payload: 'test',
        qos: 1
      }, function () {
        done()
        client.end()
      })
    })

    it('should not send a `pubcomp` if the execution of `handleMessage` fails for messages with QoS `2`', function (done) {
      var store = new Store()
      var client = connect({incomingStore: store})

      var messageId = Math.floor(65535 * Math.random())
      var topic = 'test'
      var payload = 'test'
      var qos = 2

      client.handleMessage = function (packet, callback) {
        callback(new Error('Error thrown by the application'))
      }

      client.once('connect', function () {
        client.subscribe(topic, {qos: 2})

        store.put({
          messageId: messageId,
          topic: topic,
          payload: payload,
          qos: qos,
          cmd: 'publish'
        }, function () {
          // cleans up the client
          client.end()

          client._sendPacket = sinon.spy()
          client._handlePubrel({cmd: 'pubrel', messageId: messageId}, function (err) {
            should.exist(err)
          })
          client._sendPacket.callCount.should.equal(0)
          done()
        })
      })
    })

    it('should silently ignore errors thrown by `handleMessage` and return when no callback is passed ' +
      'into `handlePubrel` method', function (done) {
      var store = new Store()
      var client = connect({incomingStore: store})

      var messageId = Math.floor(65535 * Math.random())
      var topic = 'test'
      var payload = 'test'
      var qos = 2

      client.handleMessage = function (packet, callback) {
        callback(new Error('Error thrown by the application'))
      }

      client.once('connect', function () {
        client.subscribe(topic, {qos: 2})

        store.put({
          messageId: messageId,
          topic: topic,
          payload: payload,
          qos: qos,
          cmd: 'publish'
        }, function () {
          try {
            client._handlePubrel({cmd: 'pubrel', messageId: messageId})
            done()
          } catch (err) {
            done(err)
          } finally {
            client.end()
          }
        })
      })
    })

    it('should keep message order', function (done) {
      var publishCount = 0
      var reconnect = false
      var client = {}
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var server2 = new Server(function (c) {
        // errors are not interesting for this test
        // but they might happen on some platforms
        c.on('error', function () {})

        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('publish', function (packet) {
          c.puback({messageId: packet.messageId})
          if (reconnect) {
            switch (publishCount++) {
              case 0:
                packet.payload.toString().should.equal('payload1')
                break
              case 1:
                packet.payload.toString().should.equal('payload2')
                break
              case 2:
                packet.payload.toString().should.equal('payload3')
                server2.close()
                done()
                break
            }
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore
        })

        client.on('connect', function () {
          if (!reconnect) {
            client.publish('topic', 'payload1', {qos: 1})
            client.publish('topic', 'payload2', {qos: 1})
            client.end(true)
          } else {
            client.publish('topic', 'payload3', {qos: 1})
          }
        })
        client.on('close', function () {
          if (!reconnect) {
            client.reconnect({
              clean: false,
              incomingStore: incomingStore,
              outgoingStore: outgoingStore
            })
            reconnect = true
          }
        })
      })
    })

    function testCallbackStorePutByQoS (qos, clean, expected, done) {
      var client = connect({
        clean: clean,
        clientId: 'testId'
      })

      var callbacks = []

      function cbStorePut () {
        callbacks.push('storeput')
      }

      client.on('connect', function () {
        client.publish('test', 'test', {qos: qos, cbStorePut: cbStorePut}, function (err) {
          if (err) done(err)
          callbacks.push('publish')
          should.deepEqual(callbacks, expected)
          done()
        })
        client.end()
      })
    }

    it('should not call cbStorePut when publishing message with QoS `0` and clean `true`', function (done) {
      testCallbackStorePutByQoS(0, true, ['publish'], done)
    })
    it('should not call cbStorePut when publishing message with QoS `0` and clean `false`', function (done) {
      testCallbackStorePutByQoS(0, false, ['publish'], done)
    })
    it('should call cbStorePut before publish completes when publishing message with QoS `1` and clean `true`', function (done) {
      testCallbackStorePutByQoS(1, true, ['storeput', 'publish'], done)
    })
    it('should call cbStorePut before publish completes when publishing message with QoS `1` and clean `false`', function (done) {
      testCallbackStorePutByQoS(1, false, ['storeput', 'publish'], done)
    })
    it('should call cbStorePut before publish completes when publishing message with QoS `2` and clean `true`', function (done) {
      testCallbackStorePutByQoS(2, true, ['storeput', 'publish'], done)
    })
    it('should call cbStorePut before publish completes when publishing message with QoS `2` and clean `false`', function (done) {
      testCallbackStorePutByQoS(2, false, ['storeput', 'publish'], done)
    })
  })

  describe('unsubscribing', function () {
    it('should send an unsubscribe packet (offline)', function (done) {
      var client = connect()

      client.unsubscribe('test')

      server.once('client', function (serverClient) {
        serverClient.once('unsubscribe', function (packet) {
          packet.unsubscriptions.should.containEql('test')
          client.end()
          done()
        })
      })
    })

    it('should send an unsubscribe packet', function (done) {
      var client = connect()
      var topic = 'topic'

      client.once('connect', function () {
        client.unsubscribe(topic)
      })

      server.once('client', function (serverClient) {
        serverClient.once('unsubscribe', function (packet) {
          packet.unsubscriptions.should.containEql(topic)
          client.end()
          done()
        })
      })
    })

    it('should emit a packetsend event', function (done) {
      var client = connect()
      var testTopic = 'testTopic'

      client.once('connect', function () {
        client.subscribe(testTopic)
      })

      client.on('packetsend', function (packet) {
        if (packet.cmd === 'subscribe') {
          client.end()
          done()
        }
      })
    })

    it('should emit a packetreceive event', function (done) {
      var client = connect()
      var testTopic = 'testTopic'

      client.once('connect', function () {
        client.subscribe(testTopic)
      })

      client.on('packetreceive', function (packet) {
        if (packet.cmd === 'suback') {
          client.end()
          done()
        }
      })
    })

    it('should accept an array of unsubs', function (done) {
      var client = connect()
      var topics = ['topic1', 'topic2']

      client.once('connect', function () {
        client.unsubscribe(topics)
      })

      server.once('client', function (serverClient) {
        serverClient.once('unsubscribe', function (packet) {
          packet.unsubscriptions.should.eql(topics)
          done()
        })
      })
    })

    it('should fire a callback on unsuback', function (done) {
      var client = connect()
      var topic = 'topic'

      client.once('connect', function () {
        client.unsubscribe(topic, done)
      })

      server.once('client', function (serverClient) {
        serverClient.once('unsubscribe', function (packet) {
          serverClient.unsuback(packet)
          client.end()
        })
      })
    })

    it('should unsubscribe from a chinese topic', function (done) {
      var client = connect()
      var topic = '中国'

      client.once('connect', function () {
        client.unsubscribe(topic)
      })

      server.once('client', function (serverClient) {
        serverClient.once('unsubscribe', function (packet) {
          packet.unsubscriptions.should.containEql(topic)
          client.end()
          done()
        })
      })
    })
  })

  describe('keepalive', function () {
    var clock

    beforeEach(function () {
      clock = sinon.useFakeTimers()
    })

    afterEach(function () {
      clock.restore()
    })

    it('should checkPing at keepalive interval', function (done) {
      var interval = 3
      var client = connect({ keepalive: interval })

      client._checkPing = sinon.spy()

      client.once('connect', function () {
        clock.tick(interval * 1000)
        client._checkPing.callCount.should.equal(1)

        clock.tick(interval * 1000)
        client._checkPing.callCount.should.equal(2)

        clock.tick(interval * 1000)
        client._checkPing.callCount.should.equal(3)

        client.end()
        done()
      })
    })

    it('should not checkPing if publishing at a higher rate than keepalive', function (done) {
      var intervalMs = 3000
      var client = connect({keepalive: intervalMs / 1000})

      client._checkPing = sinon.spy()

      client.once('connect', function () {
        client.publish('foo', 'bar')
        clock.tick(intervalMs - 1)
        client.publish('foo', 'bar')
        clock.tick(2)
        client._checkPing.callCount.should.equal(0)
        client.end()
        done()
      })
    })

    it('should checkPing if publishing at a higher rate than keepalive and reschedulePings===false', function (done) {
      var intervalMs = 3000
      var client = connect({
        keepalive: intervalMs / 1000,
        reschedulePings: false
      })

      client._checkPing = sinon.spy()

      client.once('connect', function () {
        client.publish('foo', 'bar')
        clock.tick(intervalMs - 1)
        client.publish('foo', 'bar')
        clock.tick(2)
        client._checkPing.callCount.should.equal(1)
        client.end()
        done()
      })
    })
  })

  describe('pinging', function () {
    it('should set a ping timer', function (done) {
      var client = connect({keepalive: 3})
      client.once('connect', function () {
        should.exist(client.pingTimer)
        client.end()
        done()
      })
    })

    it('should not set a ping timer keepalive=0', function (done) {
      var client = connect({keepalive: 0})
      client.on('connect', function () {
        should.not.exist(client.pingTimer)
        client.end()
        done()
      })
    })

    it('should reconnect if pingresp is not sent', function (done) {
      var client = connect({keepalive: 1, reconnectPeriod: 100})

      // Fake no pingresp being send by stubbing the _handlePingresp function
      client._handlePingresp = function () {}

      client.once('connect', function () {
        client.once('connect', function () {
          client.end()
          done()
        })
      })
    })

    it('should not reconnect if pingresp is successful', function (done) {
      var client = connect({keepalive: 100})
      client.once('close', function () {
        done(new Error('Client closed connection'))
      })
      setTimeout(done, 1000)
    })

    it('should defer the next ping when sending a control packet', function (done) {
      var client = connect({keepalive: 1})

      client.once('connect', function () {
        client._checkPing = sinon.spy()

        client.publish('foo', 'bar')
        setTimeout(function () {
          client._checkPing.callCount.should.equal(0)
          client.publish('foo', 'bar')

          setTimeout(function () {
            client._checkPing.callCount.should.equal(0)
            client.publish('foo', 'bar')

            setTimeout(function () {
              client._checkPing.callCount.should.equal(0)
              done()
            }, 75)
          }, 75)
        }, 75)
      })
    })
  })

  describe('subscribing', function () {
    it('should send a subscribe message (offline)', function (done) {
      var client = connect()

      client.subscribe('test')

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function () {
          done()
        })
      })
    })

    it('should send a subscribe message', function (done) {
      var client = connect()
      var topic = 'test'

      client.once('connect', function () {
        client.subscribe(topic)
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function (packet) {
          var result = {
            topic: topic,
            qos: 0
          }
          if (version === 5) {
            result.nl = false
            result.rap = false
            result.rh = 0
          }
          packet.subscriptions.should.containEql(result)
          done()
        })
      })
    })

    it('should emit a packetsend event', function (done) {
      var client = connect()
      var testTopic = 'testTopic'

      client.once('connect', function () {
        client.subscribe(testTopic)
      })

      client.on('packetsend', function (packet) {
        if (packet.cmd === 'subscribe') {
          done()
        }
      })
    })

    it('should emit a packetreceive event', function (done) {
      var client = connect()
      var testTopic = 'testTopic'

      client.once('connect', function () {
        client.subscribe(testTopic)
      })

      client.on('packetreceive', function (packet) {
        if (packet.cmd === 'suback') {
          done()
        }
      })
    })

    it('should accept an array of subscriptions', function (done) {
      var client = connect()
      var subs = ['test1', 'test2']

      client.once('connect', function () {
        client.subscribe(subs)
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function (packet) {
          // i.e. [{topic: 'a', qos: 0}, {topic: 'b', qos: 0}]
          var expected = subs.map(function (i) {
            var result = {topic: i, qos: 0}
            if (version === 5) {
              result.nl = false
              result.rap = false
              result.rh = 0
            }
            return result
          })

          packet.subscriptions.should.eql(expected)
          done()
        })
      })
    })

    it('should accept an hash of subscriptions', function (done) {
      var client = connect()
      var topics = {
        test1: {qos: 0},
        test2: {qos: 1}
      }

      client.once('connect', function () {
        client.subscribe(topics)
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function (packet) {
          var k
          var expected = []

          for (k in topics) {
            if (topics.hasOwnProperty(k)) {
              var result = {
                topic: k,
                qos: topics[k].qos
              }
              if (version === 5) {
                result.nl = false
                result.rap = false
                result.rh = 0
              }
              expected.push(result)
            }
          }

          packet.subscriptions.should.eql(expected)
          done()
        })
      })
    })

    it('should accept an options parameter', function (done) {
      var client = connect()
      var topic = 'test'
      var opts = {qos: 1}

      client.once('connect', function () {
        client.subscribe(topic, opts)
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function (packet) {
          var expected = [{
            topic: topic,
            qos: 1
          }]

          if (version === 5) {
            expected[0].nl = false
            expected[0].rap = false
            expected[0].rh = 0
          }

          packet.subscriptions.should.eql(expected)
          done()
        })
      })
    })

    it('should subscribe with the default options for an empty options parameter', function (done) {
      var client = connect()
      var topic = 'test'
      var defaultOpts = {qos: 0}

      client.once('connect', function () {
        client.subscribe(topic, {})
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function (packet) {
          var result = {
            topic: topic,
            qos: defaultOpts.qos
          }
          if (version === 5) {
            result.nl = false
            result.rap = false
            result.rh = 0
          }
          packet.subscriptions.should.containEql(result)
          done()
        })
      })
    })

    it('should fire a callback on suback', function (done) {
      var client = connect()
      var topic = 'test'

      client.once('connect', function () {
        client.subscribe(topic, { qos: 2 }, function (err, granted) {
          if (err) {
            done(err)
          } else {
            should.exist(granted, 'granted not given')
            var result = {topic: 'test', qos: 2}
            if (version === 5) {
              result.nl = false
              result.rap = false
              result.rh = 0
              result.properties = undefined
            }
            granted.should.containEql(result)
            done()
          }
        })
      })
    })

    it('should fire a callback with error if disconnected (options provided)', function (done) {
      var client = connect()
      var topic = 'test'
      client.once('connect', function () {
        client.end(true, function () {
          client.subscribe(topic, {qos: 2}, function (err, granted) {
            should.not.exist(granted, 'granted given')
            should.exist(err, 'no error given')
            done()
          })
        })
      })
    })

    it('should fire a callback with error if disconnected (options not provided)', function (done) {
      var client = connect()
      var topic = 'test'

      client.once('connect', function () {
        client.end(true, function () {
          client.subscribe(topic, function (err, granted) {
            should.not.exist(granted, 'granted given')
            should.exist(err, 'no error given')
            done()
          })
        })
      })
    })

    it('should subscribe with a chinese topic', function (done) {
      var client = connect()
      var topic = '中国'

      client.once('connect', function () {
        client.subscribe(topic)
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function (packet) {
          var result = {
            topic: topic,
            qos: 0
          }
          if (version === 5) {
            result.nl = false
            result.rap = false
            result.rh = 0
          }
          packet.subscriptions.should.containEql(result)
          done()
        })
      })
    })
  })

  describe('receiving messages', function () {
    it('should fire the message event', function (done) {
      var client = connect()
      var testPacket = {
        topic: 'test',
        payload: 'message',
        retain: true,
        qos: 1,
        messageId: 5
      }

      client.subscribe(testPacket.topic)
      client.once('message', function (topic, message, packet) {
        topic.should.equal(testPacket.topic)
        message.toString().should.equal(testPacket.payload)
        packet.should.equal(packet)
        client.end()
        done()
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.publish(testPacket)
        })
      })
    })

    it('should emit a packetreceive event', function (done) {
      var client = connect()
      var testPacket = {
        topic: 'test',
        payload: 'message',
        retain: true,
        qos: 1,
        messageId: 5
      }

      client.subscribe(testPacket.topic)
      client.on('packetreceive', function (packet) {
        if (packet.cmd === 'publish') {
          packet.qos.should.equal(1)
          packet.topic.should.equal(testPacket.topic)
          packet.payload.toString().should.equal(testPacket.payload)
          packet.retain.should.equal(true)
          client.end()
          done()
        }
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.publish(testPacket)
        })
      })
    })

    it('should support binary data', function (done) {
      var client = connect({ encoding: 'binary' })
      var testPacket = {
        topic: 'test',
        payload: 'message',
        retain: true,
        qos: 1,
        messageId: 5
      }

      client.subscribe(testPacket.topic)
      client.once('message', function (topic, message, packet) {
        topic.should.equal(testPacket.topic)
        message.should.be.an.instanceOf(Buffer)
        message.toString().should.equal(testPacket.payload)
        packet.should.equal(packet)
        done()
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.publish(testPacket)
        })
      })
    })

    it('should emit a message event (qos=2)', function (done) {
      var client = connect()
      var testPacket = {
        topic: 'test',
        payload: 'message',
        retain: true,
        qos: 2,
        messageId: 5
      }

      server.testPublish = testPacket

      client.subscribe(testPacket.topic)
      client.once('message', function (topic, message, packet) {
        topic.should.equal(testPacket.topic)
        message.toString().should.equal(testPacket.payload)
        packet.should.equal(packet)
        done()
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.publish(testPacket)
        })
      })
    })

    it('should emit a message event (qos=2) - repeated publish', function (done) {
      var client = connect()
      var testPacket = {
        topic: 'test',
        payload: 'message',
        retain: true,
        qos: 2,
        messageId: 5
      }

      server.testPublish = testPacket

      client.subscribe(testPacket.topic)
      client.on('message', function (topic, message, packet) {
        topic.should.equal(testPacket.topic)
        message.toString().should.equal(testPacket.payload)
        packet.should.equal(packet)
        done()
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.publish(testPacket)
          // twice, should be ignored
          serverClient.publish(testPacket)
        })
      })
    })

    it('should support chinese topic', function (done) {
      var client = connect({ encoding: 'binary' })
      var testPacket = {
        topic: '国',
        payload: 'message',
        retain: true,
        qos: 1,
        messageId: 5
      }

      client.subscribe(testPacket.topic)
      client.once('message', function (topic, message, packet) {
        topic.should.equal(testPacket.topic)
        message.should.be.an.instanceOf(Buffer)
        message.toString().should.equal(testPacket.payload)
        packet.should.equal(packet)
        done()
      })

      server.once('client', function (serverClient) {
        serverClient.on('subscribe', function () {
          serverClient.publish(testPacket)
        })
      })
    })
  })

  describe('qos handling', function () {
    it('should follow qos 0 semantics (trivial)', function (done) {
      var client = connect()
      var testTopic = 'test'
      var testMessage = 'message'

      client.once('connect', function () {
        client.subscribe(testTopic, {qos: 0})
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function () {
          serverClient.publish({
            topic: testTopic,
            payload: testMessage,
            qos: 0,
            retain: false
          })
          done()
        })
      })
    })

    it('should follow qos 1 semantics', function (done) {
      var client = connect()
      var testTopic = 'test'
      var testMessage = 'message'
      var mid = 50

      client.once('connect', function () {
        client.subscribe(testTopic, {qos: 1})
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function () {
          serverClient.publish({
            topic: testTopic,
            payload: testMessage,
            messageId: mid,
            qos: 1
          })
        })

        serverClient.once('puback', function (packet) {
          packet.messageId.should.equal(mid)
          done()
        })
      })
    })

    it('should follow qos 2 semantics', function (done) {
      var client = connect()
      var testTopic = 'test'
      var testMessage = 'message'
      var mid = 253
      var publishReceived = false
      var pubrecReceived = false
      var pubrelReceived = false

      client.once('connect', function () {
        client.subscribe(testTopic, {qos: 2})
      })

      client.on('packetreceive', (packet) => {
        switch (packet.cmd) {
          case 'connack':
          case 'suback':
            // expected, but not specifically part of QOS 2 semantics
            break
          case 'publish':
            pubrecReceived.should.be.false()
            pubrelReceived.should.be.false()
            publishReceived = true
            break
          case 'pubrel':
            publishReceived.should.be.true()
            pubrecReceived.should.be.true()
            pubrelReceived = true
            break
          default:
            should.fail()
        }
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function () {
          serverClient.publish({
            topic: testTopic,
            payload: testMessage,
            qos: 2,
            messageId: mid
          })
        })

        serverClient.on('pubrec', function () {
          publishReceived.should.be.true()
          pubrelReceived.should.be.false()
          pubrecReceived = true
        })

        serverClient.once('pubcomp', function () {
          client.removeAllListeners()
          serverClient.removeAllListeners()
          publishReceived.should.be.true()
          pubrecReceived.should.be.true()
          pubrelReceived.should.be.true()
          done()
        })
      })
    })

    it('should should empty the incoming store after a qos 2 handshake is completed', function (done) {
      var client = connect()
      var testTopic = 'test'
      var testMessage = 'message'
      var mid = 253

      client.once('connect', function () {
        client.subscribe(testTopic, {qos: 2})
      })

      client.on('packetreceive', (packet) => {
        if (packet.cmd === 'pubrel') {
          should(client.incomingStore._inflights.size).be.equal(1)
        }
      })

      server.once('client', function (serverClient) {
        serverClient.once('subscribe', function () {
          serverClient.publish({
            topic: testTopic,
            payload: testMessage,
            qos: 2,
            messageId: mid
          })
        })

        serverClient.once('pubcomp', function () {
          should(client.incomingStore._inflights.size).be.equal(0)
          client.removeAllListeners()
          done()
        })
      })
    })

    function testMultiplePubrel (shouldSendPubcompFail, done) {
      var client = connect()
      var testTopic = 'test'
      var testMessage = 'message'
      var mid = 253
      var pubcompCount = 0
      var pubrelCount = 0
      var handleMessageCount = 0
      var emitMessageCount = 0
      var origSendPacket = client._sendPacket
      var shouldSendFail

      client.handleMessage = function (packet, callback) {
        handleMessageCount++
        callback()
      }

      client.on('message', function () {
        emitMessageCount++
      })

      client._sendPacket = function (packet, sendDone) {
        shouldSendFail = packet.cmd === 'pubcomp' && shouldSendPubcompFail
        if (sendDone) {
          sendDone(shouldSendFail ? new Error('testing pubcomp failure') : undefined)
        }

        // send the mocked response
        switch (packet.cmd) {
          case 'subscribe':
            const suback = {cmd: 'suback', messageId: packet.messageId, granted: [2]}
            client._handlePacket(suback, function (err) {
              should(err).not.be.ok()
            })
            break
          case 'pubrec':
          case 'pubcomp':
            // for both pubrec and pubcomp, reply with pubrel, simulating the server not receiving the pubcomp
            if (packet.cmd === 'pubcomp') {
              pubcompCount++
              if (pubcompCount === 2) {
                // end the test once the client has gone through two rounds of replying to pubrel messages
                pubrelCount.should.be.exactly(2)
                handleMessageCount.should.be.exactly(1)
                emitMessageCount.should.be.exactly(1)
                client._sendPacket = origSendPacket
                done()
                break
              }
            }

            // simulate the pubrel message, either in response to pubrec or to mock pubcomp failing to be received
            const pubrel = {cmd: 'pubrel', messageId: mid}
            pubrelCount++
            client._handlePacket(pubrel, function (err) {
              if (shouldSendFail) {
                should(err).be.ok()
              } else {
                should(err).not.be.ok()
              }
            })
            break
        }
      }

      client.once('connect', function () {
        client.subscribe(testTopic, {qos: 2})
        const publish = {cmd: 'publish', topic: testTopic, payload: testMessage, qos: 2, messageId: mid}
        client._handlePacket(publish, function (err) {
          should(err).not.be.ok()
        })
      })
    }

    it('handle qos 2 messages exactly once when multiple pubrel received', function (done) {
      testMultiplePubrel(false, done)
    })

    it('handle qos 2 messages exactly once when multiple pubrel received and sending pubcomp fails on client', function (done) {
      testMultiplePubrel(true, done)
    })
  })

  describe('auto reconnect', function () {
    it('should mark the client disconnecting if #end called', function () {
      var client = connect()

      client.end()
      client.disconnecting.should.eql(true)
    })

    it('should reconnect after stream disconnect', function (done) {
      var client = connect()
      var tryReconnect = true

      client.on('connect', function () {
        if (tryReconnect) {
          client.stream.end()
          tryReconnect = false
        } else {
          client.end()
          done()
        }
      })
    })

    it('should emit \'reconnect\' when reconnecting', function (done) {
      var client = connect()
      var tryReconnect = true
      var reconnectEvent = false

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function () {
        if (tryReconnect) {
          client.stream.end()
          tryReconnect = false
        } else {
          reconnectEvent.should.equal(true)
          client.end()
          done()
        }
      })
    })

    it('should emit \'offline\' after going offline', function (done) {
      var client = connect()
      var tryReconnect = true
      var offlineEvent = false

      client.on('offline', function () {
        offlineEvent = true
      })

      client.on('connect', function () {
        if (tryReconnect) {
          client.stream.end()
          tryReconnect = false
        } else {
          offlineEvent.should.equal(true)
          client.end()
          done()
        }
      })
    })

    it('should not reconnect if it was ended by the user', function (done) {
      var client = connect()

      client.on('connect', function () {
        client.end()
        done() // it will raise an exception if called two times
      })
    })

    it('should setup a reconnect timer on disconnect', function (done) {
      var client = connect()

      client.once('connect', function () {
        should.not.exist(client.reconnectTimer)
        client.stream.end()
      })

      client.once('close', function () {
        should.exist(client.reconnectTimer)
        client.end()
        done()
      })
    })

    it('should allow specification of a reconnect period', function (done) {
      var end
      var period = 200
      var client = connect({reconnectPeriod: period})
      var reconnect = false
      var start = Date.now()

      client.on('connect', function () {
        if (!reconnect) {
          client.stream.end()
          reconnect = true
        } else {
          client.end()
          end = Date.now()
          if (end - start >= period) {
            // Connected in about 2 seconds, that's good enough
            done()
          } else {
            done(new Error('Strange reconnect period'))
          }
        }
      })
    })

    it('should always cleanup successfully on reconnection', function (done) {
      var client = connect({host: 'this_hostname_should_not_exist', connectTimeout: 0, reconnectPeriod: 1})
      setTimeout(client.end.bind(client, done), 50)
    })

    it('should resend in-flight QoS 1 publish messages from the client', function (done) {
      var client = connect({reconnectPeriod: 200})
      var serverPublished = false
      var clientCalledBack = false

      server.once('client', function (serverClient) {
        serverClient.on('connect', function () {
          setImmediate(function () {
            serverClient.stream.destroy()
          })
        })

        server.once('client', function (serverClientNew) {
          serverClientNew.on('publish', function () {
            serverPublished = true
            check()
          })
        })
      })

      client.publish('hello', 'world', { qos: 1 }, function () {
        clientCalledBack = true
        check()
      })

      function check () {
        if (serverPublished && clientCalledBack) {
          client.end()
          done()
        }
      }
    })

    it('should not resend in-flight publish messages if disconnecting', function (done) {
      var client = connect({reconnectPeriod: 200})
      var serverPublished = false
      var clientCalledBack = false
      server.once('client', function (serverClient) {
        serverClient.on('connect', function () {
          setImmediate(function () {
            serverClient.stream.destroy()
            client.end()
            serverPublished.should.be.false()
            clientCalledBack.should.be.false()
            done()
          })
        })
        server.once('client', function (serverClientNew) {
          serverClientNew.on('publish', function () {
            serverPublished = true
          })
        })
      })
      client.publish('hello', 'world', { qos: 1 }, function () {
        clientCalledBack = true
      })
    })

    it('should resend in-flight QoS 2 publish messages from the client', function (done) {
      var client = connect({reconnectPeriod: 200})
      var serverPublished = false
      var clientCalledBack = false

      server.once('client', function (serverClient) {
        // ignore errors
        serverClient.on('error', function () {})
        serverClient.on('publish', function () {
          setImmediate(function () {
            serverClient.stream.destroy()
          })
        })

        server.once('client', function (serverClientNew) {
          serverClientNew.on('pubrel', function () {
            serverPublished = true
            check()
          })
        })
      })

      client.publish('hello', 'world', { qos: 2 }, function () {
        clientCalledBack = true
        check()
      })

      function check () {
        if (serverPublished && clientCalledBack) {
          client.end()
          done()
        }
      }
    })

    it('should not resend in-flight QoS 1 removed publish messages from the client', function (done) {
      var client = connect({reconnectPeriod: 200})
      var clientCalledBack = false

      server.once('client', function (serverClient) {
        serverClient.on('connect', function () {
          setImmediate(function () {
            serverClient.stream.destroy()
          })
        })

        server.once('client', function (serverClientNew) {
          serverClientNew.on('publish', function () {
            should.fail()
            done()
          })
        })
      })

      client.publish('hello', 'world', { qos: 1 }, function (err) {
        clientCalledBack = true
        should(err.message).be.equal('Message removed')
      })
      should(Object.keys(client.outgoing).length).be.equal(1)
      should(client.outgoingStore._inflights.size).be.equal(1)
      client.removeOutgoingMessage(client.getLastMessageId())
      should(Object.keys(client.outgoing).length).be.equal(0)
      should(client.outgoingStore._inflights.size).be.equal(0)
      clientCalledBack.should.be.true()
      client.end()
      done()
    })

    it('should not resend in-flight QoS 2 removed publish messages from the client', function (done) {
      var client = connect({reconnectPeriod: 200})
      var clientCalledBack = false

      server.once('client', function (serverClient) {
        serverClient.on('connect', function () {
          setImmediate(function () {
            serverClient.stream.destroy()
          })
        })

        server.once('client', function (serverClientNew) {
          serverClientNew.on('publish', function () {
            should.fail()
            done()
          })
        })
      })

      client.publish('hello', 'world', { qos: 2 }, function (err) {
        clientCalledBack = true
        should(err.message).be.equal('Message removed')
      })
      should(Object.keys(client.outgoing).length).be.equal(1)
      should(client.outgoingStore._inflights.size).be.equal(1)
      client.removeOutgoingMessage(client.getLastMessageId())
      should(Object.keys(client.outgoing).length).be.equal(0)
      should(client.outgoingStore._inflights.size).be.equal(0)
      clientCalledBack.should.be.true()
      client.end()
      done()
    })

    it('should resubscribe when reconnecting', function (done) {
      var client = connect({ reconnectPeriod: 100 })
      var tryReconnect = true
      var reconnectEvent = false

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function () {
        if (tryReconnect) {
          client.subscribe('hello', function () {
            client.stream.end()

            server.once('client', function (serverClient) {
              serverClient.on('subscribe', function () {
                client.end()
                done()
              })
            })
          })

          tryReconnect = false
        } else {
          reconnectEvent.should.equal(true)
        }
      })
    })

    it('should not resubscribe when reconnecting if resubscribe is disabled', function (done) {
      var client = connect({ reconnectPeriod: 100, resubscribe: false })
      var tryReconnect = true
      var reconnectEvent = false

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function () {
        if (tryReconnect) {
          client.subscribe('hello', function () {
            client.stream.end()

            server.once('client', function (serverClient) {
              serverClient.on('subscribe', function () {
                should.fail()
              })
            })
          })

          tryReconnect = false
        } else {
          reconnectEvent.should.equal(true)
          should(Object.keys(client._resubscribeTopics).length).be.equal(0)
          done()
        }
      })
    })

    it('should not resubscribe when reconnecting if suback is error', function (done) {
      var tryReconnect = true
      var reconnectEvent = false
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('subscribe', function (packet) {
          c.suback({
            messageId: packet.messageId,
            granted: packet.subscriptions.map(function (e) {
              return e.qos | 0x80
            })
          })
          c.pubrel({ messageId: Math.floor(Math.random() * 9000) + 1000 })
        })
      })

      server2.listen(port + 49, function () {
        var client = mqtt.connect({
          port: port + 49,
          host: 'localhost',
          reconnectPeriod: 100
        })

        client.on('reconnect', function () {
          reconnectEvent = true
        })

        client.on('connect', function () {
          if (tryReconnect) {
            client.subscribe('hello', function () {
              client.stream.end()

              server.once('client', function (serverClient) {
                serverClient.on('subscribe', function () {
                  should.fail()
                })
              })
            })
            tryReconnect = false
          } else {
            reconnectEvent.should.equal(true)
            should(Object.keys(client._resubscribeTopics).length).be.equal(0)
            server2.close()
            done()
          }
        })
      })
    })

    it('should preserved incomingStore after disconnecting if clean is false', function (done) {
      var reconnect = false
      var client = {}
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
          if (reconnect) {
            c.pubrel({ messageId: 1 })
          }
        })
        c.on('subscribe', function (packet) {
          c.suback({
            messageId: packet.messageId,
            granted: packet.subscriptions.map(function (e) {
              return e.qos
            })
          })
          c.publish({ topic: 'topic', payload: 'payload', qos: 2, messageId: 1, retain: false })
        })
        c.on('pubrec', function (packet) {
          client.end(false, function () {
            client.reconnect({
              incomingStore: incomingStore,
              outgoingStore: outgoingStore
            })
          })
        })
        c.on('pubcomp', function (packet) {
          client.end()
          server2.close()
          done()
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore
        })

        client.on('connect', function () {
          if (!reconnect) {
            client.subscribe('test', {qos: 2}, function () {
            })
            reconnect = true
          }
        })
        client.on('message', function (topic, message) {
          topic.should.equal('topic')
          message.toString().should.equal('payload')
        })
      })
    })

    it('should clear outgoing if close from server', function (done) {
      var reconnect = false
      var client = {}
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('subscribe', function (packet) {
          if (reconnect) {
            c.suback({
              messageId: packet.messageId,
              granted: packet.subscriptions.map(function (e) {
                return e.qos
              })
            })
          } else {
            c.destroy()
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: true,
          clientId: 'cid1',
          reconnectPeriod: 0
        })

        client.on('connect', function () {
          client.subscribe('test', {qos: 2}, function (e) {
            if (!e) {
              client.end()
            }
          })
        })

        client.on('close', function () {
          if (reconnect) {
            server2.close()
            done()
          } else {
            Object.keys(client.outgoing).length.should.equal(0)
            reconnect = true
            client.reconnect()
          }
        })
      })
    })

    it('should resend in-flight QoS 1 publish messages from the client if clean is false', function (done) {
      var reconnect = false
      var client = {}
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('publish', function (packet) {
          if (reconnect) {
            server2.close()
            done()
          } else {
            client.end(true, function () {
              client.reconnect({
                incomingStore: incomingStore,
                outgoingStore: outgoingStore
              })
              reconnect = true
            })
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore
        })

        client.on('connect', function () {
          if (!reconnect) {
            client.publish('topic', 'payload', {qos: 1})
          }
        })
        client.on('error', function () {})
      })
    })

    it('should resend in-flight QoS 2 publish messages from the client if clean is false', function (done) {
      var reconnect = false
      var client = {}
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('publish', function (packet) {
          if (reconnect) {
            server2.close()
            done()
          } else {
            client.end(true, function () {
              client.reconnect({
                incomingStore: incomingStore,
                outgoingStore: outgoingStore
              })
              reconnect = true
            })
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore
        })

        client.on('connect', function () {
          if (!reconnect) {
            client.publish('topic', 'payload', {qos: 2})
          }
        })
        client.on('error', function () {})
      })
    })

    it('should resend in-flight QoS 2 pubrel messages from the client if clean is false', function (done) {
      var reconnect = false
      var client = {}
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var server2 = new Server(function (c) {
        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('publish', function (packet) {
          if (!reconnect) {
            c.pubrec({messageId: packet.messageId})
          }
        })
        c.on('pubrel', function () {
          if (reconnect) {
            server2.close()
            done()
          } else {
            client.end(true, function () {
              client.reconnect({
                incomingStore: incomingStore,
                outgoingStore: outgoingStore
              })
              reconnect = true
            })
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore
        })

        client.on('connect', function () {
          if (!reconnect) {
            client.publish('topic', 'payload', {qos: 2})
          }
        })
        client.on('error', function () {})
      })
    })

    it('should resend in-flight publish messages by published order', function (done) {
      var publishCount = 0
      var reconnect = false
      var disconnectOnce = true
      var client = {}
      var incomingStore = new mqtt.Store({ clean: false })
      var outgoingStore = new mqtt.Store({ clean: false })
      var server2 = new Server(function (c) {
        // errors are not interesting for this test
        // but they might happen on some platforms
        c.on('error', function () {})

        c.on('connect', function (packet) {
          c.connack({returnCode: 0})
        })
        c.on('publish', function (packet) {
          c.puback({messageId: packet.messageId})
          if (reconnect) {
            switch (publishCount++) {
              case 0:
                packet.payload.toString().should.equal('payload1')
                break
              case 1:
                packet.payload.toString().should.equal('payload2')
                break
              case 2:
                packet.payload.toString().should.equal('payload3')
                server2.close()
                done()
                break
            }
          } else {
            if (disconnectOnce) {
              client.end(true, function () {
                reconnect = true
                client.reconnect({
                  incomingStore: incomingStore,
                  outgoingStore: outgoingStore
                })
              })
              disconnectOnce = false
            }
          }
        })
      })

      server2.listen(port + 50, function () {
        client = mqtt.connect({
          port: port + 50,
          host: 'localhost',
          clean: false,
          clientId: 'cid1',
          reconnectPeriod: 0,
          incomingStore: incomingStore,
          outgoingStore: outgoingStore
        })

        client.nextId = 65535

        client.on('connect', function () {
          if (!reconnect) {
            client.publish('topic', 'payload1', {qos: 1})
            client.publish('topic', 'payload2', {qos: 1})
            client.publish('topic', 'payload3', {qos: 1})
          }
        })
        client.on('error', function () {})
      })
    })

    it('should be able to pub/sub if reconnect() is called at close handler', function (done) {
      var client = connect({ reconnectPeriod: 0 })
      var tryReconnect = true
      var reconnectEvent = false

      client.on('close', function () {
        if (tryReconnect) {
          tryReconnect = false
          client.reconnect()
        } else {
          reconnectEvent.should.equal(true)
          done()
        }
      })

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function () {
        if (tryReconnect) {
          client.end()
        } else {
          client.subscribe('hello', function () {
            client.end()
          })
        }
      })
    })

    it('should be able to pub/sub if reconnect() is called at out of close handler', function (done) {
      var client = connect({ reconnectPeriod: 0 })
      var tryReconnect = true
      var reconnectEvent = false

      client.on('close', function () {
        if (tryReconnect) {
          tryReconnect = false
          setTimeout(function () {
            client.reconnect()
          }, 100)
        } else {
          reconnectEvent.should.equal(true)
          done()
        }
      })

      client.on('reconnect', function () {
        reconnectEvent = true
      })

      client.on('connect', function () {
        if (tryReconnect) {
          client.end()
        } else {
          client.subscribe('hello', function () {
            client.end()
          })
        }
      })
    })

    context('with alternate server client', function () {
      var cachedClientListeners
      var connack = version === 5 ? { reasonCode: 0 } : { returnCode: 0 }

      beforeEach(function () {
        cachedClientListeners = server.listeners('client')
        server.removeAllListeners('client')
      })

      afterEach(function () {
        server.removeAllListeners('client')
        cachedClientListeners.forEach(function (listener) {
          server.on('client', listener)
        })
      })

      it('should resubscribe even if disconnect is before suback', function (done) {
        var client = mqtt.connect(Object.assign({ reconnectPeriod: 100 }, config))
        var subscribeCount = 0
        var connectCount = 0

        server.on('client', function (serverClient) {
          serverClient.on('connect', function () {
            connectCount++
            serverClient.connack(connack)
          })

          serverClient.on('subscribe', function () {
            subscribeCount++

            // disconnect before sending the suback on the first subscribe
            if (subscribeCount === 1) {
              client.stream.end()
            }

            // after the second connection, confirm that the only two
            // subscribes have taken place, then cleanup and exit
            if (connectCount >= 2) {
              subscribeCount.should.equal(2)
              client.end(true, done)
            }
          })
        })

        client.subscribe('hello')
      })

      it('should resubscribe exactly once', function (done) {
        var client = mqtt.connect(Object.assign({ reconnectPeriod: 100 }, config))
        var subscribeCount = 0

        server.on('client', function (serverClient) {
          serverClient.on('connect', function () {
            serverClient.connack(connack)
          })

          serverClient.on('subscribe', function () {
            subscribeCount++

            // disconnect before sending the suback on the first subscribe
            if (subscribeCount === 1) {
              client.stream.end()
            }

            // after the second connection, only two subs
            // subscribes have taken place, then cleanup and exit
            if (subscribeCount === 2) {
              client.end(true, done)
            }
          })
        })

        client.subscribe('hello')
      })
    })
  })
}
