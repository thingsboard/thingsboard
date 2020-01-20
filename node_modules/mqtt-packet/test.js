'use strict'

var test = require('tape')
var mqtt = require('./')
var Buffer = require('safe-buffer').Buffer
var WS = require('readable-stream').Writable

function normalExpectedObject (object) {
  if (object.username != null) object.username = object.username.toString()
  if (object.password != null) object.password = Buffer.from(object.password)
  return object
}

function testParseGenerate (name, object, buffer, opts) {
  test(name + ' parse', function (t) {
    t.plan(2)

    var parser = mqtt.parser(opts)
    var expected = object
    var fixture = buffer

    parser.on('packet', function (packet) {
      if (packet.cmd !== 'publish') {
        delete packet.topic
        delete packet.payload
      }
      t.deepEqual(packet, normalExpectedObject(expected), 'expected packet')
    })

    parser.on('error', function (err) {
      t.fail(err)
    })

    t.equal(parser.parse(fixture), 0, 'remaining bytes')
  })

  test(name + ' generate', function (t) {
    t.equal(mqtt.generate(object, opts).toString('hex'), buffer.toString('hex'))
    t.end()
  })

  test(name + ' mirror', function (t) {
    t.plan(2)

    var parser = mqtt.parser(opts)
    var expected = object
    var fixture = mqtt.generate(object, opts)

    parser.on('packet', function (packet) {
      if (packet.cmd !== 'publish') {
        delete packet.topic
        delete packet.payload
      }
      t.deepEqual(packet, normalExpectedObject(expected), 'expected packet')
    })

    parser.on('error', function (err) {
      t.fail(err)
    })

    t.equal(parser.parse(fixture), 0, 'remaining bytes')
  })

  test(name + ' writeToStream', function (t) {
    var stream = WS()
    stream.write = () => true
    stream.on('error', (err) => t.fail(err))

    var result = mqtt.writeToStream(object, stream, opts)
    t.equal(result, true, 'should return true')
    t.end()
  })
}

function testParseError (expected, fixture, opts) {
  test(expected, function (t) {
    t.plan(1)

    var parser = mqtt.parser(opts)

    parser.on('error', function (err) {
      t.equal(err.message, expected, 'expected error message')
    })

    parser.on('packet', function () {
      t.fail('parse errors should not be followed by packet events')
    })

    parser.parse(fixture)
  })
}

function testGenerateError (expected, fixture, opts) {
  test(expected, function (t) {
    t.plan(1)

    try {
      mqtt.generate(fixture, opts)
    } catch (err) {
      t.equal(expected, err.message)
    }
  })
}

function testParseGenerateDefaults (name, object, buffer, opts) {
  test(name + ' parse', function (t) {
    var parser = mqtt.parser(opts)
    var expected = object
    var fixture = buffer

    t.plan(1 + Object.keys(expected).length)

    parser.on('packet', function (packet) {
      Object.keys(expected).forEach(function (key) {
        t.deepEqual(packet[key], expected[key], 'expected packet property ' + key)
      })
    })

    t.equal(parser.parse(fixture), 0, 'remaining bytes')
  })

  test(name + ' generate', function (t) {
    t.equal(mqtt.generate(object).toString('hex'), buffer.toString('hex'))
    t.end()
  })
}

function testWriteToStreamError (expected, fixture) {
  test('writeToStream ' + expected + ' error', function (t) {
    t.plan(2)

    var stream = WS()

    stream.write = () => t.fail('should not have called write')
    stream.on('error', () => t.pass('error emitted'))

    var result = mqtt.writeToStream(fixture, stream)

    t.false(result, 'result should be false')
  })
}

test('disabled numbers cache', function (t) {
  var stream = WS()
  var message = {
    cmd: 'publish',
    retain: false,
    qos: 0,
    dup: false,
    length: 10,
    topic: Buffer.from('test'),
    payload: Buffer.from('test')
  }
  var expected = Buffer.from([
    48, 10, // Header
    0, 4, // Topic length
    116, 101, 115, 116, // Topic (test)
    116, 101, 115, 116 // Payload (test)
  ])
  var written = Buffer.alloc(0)

  stream.write = (chunk) => {
    written = Buffer.concat([written, chunk])
  }
  mqtt.writeToStream.cacheNumbers = false

  mqtt.writeToStream(message, stream)

  t.deepEqual(written, expected, 'written buffer is expected')

  mqtt.writeToStream.cacheNumbers = true

  stream.end()
  t.end()
})

testParseGenerate('minimal connect', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 18,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  clean: false,
  keepalive: 30,
  clientId: 'test'
}, Buffer.from([
  16, 18, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  0, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116 // Client ID
]))

testParseGenerate('connect MQTT 5.0', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 125,
  protocolId: 'MQTT',
  protocolVersion: 5,
  will: {
    retain: true,
    qos: 2,
    properties: {
      willDelayInterval: 1234,
      payloadFormatIndicator: false,
      messageExpiryInterval: 4321,
      contentType: 'test',
      responseTopic: 'topic',
      correlationData: Buffer.from([1, 2, 3, 4]),
      userProperties: {
        'test': 'test'
      }
    },
    topic: 'topic',
    payload: Buffer.from([4, 3, 2, 1])
  },
  clean: true,
  keepalive: 30,
  properties: {
    sessionExpiryInterval: 1234,
    receiveMaximum: 432,
    maximumPacketSize: 100,
    topicAliasMaximum: 456,
    requestResponseInformation: true,
    requestProblemInformation: true,
    userProperties: {
      'test': 'test'
    },
    authenticationMethod: 'test',
    authenticationData: Buffer.from([1, 2, 3, 4])
  },
  clientId: 'test'
}, Buffer.from([
  16, 125, // Header
  0, 4, // Protocol ID length
  77, 81, 84, 84, // Protocol ID
  5, // Protocol version
  54, // Connect flags
  0, 30, // Keepalive
  47, // properties length
  17, 0, 0, 4, 210, // sessionExpiryInterval
  33, 1, 176, // receiveMaximum
  39, 0, 0, 0, 100, // maximumPacketSize
  34, 1, 200,  // topicAliasMaximum
  25, 1, // requestResponseInformation
  23, 1, // requestProblemInformation,
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties,
  21, 0, 4, 116, 101, 115, 116, // authenticationMethod
  22, 0, 4, 1, 2, 3, 4, // authenticationData
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  47, // will properties
  24, 0, 0, 4, 210, // will delay interval
  1, 0, // payload format indicator
  2, 0, 0, 16, 225, // message expiry interval
  3, 0, 4, 116, 101, 115, 116, // content type
  8, 0, 5, 116, 111, 112, 105, 99, // response topic
  9, 0, 4, 1, 2, 3, 4, // corelation data
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // user properties
  0, 5, // Will topic length
  116, 111, 112, 105, 99, // Will topic
  0, 4, // Will payload length
  4, 3, 2, 1// Will payload
]))

testParseGenerate('connect MQTT 5.0 with will properties but w/o will payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 121,
  protocolId: 'MQTT',
  protocolVersion: 5,
  will: {
    retain: true,
    qos: 2,
    properties: {
      willDelayInterval: 1234,
      payloadFormatIndicator: false,
      messageExpiryInterval: 4321,
      contentType: 'test',
      responseTopic: 'topic',
      correlationData: Buffer.from([1, 2, 3, 4]),
      userProperties: {
        'test': 'test'
      }
    },
    topic: 'topic',
    payload: Buffer.from([])
  },
  clean: true,
  keepalive: 30,
  properties: {
    sessionExpiryInterval: 1234,
    receiveMaximum: 432,
    maximumPacketSize: 100,
    topicAliasMaximum: 456,
    requestResponseInformation: true,
    requestProblemInformation: true,
    userProperties: {
      'test': 'test'
    },
    authenticationMethod: 'test',
    authenticationData: Buffer.from([1, 2, 3, 4])
  },
  clientId: 'test'
}, Buffer.from([
  16, 121, // Header
  0, 4, // Protocol ID length
  77, 81, 84, 84, // Protocol ID
  5, // Protocol version
  54, // Connect flags
  0, 30, // Keepalive
  47, // properties length
  17, 0, 0, 4, 210, // sessionExpiryInterval
  33, 1, 176, // receiveMaximum
  39, 0, 0, 0, 100, // maximumPacketSize
  34, 1, 200,  // topicAliasMaximum
  25, 1, // requestResponseInformation
  23, 1, // requestProblemInformation,
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties,
  21, 0, 4, 116, 101, 115, 116, // authenticationMethod
  22, 0, 4, 1, 2, 3, 4, // authenticationData
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  47, // will properties
  24, 0, 0, 4, 210, // will delay interval
  1, 0, // payload format indicator
  2, 0, 0, 16, 225, // message expiry interval
  3, 0, 4, 116, 101, 115, 116, // content type
  8, 0, 5, 116, 111, 112, 105, 99, // response topic
  9, 0, 4, 1, 2, 3, 4, // corelation data
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // user properties
  0, 5, // Will topic length
  116, 111, 112, 105, 99, // Will topic
  0, 0 // Will payload length
]))

testParseGenerate('connect MQTT 5.0 w/o will properties', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 78,
  protocolId: 'MQTT',
  protocolVersion: 5,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: Buffer.from([4, 3, 2, 1])
  },
  clean: true,
  keepalive: 30,
  properties: {
    sessionExpiryInterval: 1234,
    receiveMaximum: 432,
    maximumPacketSize: 100,
    topicAliasMaximum: 456,
    requestResponseInformation: true,
    requestProblemInformation: true,
    userProperties: {
      'test': 'test'
    },
    authenticationMethod: 'test',
    authenticationData: Buffer.from([1, 2, 3, 4])
  },
  clientId: 'test'
}, Buffer.from([
  16, 78, // Header
  0, 4, // Protocol ID length
  77, 81, 84, 84, // Protocol ID
  5, // Protocol version
  54, // Connect flags
  0, 30, // Keepalive
  47, // properties length
  17, 0, 0, 4, 210, // sessionExpiryInterval
  33, 1, 176, // receiveMaximum
  39, 0, 0, 0, 100, // maximumPacketSize
  34, 1, 200,  // topicAliasMaximum
  25, 1, // requestResponseInformation
  23, 1, // requestProblemInformation,
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties,
  21, 0, 4, 116, 101, 115, 116, // authenticationMethod
  22, 0, 4, 1, 2, 3, 4, // authenticationData
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, // will properties
  0, 5, // Will topic length
  116, 111, 112, 105, 99, // Will topic
  0, 4, // Will payload length
  4, 3, 2, 1// Will payload
]))

testParseGenerate('no clientId with 3.1.1', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 12,
  protocolId: 'MQTT',
  protocolVersion: 4,
  clean: true,
  keepalive: 30,
  clientId: ''
}, Buffer.from([
  16, 12, // Header
  0, 4, // Protocol ID length
  77, 81, 84, 84, // Protocol ID
  4, // Protocol version
  2, // Connect flags
  0, 30, // Keepalive
  0, 0 // Client ID length
]))

testParseGenerateDefaults('default connect', {
  cmd: 'connect',
  clientId: 'test'
}, Buffer.from([
  16, 16, 0, 4, 77, 81, 84,
  84, 4, 2, 0, 0,
  0, 4, 116, 101, 115, 116
]))

testParseGenerate('empty will payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 47,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: new Buffer(0)
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: new Buffer('password')
}, Buffer.from([
  16, 47, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  246, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 5, // Will topic length
  116, 111, 112, 105, 99, // Will topic
  0, 0, // Will payload length
  // Will payload
  0, 8, // Username length
  117, 115, 101, 114, 110, 97, 109, 101, // Username
  0, 8, // Password length
  112, 97, 115, 115, 119, 111, 114, 100 // Password
]))

testParseGenerate('empty buffer username payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 20,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: new Buffer('')
}, Buffer.from([
  16, 20, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  130, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 0 // Username length
  // Empty Username payload
]))

testParseGenerate('empty string username payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 20,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: ''
}, Buffer.from([
  16, 20, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  130, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 0 // Username length
  // Empty Username payload
]))

testParseGenerate('empty buffer password payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 30,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: new Buffer('')
}, Buffer.from([
  16, 30, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  194, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 8, // Username length
  117, 115, 101, 114, 110, 97, 109, 101, // Username payload
  0, 0 // Password length
  // Empty password payload
]))

testParseGenerate('empty string password payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 30,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: ''
}, Buffer.from([
  16, 30, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  194, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 8, // Username length
  117, 115, 101, 114, 110, 97, 109, 101, // Username payload
  0, 0 // Password length
  // Empty password payload
]))

testParseGenerate('empty string username and password payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 22,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: '',
  password: new Buffer('')
}, Buffer.from([
  16, 22, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  194, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 0, // Username length
  // Empty Username payload
  0, 0 // Password length
  // Empty password payload
]))

testParseGenerate('maximal connect', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: new Buffer('payload')
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: new Buffer('password')
}, Buffer.from([
  16, 54, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  246, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116, // Client ID
  0, 5, // Will topic length
  116, 111, 112, 105, 99, // Will topic
  0, 7, // Will payload length
  112, 97, 121, 108, 111, 97, 100, // Will payload
  0, 8, // Username length
  117, 115, 101, 114, 110, 97, 109, 101, // Username
  0, 8, // Password length
  112, 97, 115, 115, 119, 111, 114, 100 // Password
]))

testParseGenerate('max connect with special chars', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 57,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'tòpic',
    payload: new Buffer('pay£oad')
  },
  clean: true,
  keepalive: 30,
  clientId: 'te$t',
  username: 'u$ern4me',
  password: new Buffer('p4$$w0£d')
}, Buffer.from([
  16, 57, // Header
  0, 6, // Protocol ID length
  77, 81, 73, 115, 100, 112, // Protocol ID
  3, // Protocol version
  246, // Connect flags
  0, 30, // Keepalive
  0, 4, // Client ID length
  116, 101, 36, 116, // Client ID
  0, 6, // Will topic length
  116, 195, 178, 112, 105, 99, // Will topic
  0, 8, // Will payload length
  112, 97, 121, 194, 163, 111, 97, 100, // Will payload
  0, 8, // Username length
  117, 36, 101, 114, 110, 52, 109, 101, // Username
  0, 9, // Password length
  112, 52, 36, 36, 119, 48, 194, 163, 100 // Password
]))

test('connect all strings generate', function (t) {
  var message = {
    cmd: 'connect',
    retain: false,
    qos: 0,
    dup: false,
    length: 54,
    protocolId: 'MQIsdp',
    protocolVersion: 3,
    will: {
      retain: true,
      qos: 2,
      topic: 'topic',
      payload: 'payload'
    },
    clean: true,
    keepalive: 30,
    clientId: 'test',
    username: 'username',
    password: 'password'
  }
  var expected = Buffer.from([
    16, 54, // Header
    0, 6, // Protocol ID length
    77, 81, 73, 115, 100, 112, // Protocol ID
    3, // Protocol version
    246, // Connect flags
    0, 30, // Keepalive
    0, 4, // Client ID length
    116, 101, 115, 116, // Client ID
    0, 5, // Will topic length
    116, 111, 112, 105, 99, // Will topic
    0, 7, // Will payload length
    112, 97, 121, 108, 111, 97, 100, // Will payload
    0, 8, // Username length
    117, 115, 101, 114, 110, 97, 109, 101, // Username
    0, 8, // Password length
    112, 97, 115, 115, 119, 111, 114, 100 // Password
  ])

  t.equal(mqtt.generate(message).toString('hex'), expected.toString('hex'))
  t.end()
})

testParseError('Cannot parse protocolId', Buffer.from([
  16, 4,
  0, 6,
  77, 81
]))

testParseGenerate('connack with return code 0', {
  cmd: 'connack',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  sessionPresent: false,
  returnCode: 0
}, Buffer.from([
  32, 2, 0, 0
]))

testParseGenerate('connack MQTT5 with properties', {
  cmd: 'connack',
  retain: false,
  qos: 0,
  dup: false,
  length: 87,
  sessionPresent: false,
  reasonCode: 0,
  properties: {
    sessionExpiryInterval: 1234,
    receiveMaximum: 432,
    maximumQoS: 2,
    retainAvailable: true,
    maximumPacketSize: 100,
    assignedClientIdentifier: 'test',
    topicAliasMaximum: 456,
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    },
    wildcardSubscriptionAvailable: true,
    subscriptionIdentifiersAvailable: true,
    sharedSubscriptionAvailable: false,
    serverKeepAlive: 1234,
    responseInformation: 'test',
    serverReference: 'test',
    authenticationMethod: 'test',
    authenticationData: Buffer.from([1, 2, 3, 4])
  }
}, Buffer.from([
  32, 87, 0, 0,
  84, // properties length
  17, 0, 0, 4, 210, // sessionExpiryInterval
  33, 1, 176, // receiveMaximum
  36, 2, // Maximum qos
  37, 1, // retainAvailable
  39, 0, 0, 0, 100, // maximumPacketSize
  18, 0, 4, 116, 101, 115, 116, // assignedClientIdentifier
  34, 1, 200, // topicAliasMaximum
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  40, 1, // wildcardSubscriptionAvailable
  41, 1, // subscriptionIdentifiersAvailable
  42, 0, // sharedSubscriptionAvailable
  19, 4, 210, // serverKeepAlive
  26, 0, 4, 116, 101, 115, 116, // responseInformation
  28, 0, 4, 116, 101, 115, 116, // serverReference
  21, 0, 4, 116, 101, 115, 116, // authenticationMethod
  22, 0, 4, 1, 2, 3, 4 // authenticationData
]), { protocolVersion: 5 })

testParseGenerate('connack MQTT5 with properties and doubled user properties', {
  cmd: 'connack',
  retain: false,
  qos: 0,
  dup: false,
  length: 100,
  sessionPresent: false,
  reasonCode: 0,
  properties: {
    sessionExpiryInterval: 1234,
    receiveMaximum: 432,
    maximumQoS: 2,
    retainAvailable: true,
    maximumPacketSize: 100,
    assignedClientIdentifier: 'test',
    topicAliasMaximum: 456,
    reasonString: 'test',
    userProperties: {
      'test': ['test', 'test']
    },
    wildcardSubscriptionAvailable: true,
    subscriptionIdentifiersAvailable: true,
    sharedSubscriptionAvailable: false,
    serverKeepAlive: 1234,
    responseInformation: 'test',
    serverReference: 'test',
    authenticationMethod: 'test',
    authenticationData: Buffer.from([1, 2, 3, 4])
  }
}, Buffer.from([
  32, 100, 0, 0,
  97, // properties length
  17, 0, 0, 4, 210, // sessionExpiryInterval
  33, 1, 176, // receiveMaximum
  36, 2, // Maximum qos
  37, 1, // retainAvailable
  39, 0, 0, 0, 100, // maximumPacketSize
  18, 0, 4, 116, 101, 115, 116, // assignedClientIdentifier
  34, 1, 200, // topicAliasMaximum
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116,
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  40, 1, // wildcardSubscriptionAvailable
  41, 1, // subscriptionIdentifiersAvailable
  42, 0, // sharedSubscriptionAvailable
  19, 4, 210, // serverKeepAlive
  26, 0, 4, 116, 101, 115, 116, // responseInformation
  28, 0, 4, 116, 101, 115, 116, // serverReference
  21, 0, 4, 116, 101, 115, 116, // authenticationMethod
  22, 0, 4, 1, 2, 3, 4 // authenticationData
]), { protocolVersion: 5 })

testParseGenerate('connack with return code 0 session present bit set', {
  cmd: 'connack',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  sessionPresent: true,
  returnCode: 0
}, Buffer.from([
  32, 2, 1, 0
]))

testParseGenerate('connack with return code 5', {
  cmd: 'connack',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  sessionPresent: false,
  returnCode: 5
}, Buffer.from([
  32, 2, 0, 5
]))

testParseGenerate('minimal publish', {
  cmd: 'publish',
  retain: false,
  qos: 0,
  dup: false,
  length: 10,
  topic: 'test',
  payload: new Buffer('test')
}, Buffer.from([
  48, 10, // Header
  0, 4, // Topic length
  116, 101, 115, 116, // Topic (test)
  116, 101, 115, 116 // Payload (test)
]))

testParseGenerate('publish MQTT5 properties', {
  cmd: 'publish',
  retain: true,
  qos: 2,
  dup: true,
  length: 60,
  topic: 'test',
  payload: new Buffer('test'),
  messageId: 10,
  properties: {
    payloadFormatIndicator: true,
    messageExpiryInterval: 4321,
    topicAlias: 100,
    responseTopic: 'topic',
    correlationData: Buffer.from([1, 2, 3, 4]),
    userProperties: {
      'test': 'test'
    },
    subscriptionIdentifier: 120,
    contentType: 'test'
  }
}, Buffer.from([
  61, 60, // Header
  0, 4, // Topic length
  116, 101, 115, 116, // Topic (test)
  0, 10, // Message ID
  47, // properties length
  1, 1, // payloadFormatIndicator
  2, 0, 0, 16, 225, // message expiry interval
  35, 0, 100, // topicAlias
  8, 0, 5, 116, 111, 112, 105, 99, // response topic
  9, 0, 4, 1, 2, 3, 4, // correlationData
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  11, 120, // subscriptionIdentifier
  3, 0, 4, 116, 101, 115, 116, // content type
  116, 101, 115, 116 // Payload (test)
]), { protocolVersion: 5 })

testParseGenerate('publish MQTT5 with multiple same properties', {
  cmd: 'publish',
  retain: true,
  qos: 2,
  dup: true,
  length: 62,
  topic: 'test',
  payload: new Buffer('test'),
  messageId: 10,
  properties: {
    payloadFormatIndicator: true,
    messageExpiryInterval: 4321,
    topicAlias: 100,
    responseTopic: 'topic',
    correlationData: Buffer.from([1, 2, 3, 4]),
    userProperties: {
      'test': 'test'
    },
    subscriptionIdentifier: [120, 121],
    contentType: 'test'
  }
}, Buffer.from([
  61, 62, // Header
  0, 4, // Topic length
  116, 101, 115, 116, // Topic (test)
  0, 10, // Message ID
  49, // properties length
  1, 1, // payloadFormatIndicator
  2, 0, 0, 16, 225, // message expiry interval
  35, 0, 100, // topicAlias
  8, 0, 5, 116, 111, 112, 105, 99, // response topic
  9, 0, 4, 1, 2, 3, 4, // correlationData
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  11, 120, // subscriptionIdentifier
  11, 121, // subscriptionIdentifier
  3, 0, 4, 116, 101, 115, 116, // content type
  116, 101, 115, 116 // Payload (test)
]), { protocolVersion: 5 })

;(function () {
  var buffer = new Buffer(2048)
  testParseGenerate('2KB publish packet', {
    cmd: 'publish',
    retain: false,
    qos: 0,
    dup: false,
    length: 2054,
    topic: 'test',
    payload: buffer
  }, Buffer.concat([Buffer.from([
    48, 134, 16, // Header
    0, 4, // Topic length
    116, 101, 115, 116 // Topic (test)
  ]), buffer]))
})()

;(function () {
  var buffer = new Buffer(2 * 1024 * 1024)
  testParseGenerate('2MB publish packet', {
    cmd: 'publish',
    retain: false,
    qos: 0,
    dup: false,
    length: 6 + 2 * 1024 * 1024,
    topic: 'test',
    payload: buffer
  }, Buffer.concat([Buffer.from([
    48, 134, 128, 128, 1, // Header
    0, 4, // Topic length
    116, 101, 115, 116 // Topic (test)
  ]), buffer]))
})()

testParseGenerate('maximal publish', {
  cmd: 'publish',
  retain: true,
  qos: 2,
  length: 12,
  dup: true,
  topic: 'test',
  messageId: 10,
  payload: new Buffer('test')
}, Buffer.from([
  61, 12, // Header
  0, 4, // Topic length
  116, 101, 115, 116, // Topic
  0, 10, // Message ID
  116, 101, 115, 116 // Payload
]))

test('publish all strings generate', function (t) {
  var message = {
    cmd: 'publish',
    retain: true,
    qos: 2,
    length: 12,
    dup: true,
    topic: 'test',
    messageId: 10,
    payload: new Buffer('test')
  }
  var expected = Buffer.from([
    61, 12, // Header
    0, 4, // Topic length
    116, 101, 115, 116, // Topic
    0, 10, // Message ID
    116, 101, 115, 116 // Payload
  ])

  t.equal(mqtt.generate(message).toString('hex'), expected.toString('hex'))
  t.end()
})

testParseGenerate('empty publish', {
  cmd: 'publish',
  retain: false,
  qos: 0,
  dup: false,
  length: 6,
  topic: 'test',
  payload: new Buffer(0)
}, Buffer.from([
  48, 6, // Header
  0, 4, // Topic length
  116, 101, 115, 116 // Topic
  // Empty payload
]))

test('splitted publish parse', function (t) {
  t.plan(3)

  var parser = mqtt.parser()
  var expected = {
    cmd: 'publish',
    retain: false,
    qos: 0,
    dup: false,
    length: 10,
    topic: 'test',
    payload: new Buffer('test')
  }

  parser.on('packet', function (packet) {
    t.deepEqual(packet, expected, 'expected packet')
  })

  t.equal(parser.parse(Buffer.from([
    48, 10, // Header
    0, 4, // Topic length
    116, 101, 115, 116 // Topic (test)
  ])), 6, 'remaining bytes')

  t.equal(parser.parse(Buffer.from([
    116, 101, 115, 116 // Payload (test)
  ])), 0, 'remaining bytes')
})

testParseGenerate('puback', {
  cmd: 'puback',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  messageId: 2
}, Buffer.from([
  64, 2, // Header
  0, 2 // Message ID
]))

testParseGenerate('puback MQTT5 properties', {
  cmd: 'puback',
  retain: false,
  qos: 0,
  dup: false,
  length: 24,
  messageId: 2,
  reasonCode: 16,
  properties: {
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  64, 24, // Header
  0, 2, // Message ID
  16, // reason code
  20, // properties length
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116 // userProperties
]), {protocolVersion: 5})

testParseGenerate('pubrec', {
  cmd: 'pubrec',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  messageId: 2
}, Buffer.from([
  80, 2, // Header
  0, 2 // Message ID
]))

testParseGenerate('pubrec MQTT5 properties', {
  cmd: 'pubrec',
  retain: false,
  qos: 0,
  dup: false,
  length: 24,
  messageId: 2,
  reasonCode: 16,
  properties: {
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  80, 24, // Header
  0, 2, // Message ID
  16, // reason code
  20, // properties length
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116 // userProperties
]), {protocolVersion: 5})

testParseGenerate('pubrel', {
  cmd: 'pubrel',
  retain: false,
  qos: 1,
  dup: false,
  length: 2,
  messageId: 2
}, Buffer.from([
  98, 2, // Header
  0, 2 // Message ID
]))

testParseGenerate('pubrel MQTT5 properties', {
  cmd: 'pubrel',
  retain: false,
  qos: 1,
  dup: false,
  length: 24,
  messageId: 2,
  reasonCode: 16,
  properties: {
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  98, 24, // Header
  0, 2, // Message ID
  16, // reason code
  20, // properties length
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116 // userProperties
]), {protocolVersion: 5})

testParseGenerate('pubcomp', {
  cmd: 'pubcomp',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  messageId: 2
}, Buffer.from([
  112, 2, // Header
  0, 2 // Message ID
]))

testParseGenerate('pubcomp MQTT5 properties', {
  cmd: 'pubcomp',
  retain: false,
  qos: 0,
  dup: false,
  length: 24,
  messageId: 2,
  reasonCode: 16,
  properties: {
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  112, 24, // Header
  0, 2, // Message ID
  16, // reason code
  20, // properties length
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116 // userProperties
]), {protocolVersion: 5})

testParseError('Wrong subscribe header', Buffer.from([
  128, 9, // Header (subscribeqos=0length=9)
  0, 6, // Message ID (6)
  0, 4, // Topic length,
  116, 101, 115, 116, // Topic (test)
  0 // Qos (0)
]))

testParseGenerate('subscribe to one topic', {
  cmd: 'subscribe',
  retain: false,
  qos: 1,
  dup: false,
  length: 9,
  subscriptions: [
    {
      topic: 'test',
      qos: 0
    }
  ],
  messageId: 6
}, Buffer.from([
  130, 9, // Header (subscribeqos=1length=9)
  0, 6, // Message ID (6)
  0, 4, // Topic length,
  116, 101, 115, 116, // Topic (test)
  0 // Qos (0)
]))

testParseGenerate('subscribe to one topic by MQTT 5', {
  cmd: 'subscribe',
  retain: false,
  qos: 1,
  dup: false,
  length: 26,
  subscriptions: [
    {
      topic: 'test',
      qos: 0,
      nl: false,
      rap: true,
      rh: 1
    }
  ],
  messageId: 6,
  properties: {
    subscriptionIdentifier: 145,
    userProperties: {
      test: 'test'
    }
  }
}, Buffer.from([
  130, 26, // Header (subscribeqos=1length=9)
  0, 6, // Message ID (6)
  16, // properties length
  11, 145, 1, // subscriptionIdentifier
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  0, 4, // Topic length,
  116, 101, 115, 116, // Topic (test)
  24 // settings(qos: 0, noLocal: false, Retain as Published: true, retain handling: 1)
]), {protocolVersion: 5})

testParseGenerate('subscribe to three topics', {
  cmd: 'subscribe',
  retain: false,
  qos: 1,
  dup: false,
  length: 23,
  subscriptions: [
    {
      topic: 'test',
      qos: 0
    }, {
      topic: 'uest',
      qos: 1
    }, {
      topic: 'tfst',
      qos: 2
    }
  ],
  messageId: 6
}, Buffer.from([
  130, 23, // Header (publishqos=1length=9)
  0, 6, // Message ID (6)
  0, 4, // Topic length,
  116, 101, 115, 116, // Topic (test)
  0, // Qos (0)
  0, 4, // Topic length
  117, 101, 115, 116, // Topic (uest)
  1, // Qos (1)
  0, 4, // Topic length
  116, 102, 115, 116, // Topic (tfst)
  2 // Qos (2)
]))

testParseGenerate('subscribe to 3 topics by MQTT 5', {
  cmd: 'subscribe',
  retain: false,
  qos: 1,
  dup: false,
  length: 40,
  subscriptions: [
    {
      topic: 'test',
      qos: 0,
      nl: false,
      rap: true,
      rh: 1
    },
    {
      topic: 'uest',
      qos: 1,
      nl: false,
      rap: false,
      rh: 0
    }, {
      topic: 'tfst',
      qos: 2,
      nl: true,
      rap: false,
      rh: 0
    }
  ],
  messageId: 6,
  properties: {
    subscriptionIdentifier: 145,
    userProperties: {
      test: 'test'
    }
  }
}, Buffer.from([
  130, 40, // Header (subscribeqos=1length=9)
  0, 6, // Message ID (6)
  16, // properties length
  11, 145, 1, // subscriptionIdentifier
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  0, 4, // Topic length,
  116, 101, 115, 116, // Topic (test)
  24, // settings(qos: 0, noLocal: false, Retain as Published: true, retain handling: 1)
  0, 4, // Topic length
  117, 101, 115, 116, // Topic (uest)
  1, // Qos (1)
  0, 4, // Topic length
  116, 102, 115, 116, // Topic (tfst)
  6 // Qos (2), No Local: true
]), {protocolVersion: 5})

testParseGenerate('suback', {
  cmd: 'suback',
  retain: false,
  qos: 0,
  dup: false,
  length: 6,
  granted: [0, 1, 2, 128],
  messageId: 6
}, Buffer.from([
  144, 6, // Header
  0, 6, // Message ID
  0, 1, 2, 128 // Granted qos (0, 1, 2) and a rejected being 0x80
]))

testParseGenerate('suback MQTT5', {
  cmd: 'suback',
  retain: false,
  qos: 0,
  dup: false,
  length: 27,
  granted: [0, 1, 2, 128],
  messageId: 6,
  properties: {
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  144, 27, // Header
  0, 6, // Message ID
  20, // properties length
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  0, 1, 2, 128 // Granted qos (0, 1, 2) and a rejected being 0x80
]), {protocolVersion: 5})

testParseGenerate('unsubscribe', {
  cmd: 'unsubscribe',
  retain: false,
  qos: 1,
  dup: false,
  length: 14,
  unsubscriptions: [
    'tfst',
    'test'
  ],
  messageId: 7
}, Buffer.from([
  162, 14,
  0, 7, // Message ID (7)
  0, 4, // Topic length
  116, 102, 115, 116, // Topic (tfst)
  0, 4, // Topic length,
  116, 101, 115, 116 // Topic (test)
]))

testParseGenerate('unsubscribe MQTT 5', {
  cmd: 'unsubscribe',
  retain: false,
  qos: 1,
  dup: false,
  length: 28,
  unsubscriptions: [
    'tfst',
    'test'
  ],
  messageId: 7,
  properties: {
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  162, 28,
  0, 7, // Message ID (7)
  13, // properties length
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  0, 4, // Topic length
  116, 102, 115, 116, // Topic (tfst)
  0, 4, // Topic length,
  116, 101, 115, 116 // Topic (test)
]), {protocolVersion: 5})

testParseGenerate('unsuback', {
  cmd: 'unsuback',
  retain: false,
  qos: 0,
  dup: false,
  length: 2,
  messageId: 8
}, Buffer.from([
  176, 2, // Header
  0, 8 // Message ID
]))

testParseGenerate('unsuback MQTT 5', {
  cmd: 'unsuback',
  retain: false,
  qos: 0,
  dup: false,
  length: 25,
  messageId: 8,
  properties: {
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  },
  granted: [0, 128]
}, Buffer.from([
  176, 25, // Header
  0, 8, // Message ID
  20, // properties length
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  0, 128 // success and error
]), {protocolVersion: 5})

testParseGenerate('pingreq', {
  cmd: 'pingreq',
  retain: false,
  qos: 0,
  dup: false,
  length: 0
}, Buffer.from([
  192, 0 // Header
]))

testParseGenerate('pingresp', {
  cmd: 'pingresp',
  retain: false,
  qos: 0,
  dup: false,
  length: 0
}, Buffer.from([
  208, 0 // Header
]))

testParseGenerate('disconnect', {
  cmd: 'disconnect',
  retain: false,
  qos: 0,
  dup: false,
  length: 0
}, Buffer.from([
  224, 0 // Header
]))

testParseGenerate('disconnect MQTT 5', {
  cmd: 'disconnect',
  retain: false,
  qos: 0,
  dup: false,
  length: 34,
  reasonCode: 0,
  properties: {
    sessionExpiryInterval: 145,
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    },
    serverReference: 'test'
  }
}, Buffer.from([
  224, 34, // Header
  0, // reason code
  32, // properties length
  17, 0, 0, 0, 145, // sessionExpiryInterval
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  28, 0, 4, 116, 101, 115, 116// serverReference
]), {protocolVersion: 5})

testParseGenerate('auth MQTT 5', {
  cmd: 'auth',
  retain: false,
  qos: 0,
  dup: false,
  length: 36,
  reasonCode: 0,
  properties: {
    authenticationMethod: 'test',
    authenticationData: Buffer.from([0, 1, 2, 3]),
    reasonString: 'test',
    userProperties: {
      'test': 'test'
    }
  }
}, Buffer.from([
  240, 36, // Header
  0, // reason code
  34, // properties length
  21, 0, 4, 116, 101, 115, 116, // auth method
  22, 0, 4, 0, 1, 2, 3, // auth data
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116 // userProperties
]), {protocolVersion: 5})

testGenerateError('Unknown command', {})

testGenerateError('Invalid protocolId', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 42,
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: 'password'
})

testGenerateError('clientId must be supplied before 3.1.1', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: true,
  keepalive: 30,
  username: 'username',
  password: 'password'
})

testGenerateError('clientId must be given if cleanSession set to 0', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQTT',
  protocolVersion: 4,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: false,
  keepalive: 30,
  username: 'username',
  password: 'password'
})

testGenerateError('Invalid keepalive', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: true,
  keepalive: 'hello',
  clientId: 'test',
  username: 'username',
  password: 'password'
})

testGenerateError('Invalid keepalive', {
  cmd: 'connect',
  keepalive: 3.1416
})

testGenerateError('Invalid will', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: 42,
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: 'password'
})

testGenerateError('Invalid will topic', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    payload: 'payload'
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: 'password'
})

testGenerateError('Invalid will payload', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 42
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: 'password'
})

testGenerateError('Invalid username', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 42,
  password: 'password'
})

testGenerateError('Invalid password', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  username: 'username',
  password: 42
})

testGenerateError('Username is required to use password', {
  cmd: 'connect',
  retain: false,
  qos: 0,
  dup: false,
  length: 54,
  protocolId: 'MQIsdp',
  protocolVersion: 3,
  will: {
    retain: true,
    qos: 2,
    topic: 'topic',
    payload: 'payload'
  },
  clean: true,
  keepalive: 30,
  clientId: 'test',
  password: 'password'
})

test('support cork', function (t) {
  t.plan(9)

  var dest = WS()

  dest._write = function (chunk, enc, cb) {
    t.pass('_write called')
    cb()
  }

  mqtt.writeToStream({
    cmd: 'connect',
    retain: false,
    qos: 0,
    dup: false,
    length: 18,
    protocolId: 'MQIsdp',
    protocolVersion: 3,
    clean: false,
    keepalive: 30,
    clientId: 'test'
  }, dest)

  dest.end()
})

// The following test case was designed after experiencing errors
// when trying to connect with tls on a non tls mqtt port
// the specific behaviour is:
// - first byte suggests this is a connect message
// - second byte suggests message length to be smaller than buffer length
//   thus payload processing starts
// - the first two bytes suggest a protocol identifier string length
//   that leads the parser pointer close to the end of the buffer
// - when trying to read further connect flags the buffer produces
//   a "out of range" Error
//
testParseError('Packet too short', Buffer.from([
  16, 9,
  0, 6,
  77, 81, 73, 115, 100, 112,
  3
]))

// CONNECT Packets that show other protocol IDs than
// the valid values MQTT and MQIsdp should cause an error
// those packets are a hint that this is not a mqtt connection
testParseError('Invalid protocolId', Buffer.from([
  16, 18,
  0, 6,
  65, 65, 65, 65, 65, 65, // AAAAAA
  3, // Protocol version
  0, // Connect flags
  0, 10, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116 // Client ID
]))

// CONNECT Packets that contain an unsupported protocol version
// Flag (i.e. not `3` or `4`) should cause an error
testParseError('Invalid protocol version', Buffer.from([
  16, 18,
  0, 6,
  77, 81, 73, 115, 100, 112, // Protocol ID
  1, // Protocol version
  0, // Connect flags
  0, 10, // Keepalive
  0, 4, // Client ID length
  116, 101, 115, 116 // Client ID
]))

// When a packet contains a string in the variable header and the
// given string length of this exceeds the overall length of the packet that
// was specified in the fixed header, parsing must fail.
// this case simulates this behavior with the protocol ID string of the
// CONNECT packet. The fixed header suggests a remaining length of 8 bytes
// which would be exceeded by the string length of 15
// in this case, a protocol ID parse error is expected
testParseError('Cannot parse protocolId', Buffer.from([
  16, 8, // Fixed header
  0, 15, // string length 15 --> 15 > 8 --> error!
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112,
  77, 81, 73, 115, 100, 112
]))

testParseError('Unknown property', Buffer.from([
  61, 60, // Header
  0, 4, // Topic length
  116, 101, 115, 116, // Topic (test)
  0, 10, // Message ID
  47, // properties length
  126, 1, // unknown property
  2, 0, 0, 16, 225, // message expiry interval
  35, 0, 100, // topicAlias
  8, 0, 5, 116, 111, 112, 105, 99, // response topic
  9, 0, 4, 1, 2, 3, 4, // correlationData
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116, // userProperties
  11, 120, // subscriptionIdentifier
  3, 0, 4, 116, 101, 115, 116, // content type
  116, 101, 115, 116 // Payload (test)
]), { protocolVersion: 5 })

testParseError('Not supported auth packet for this version MQTT', Buffer.from([
  240, 36, // Header
  0, // reason code
  34, // properties length
  21, 0, 4, 116, 101, 115, 116, // auth method
  22, 0, 4, 0, 1, 2, 3, // auth data
  31, 0, 4, 116, 101, 115, 116, // reasonString
  38, 0, 4, 116, 101, 115, 116, 0, 4, 116, 101, 115, 116 // userProperties
]))

// When a Subscribe packet contains a topic_filter and the given
// length is topic_filter.length + 1 then the last byte (requested QoS) is interpreted as topic_filter
// reading the requested_qos at the end causes 'Index out of range' read
testParseError('Malformed Subscribe Payload', Buffer.from([
  130, 14, // subscribe header and remaining length
  0, 123,  // packet ID
  0, 10,   // topic filter length
  104, 105, 106, 107, 108, 47, 109, 110, 111,  // topic filter with length of 9 bytes
  0       // requested QoS
]))

test('stops parsing after first error', function (t) {
  t.plan(4)

  var parser = mqtt.parser()

  var packetCount = 0
  var errorCount = 0
  var expectedPackets = 1
  var expectedErrors = 1

  parser.on('packet', function (packet) {
    t.ok(++packetCount <= expectedPackets, 'expected <= ' + expectedPackets + ' packets')
  })

  parser.on('error', function (erroneous) {
    t.ok(++errorCount <= expectedErrors, 'expected <= ' + expectedErrors + ' errors')
  })

  parser.parse(Buffer.from([
    // First, a valid connect packet:

    16, 12, // Header
    0, 4, // Protocol ID length
    77, 81, 84, 84, // Protocol ID
    4, // Protocol version
    2, // Connect flags
    0, 30, // Keepalive
    0, 0, // Client ID length

    // Then an invalid subscribe packet:

    128, 9, // Header (subscribeqos=0length=9)
    0, 6, // Message ID (6)
    0, 4, // Topic length,
    116, 101, 115, 116, // Topic (test)
    0, // Qos (0)

    // And another invalid subscribe packet:

    128, 9, // Header (subscribeqos=0length=9)
    0, 6, // Message ID (6)
    0, 4, // Topic length,
    116, 101, 115, 116, // Topic (test)
    0, // Qos (0)

    // Finally, a valid disconnect packet:

    224, 0 // Header
  ]))

  // Calling parse again clears the error and continues parsing
  packetCount = 0
  errorCount = 0
  expectedPackets = 2
  expectedErrors = 0

  parser.parse(Buffer.from([
    // Connect:

    16, 12, // Header
    0, 4, // Protocol ID length
    77, 81, 84, 84, // Protocol ID
    4, // Protocol version
    2, // Connect flags
    0, 30, // Keepalive
    0, 0, // Client ID length

    // Disconnect:

    224, 0 // Header
  ]))
})

testWriteToStreamError('Invalid protocolId', {
  cmd: 'connect',
  protocolId: {}
})

testWriteToStreamError('Invalid topic', {
  cmd: 'publish',
  topic: {}
})

testWriteToStreamError('Invalid messageId', {
  cmd: 'subscribe',
  mid: {}
})
