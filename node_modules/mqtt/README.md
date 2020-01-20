![mqtt.js](https://raw.githubusercontent.com/mqttjs/MQTT.js/137ee0e3940c1f01049a30248c70f24dc6e6f829/MQTT.js.png)
=======

[![Build Status](https://travis-ci.org/mqttjs/MQTT.js.svg)](https://travis-ci.org/mqttjs/MQTT.js) [![codecov](https://codecov.io/gh/mqttjs/MQTT.js/branch/master/graph/badge.svg)](https://codecov.io/gh/mqttjs/MQTT.js)

[![NPM](https://nodei.co/npm-dl/mqtt.png)](https://nodei.co/npm/mqtt/) [![NPM](https://nodei.co/npm/mqtt.png)](https://nodei.co/npm/mqtt/)

[![Sauce Test Status](https://saucelabs.com/browser-matrix/mqttjs.svg)](https://saucelabs.com/u/mqttjs)

MQTT.js is a client library for the [MQTT](http://mqtt.org/) protocol, written
in JavaScript for node.js and the browser.

* [Upgrade notes](#notes)
* [Installation](#install)
* [Example](#example)
* [Command Line Tools](#cli)
* [API](#api)
* [Browser](#browser)
* [Weapp](#weapp)
* [About QoS](#qos)
* [TypeScript](#typescript)
* [Contributing](#contributing)
* [License](#license)

MQTT.js is an OPEN Open Source Project, see the [Contributing](#contributing) section to find out what this means.

[![JavaScript Style
Guide](https://cdn.rawgit.com/feross/standard/master/badge.svg)](https://github.com/feross/standard)


<a name="notes"></a>
## Important notes for existing users

v2.0.0 removes support for node v0.8, v0.10 and v0.12, and it is 3x faster in sending
packets. It also removes all the deprecated functionality in v1.0.0,
mainly `mqtt.createConnection` and `mqtt.Server`. From v2.0.0,
subscriptions are restored upon reconnection if `clean: true`.
v1.x.x is now in *LTS*, and it will keep being supported as long as
there are v0.8, v0.10 and v0.12 users.

v1.0.0 improves the overall architecture of the project, which is now
split into three components: MQTT.js keeps the Client,
[mqtt-connection](http://npm.im/mqtt-connection) includes the barebone
Connection code for server-side usage, and [mqtt-packet](http://npm.im/mqtt-packet)
includes the protocol parser and generator. The new Client improves
performance by a 30% factor, embeds Websocket support
([MOWS](http://npm.im/mows) is now deprecated), and it has a better
support for QoS 1 and 2. The previous API is still supported but
deprecated, as such, it is not documented in this README.

As a __breaking change__, the `encoding` option in the old client is
removed, and now everything is UTF-8 with the exception of the
`password` in the CONNECT message and `payload` in the PUBLISH message,
which are `Buffer`.

Another __breaking change__ is that MQTT.js now defaults to MQTT v3.1.1,
so to support old brokers, please read the [client options doc](#client).

MQTT v5 support is experimental as it has not been implemented by brokers yet.

<a name="install"></a>
## Installation

```sh
npm install mqtt --save
```

<a name="example"></a>
## Example

For the sake of simplicity, let's put the subscriber and the publisher in the same file:

```js
var mqtt = require('mqtt')
var client  = mqtt.connect('mqtt://test.mosquitto.org')

client.on('connect', function () {
  client.subscribe('presence', function (err) {
    if (!err) {
      client.publish('presence', 'Hello mqtt')
    }
  })
})

client.on('message', function (topic, message) {
  // message is Buffer
  console.log(message.toString())
  client.end()
})
```

output:
```
Hello mqtt
```

If you want to run your own MQTT broker, you can use
[Mosquitto](http://mosquitto.org) or
[Mosca](http://mcollina.github.io/mosca/), and launch it.
You can also use a test instance: test.mosquitto.org and test.mosca.io
are both public.

If you do not want to install a separate broker, you can try using the
[mqtt-connection](https://www.npmjs.com/package/mqtt-connection).

to use MQTT.js in the browser see the [browserify](#browserify) section

<a name="promises"></a>
## Promise support

If you want to use the new [async-await](https://blog.risingstack.com/async-await-node-js-7-nightly/) functionality in JavaScript, or just prefer using Promises instead of callbacks, [async-mqtt](https://github.com/mqttjs/async-mqtt) is a wrapper over MQTT.js which uses promises instead of callbacks when possible.

<a name="cli"></a>
## Command Line Tools

MQTT.js bundles a command to interact with a broker.
In order to have it available on your path, you should install MQTT.js
globally:

```sh
npm install mqtt -g
```

Then, on one terminal

```
mqtt sub -t 'hello' -h 'test.mosquitto.org' -v
```

On another

```
mqtt pub -t 'hello' -h 'test.mosquitto.org' -m 'from MQTT.js'
```

See `mqtt help <command>` for the command help.

<a name="api"></a>
## API

  * <a href="#connect"><code>mqtt.<b>connect()</b></code></a>
  * <a href="#client"><code>mqtt.<b>Client()</b></code></a>
  * <a href="#publish"><code>mqtt.Client#<b>publish()</b></code></a>
  * <a href="#subscribe"><code>mqtt.Client#<b>subscribe()</b></code></a>
  * <a href="#unsubscribe"><code>mqtt.Client#<b>unsubscribe()</b></code></a>
  * <a href="#end"><code>mqtt.Client#<b>end()</b></code></a>
  * <a href="#removeOutgoingMessage"><code>mqtt.Client#<b>removeOutgoingMessage()</b></code></a>
  * <a href="#reconnect"><code>mqtt.Client#<b>reconnect()</b></code></a>
  * <a href="#handleMessage"><code>mqtt.Client#<b>handleMessage()</b></code></a>
  * <a href="#connected"><code>mqtt.Client#<b>connected</b></code></a>
  * <a href="#reconnecting"><code>mqtt.Client#<b>reconnecting</b></code></a>
  * <a href="#getLastMessageId"><code>mqtt.Client#<b>getLastMessageId()</b></code></a>
  * <a href="#store"><code>mqtt.<b>Store()</b></code></a>
  * <a href="#put"><code>mqtt.Store#<b>put()</b></code></a>
  * <a href="#del"><code>mqtt.Store#<b>del()</b></code></a>
  * <a href="#createStream"><code>mqtt.Store#<b>createStream()</b></code></a>
  * <a href="#close"><code>mqtt.Store#<b>close()</b></code></a>

-------------------------------------------------------
<a name="connect"></a>
### mqtt.connect([url], options)

Connects to the broker specified by the given url and options and
returns a [Client](#client).

The URL can be on the following protocols: 'mqtt', 'mqtts', 'tcp',
'tls', 'ws', 'wss'. The URL can also be an object as returned by
[`URL.parse()`](http://nodejs.org/api/url.html#url_url_parse_urlstr_parsequerystring_slashesdenotehost),
in that case the two objects are merged, i.e. you can pass a single
object with both the URL and the connect options.

You can also specify a `servers` options with content: `[{ host:
'localhost', port: 1883 }, ... ]`, in that case that array is iterated
at every connect.

For all MQTT-related options, see the [Client](#client)
constructor.

-------------------------------------------------------
<a name="client"></a>
### mqtt.Client(streamBuilder, options)

The `Client` class wraps a client connection to an
MQTT broker over an arbitrary transport method (TCP, TLS,
WebSocket, ecc).

`Client` automatically handles the following:

* Regular server pings
* QoS flow
* Automatic reconnections
* Start publishing before being connected

The arguments are:

* `streamBuilder` is a function that returns a subclass of the `Stream` class that supports
the `connect` event. Typically a `net.Socket`.
* `options` is the client connection options (see: the [connect packet](https://github.com/mcollina/mqtt-packet#connect)). Defaults:
  * `wsOptions`: is the WebSocket connection options. Default is `{}`.
     It's specific for WebSockets. For possible options have a look at: https://github.com/websockets/ws/blob/master/doc/ws.md.
  * `keepalive`: `60` seconds, set to `0` to disable
  * `reschedulePings`: reschedule ping messages after sending packets (default `true`)
  * `clientId`: `'mqttjs_' + Math.random().toString(16).substr(2, 8)`
  * `protocolId`: `'MQTT'`
  * `protocolVersion`: `4`
  * `clean`: `true`, set to false to receive QoS 1 and 2 messages while
    offline
  * `reconnectPeriod`: `1000` milliseconds, interval between two
    reconnections
  * `connectTimeout`: `30 * 1000` milliseconds, time to wait before a
    CONNACK is received
  * `username`: the username required by your broker, if any
  * `password`: the password required by your broker, if any
  * `incomingStore`: a [Store](#store) for the incoming packets
  * `outgoingStore`: a [Store](#store) for the outgoing packets
  * `queueQoSZero`: if connection is broken, queue outgoing QoS zero messages (default `true`)
  * `customHandleAcks`: MQTT 5 feature of custom handling puback and pubrec packets. Its callback:
      ```js
        customHandleAcks: function(topic, message, packet, done) {*some logic wit colling done(error, reasonCode)*}
      ```
  * `properties`: properties MQTT 5.0.
  `object` that supports the following properties:
    * `sessionExpiryInterval`: representing the Session Expiry Interval in seconds `number`,
    * `receiveMaximum`: representing the Receive Maximum value `number`,
    * `maximumPacketSize`: representing the Maximum Packet Size the Client is willing to accept `number`,
    * `topicAliasMaximum`: representing the Topic Alias Maximum value indicates the highest value that the Client will accept as a Topic Alias sent by the Server `number`,
    * `requestResponseInformation`: The Client uses this value to request the Server to return Response Information in the CONNACK `boolean`,
    * `requestProblemInformation`: The Client uses this value to indicate whether the Reason String or User Properties are sent in the case of failures `boolean`,
    * `userProperties`: The User Property is allowed to appear multiple times to represent multiple name, value pairs `object`,
    * `authenticationMethod`: the name of the authentication method used for extended authentication `string`,
    * `authenticationData`: Binary Data containing authentication data `binary`
  * `authPacket`: settings for auth packet `object`
  * `will`: a message that will sent by the broker automatically when
     the client disconnect badly. The format is:
    * `topic`: the topic to publish
    * `payload`: the message to publish
    * `qos`: the QoS
    * `retain`: the retain flag
    * `properties`: properties of will by MQTT 5.0:
      * `willDelayInterval`: representing the Will Delay Interval in seconds `number`,
      * `payloadFormatIndicator`: Will Message is UTF-8 Encoded Character Data or not `boolean`,
      * `messageExpiryInterval`: value is the lifetime of the Will Message in seconds and is sent as the Publication Expiry Interval when the Server publishes the Will Message `number`,
      * `contentType`: describing the content of the Will Message `string`,
      * `responseTopic`: String which is used as the Topic Name for a response message `string`,
      * `correlationData`: The Correlation Data is used by the sender of the Request Message to identify which request the Response Message is for when it is received `binary`,
      * `userProperties`: The User Property is allowed to appear multiple times to represent multiple name, value pairs `object`
  * `transformWsUrl` : optional `(url, options, client) => url` function
        For ws/wss protocols only. Can be used to implement signing
        urls which upon reconnect can have become expired.
  * `resubscribe` : if connection is broken and reconnects,
     subscribed topics are automatically subscribed again (default `true`)

In case mqtts (mqtt over tls) is required, the `options` object is
passed through to
[`tls.connect()`](http://nodejs.org/api/tls.html#tls_tls_connect_options_callback).
If you are using a **self-signed certificate**, pass the `rejectUnauthorized: false` option.
Beware that you are exposing yourself to man in the middle attacks, so it is a configuration
that is not recommended for production environments.

If you are connecting to a broker that supports only MQTT 3.1 (not
3.1.1 compliant), you should pass these additional options:

```js
{
  protocolId: 'MQIsdp',
  protocolVersion: 3
}
```

This is confirmed on RabbitMQ 3.2.4, and on Mosquitto < 1.3. Mosquitto
version 1.3 and 1.4 works fine without those.

#### Event `'connect'`

`function (connack) {}`

Emitted on successful (re)connection (i.e. connack rc=0).
* `connack` received connack packet. When `clean` connection option is `false` and server has a previous session
for `clientId` connection option, then `connack.sessionPresent` flag is `true`. When that is the case,
you may rely on stored session and prefer not to send subscribe commands for the client.

#### Event `'reconnect'`

`function () {}`

Emitted when a reconnect starts.

#### Event `'close'`

`function () {}`

Emitted after a disconnection.

#### Event `'disconnect'`

`function (packet) {}`

Emitted after receiving disconnect packet from broker. MQTT 5.0 feature.

#### Event `'offline'`

`function () {}`

Emitted when the client goes offline.

#### Event `'error'`

`function (error) {}`

Emitted when the client cannot connect (i.e. connack rc != 0) or when a
parsing error occurs.

#### Event `'end'`

`function () {}`

Emitted when <a href="#end"><code>mqtt.Client#<b>end()</b></code></a> is called.
If a callback was passed to `mqtt.Client#end()`, this event is emitted once the
callback returns.

#### Event `'message'`

`function (topic, message, packet) {}`

Emitted when the client receives a publish packet
* `topic` topic of the received packet
* `message` payload of the received packet
* `packet` received packet, as defined in
  [mqtt-packet](https://github.com/mcollina/mqtt-packet#publish)

#### Event `'packetsend'`

`function (packet) {}`

Emitted when the client sends any packet. This includes .published() packets
as well as packets used by MQTT for managing subscriptions and connections
* `packet` received packet, as defined in
  [mqtt-packet](https://github.com/mcollina/mqtt-packet)

#### Event `'packetreceive'`

`function (packet) {}`

Emitted when the client receives any packet. This includes packets from
subscribed topics as well as packets used by MQTT for managing subscriptions
and connections
* `packet` received packet, as defined in
  [mqtt-packet](https://github.com/mcollina/mqtt-packet)

-------------------------------------------------------
<a name="publish"></a>
### mqtt.Client#publish(topic, message, [options], [callback])

Publish a message to a topic

* `topic` is the topic to publish to, `String`
* `message` is the message to publish, `Buffer` or `String`
* `options` is the options to publish with, including:
  * `qos` QoS level, `Number`, default `0`
  * `retain` retain flag, `Boolean`, default `false`
  * `dup` mark as duplicate flag, `Boolean`, default `false`
  * `properties`: MQTT 5.0 properties `object`
    * `payloadFormatIndicator`: Payload is UTF-8 Encoded Character Data or not `boolean`,
    * `messageExpiryInterval`: the lifetime of the Application Message in seconds `number`,
    * `topicAlias`: value that is used to identify the Topic instead of using the Topic Name `number`,
    * `responseTopic`: String which is used as the Topic Name for a response message `string`,
    * `correlationData`: used by the sender of the Request Message to identify which request the Response Message is for when it is received `binary`,
    * `userProperties`: The User Property is allowed to appear multiple times to represent multiple name, value pairs `object`,
    * `subscriptionIdentifier`: representing the identifier of the subscription `number`,
    * `contentType`: String describing the content of the Application Message `string`
  * `cbStorePut` - `function ()`, fired when message is put into `outgoingStore` if QoS is `1` or `2`.
* `callback` - `function (err)`, fired when the QoS handling completes,
  or at the next tick if QoS 0. An error occurs if client is disconnecting.

-------------------------------------------------------
<a name="subscribe"></a>
### mqtt.Client#subscribe(topic/topic array/topic object, [options], [callback])

Subscribe to a topic or topics

* `topic` is a `String` topic to subscribe to or an `Array` of
  topics to subscribe to. It can also be an object, it has as object
  keys the topic name and as value the QoS, like `{'test1': {qos: 0}, 'test2': {qos: 1}}`.
  MQTT `topic` wildcard characters are supported (`+` - for single level and `#` - for multi level)
* `options` is the options to subscribe with, including:
  * `qos` qos subscription level, default 0
  * `nl` No Local MQTT 5.0 flag (If the value is true, Application Messages MUST NOT be forwarded to a connection with a ClientID equal to the ClientID of the publishing connection)
  * `rap` Retain as Published MQTT 5.0 flag (If true, Application Messages forwarded using this subscription keep the RETAIN flag they were published with. If false, Application Messages forwarded using this subscription have the RETAIN flag set to 0.)
  * `rh` Retain Handling MQTT 5.0 (This option specifies whether retained messages are sent when the subscription is established.)
  * `properties`: `object`
    * `subscriptionIdentifier`:  representing the identifier of the subscription `number`,
    * `userProperties`: The User Property is allowed to appear multiple times to represent multiple name, value pairs `object`
* `callback` - `function (err, granted)`
  callback fired on suback where:
  * `err` a subscription error or an error that occurs when client is disconnecting
  * `granted` is an array of `{topic, qos}` where:
    * `topic` is a subscribed to topic
    * `qos` is the granted qos level on it

-------------------------------------------------------
<a name="unsubscribe"></a>
### mqtt.Client#unsubscribe(topic/topic array, [options], [callback])

Unsubscribe from a topic or topics

* `topic` is a `String` topic or an array of topics to unsubscribe from
* `options`: options of unsubscribe.
  * `properties`: `object`
      * `userProperties`: The User Property is allowed to appear multiple times to represent multiple name, value pairs `object`
* `callback` - `function (err)`, fired on unsuback. An error occurs if client is disconnecting.

-------------------------------------------------------
<a name="end"></a>
### mqtt.Client#end([force], [options], [cb])

Close the client, accepts the following options:

* `force`: passing it to true will close the client right away, without
  waiting for the in-flight messages to be acked. This parameter is
  optional.
* `options`: options of disconnect.
  * `reasonCode`: Disconnect Reason Code `number`
  * `properties`: `object`
    * `sessionExpiryInterval`: representing the Session Expiry Interval in seconds `number`,
    * `reasonString`: representing the reason for the disconnect `string`,
    * `userProperties`: The User Property is allowed to appear multiple times to represent multiple name, value pairs `object`,
    * `serverReference`: String which can be used by the Client to identify another Server to use `string`
* `cb`: will be called when the client is closed. This parameter is
  optional.

-------------------------------------------------------
<a name="removeOutgoingMessage"></a>
### mqtt.Client#removeOutgoingMessage(mid)

Remove a message from the outgoingStore.
The outgoing callback will be called with Error('Message removed') if the message is removed.

After this function is called, the messageId is released and becomes reusable.

* `mid`: The messageId of the message in the outgoingStore.

-------------------------------------------------------
<a name="reconnect"></a>
### mqtt.Client#reconnect()

Connect again using the same options as connect()

-------------------------------------------------------
<a name="handleMessage"></a>
### mqtt.Client#handleMessage(packet, callback)

Handle messages with backpressure support, one at a time.
Override at will, but __always call `callback`__, or the client
will hang.

-------------------------------------------------------
<a name="connected"></a>
### mqtt.Client#connected

Boolean : set to `true` if the client is connected. `false` otherwise.

-------------------------------------------------------
<a name="getLastMessageId"></a>
### mqtt.Client#getLastMessageId()

Number : get last message id. This is for sent messages only.

-------------------------------------------------------
<a name="reconnecting"></a>
### mqtt.Client#reconnecting

Boolean : set to `true` if the client is trying to reconnect to the server. `false` otherwise.

-------------------------------------------------------
<a name="store"></a>
### mqtt.Store(options)

In-memory implementation of the message store.

* `options` is the store options:
  * `clean`: `true`, clean inflight messages when close is called (default `true`)

Other implementations of `mqtt.Store`:

* [mqtt-level-store](http://npm.im/mqtt-level-store) which uses
  [Level-browserify](http://npm.im/level-browserify) to store the inflight
  data, making it usable both in Node and the Browser.
* [mqtt-nedbb-store](https://github.com/behrad/mqtt-nedb-store) which
  uses [nedb](https://www.npmjs.com/package/nedb) to store the inflight
  data.
* [mqtt-localforage-store](http://npm.im/mqtt-localforage-store) which uses
  [localForage](http://npm.im/localforage) to store the inflight
  data, making it usable in the Browser without browserify.

-------------------------------------------------------
<a name="put"></a>
### mqtt.Store#put(packet, callback)

Adds a packet to the store, a packet is
anything that has a `messageId` property.
The callback is called when the packet has been stored.

-------------------------------------------------------
<a name="createStream"></a>
### mqtt.Store#createStream()

Creates a stream with all the packets in the store.

-------------------------------------------------------
<a name="del"></a>
### mqtt.Store#del(packet, cb)

Removes a packet from the store, a packet is
anything that has a `messageId` property.
The callback is called when the packet has been removed.

-------------------------------------------------------
<a name="close"></a>
### mqtt.Store#close(cb)

Closes the Store.

<a name="browser"></a>
## Browser

<a name="cdn"></a>
### Via CDN

The MQTT.js bundle is available through http://unpkg.com, specifically
at https://unpkg.com/mqtt/dist/mqtt.min.js.
See http://unpkg.com for the full documentation on version ranges.

<a name="weapp"></a>
## WeChat Mini Program
Support [WeChat Mini Program](https://mp.weixin.qq.com/). See [Doc](https://mp.weixin.qq.com/debug/wxadoc/dev/api/network-socket.html).
<a name="example"></a>

## Example(js)

```js
var mqtt = require('mqtt')
var client = mqtt.connect('wxs://test.mosquitto.org')
```

## Example(ts)

```ts
import { connect } from 'mqtt';
const client = connect('wxs://test.mosquitto.org');
```

## Ali Mini Program
Surport [Ali Mini Program](https://open.alipay.com/channel/miniIndex.htm). See [Doc](https://docs.alipay.com/mini/developer/getting-started).
<a name="example"></a>

## Example(js)

```js
var mqtt = require('mqtt')
var client = mqtt.connect('alis://test.mosquitto.org')
```

## Example(ts)

```ts
import { connect } from 'mqtt';
const client  = connect('alis://test.mosquitto.org');
```

<a name="browserify"></a>
### Browserify

In order to use MQTT.js as a browserify module you can either require it in your browserify bundles or build it as a stand alone module. The exported module is AMD/CommonJs compatible and it will add an object in the global space.

```javascript
npm install -g browserify // install browserify
cd node_modules/mqtt
npm install . // install dev dependencies
browserify mqtt.js -s mqtt > browserMqtt.js // require mqtt in your client-side app
```

<a name="webpack"></a>
### Webpack

Just like browserify, export MQTT.js as library. The exported module would be `var mqtt = xxx` and it will add an object in the global space. You could also export module in other [formats (AMD/CommonJS/others)](http://webpack.github.io/docs/configuration.html#output-librarytarget) by setting **output.libraryTarget** in webpack configuration.

```javascript
npm install -g webpack // install webpack

cd node_modules/mqtt
npm install . // install dev dependencies
webpack mqtt.js ./browserMqtt.js --output-library mqtt
```

you can then use mqtt.js in the browser with the same api than node's one.

```html
<html>
<head>
  <title>test Ws mqtt.js</title>
</head>
<body>
<script src="./browserMqtt.js"></script>
<script>
  var client = mqtt.connect() // you add a ws:// url here
  client.subscribe("mqtt/demo")

  client.on("message", function (topic, payload) {
    alert([topic, payload].join(": "))
    client.end()
  })

  client.publish("mqtt/demo", "hello world!")
</script>
</body>
</html>
```

Your broker should accept websocket connection (see [MQTT over Websockets](https://github.com/mcollina/mosca/wiki/MQTT-over-Websockets) to setup [Mosca](http://mcollina.github.io/mosca/)).

<a name="signedurls"></a>
### Signed WebSocket Urls

If you need to sign an url, for example for [AWS IoT](http://docs.aws.amazon.com/iot/latest/developerguide/protocols.html#mqtt-ws),
then you can pass in a `transformWsUrl` function to the <a href="#connect"><code>mqtt.<b>connect()</b></code></a> options
This is needed because signed urls have an expiry and eventually upon reconnects, a new signed url needs to be created:

```js
// This module doesn't actually exist, just an example
var awsIotUrlSigner = require('awsIotUrlSigner')
mqtt.connect('wss://a2ukbzaqo9vbpb.iot.ap-southeast-1.amazonaws.com/mqtt', {
  transformWsUrl: function (url, options, client) {
    // It's possible to inspect some state on options(pre parsed url components)
    // and the client (reconnect state etc)
    return awsIotUrlSigner(url)
  }
})

// Now every time a new WebSocket connection is opened (hopefully not that
// often) we get a freshly signed url

```

<a name="qos"></a>
## About QoS

Here is how QoS works:

* QoS 0 : received **at most once** : The packet is sent, and that's it. There is no validation about whether it has been received.
* QoS 1 : received **at least once** : The packet is sent and stored as long as the client has not received a confirmation from the server. MQTT ensures that it *will* be received, but there can be duplicates.
* QoS 2 : received **exactly once** : Same as QoS 1 but there is no duplicates.

About data consumption, obviously, QoS 2 > QoS 1 > QoS 0, if that's a concern to you.

<a name="typescript"></a>
## Usage with TypeScript
This repo bundles TypeScript definition files for use in TypeScript projects and to support tools that can read `.d.ts` files.

### Pre-requisites
Before you can begin using these TypeScript definitions with your project, you need to make sure your project meets a few of these requirements:
 * TypeScript >= 2.1
 * Set tsconfig.json: `{"compilerOptions" : {"moduleResolution" : "node"}, ...}`
 * Includes the TypeScript definitions for node. You can use npm to install this by typing the following into a terminal window:
   `npm install --save-dev @types/node`

<a name="contributing"></a>
## Contributing

MQTT.js is an **OPEN Open Source Project**. This means that:

> Individuals making significant and valuable contributions are given commit-access to the project to contribute as they see fit. This project is more like an open wiki than a standard guarded open source project.

See the [CONTRIBUTING.md](https://github.com/mqttjs/MQTT.js/blob/master/CONTRIBUTING.md) file for more details.

### Contributors

MQTT.js is only possible due to the excellent work of the following contributors:

<table><tbody>
<tr><th align="left">Adam Rudd</th><td><a href="https://github.com/adamvr">GitHub/adamvr</a></td><td><a href="http://twitter.com/adam_vr">Twitter/@adam_vr</a></td></tr>
<tr><th align="left">Matteo Collina</th><td><a href="https://github.com/mcollina">GitHub/mcollina</a></td><td><a href="http://twitter.com/matteocollina">Twitter/@matteocollina</a></td></tr>
<tr><th align="left">Maxime Agor</th><td><a href="https://github.com/4rzael">GitHub/4rzael</a></td><td><a href="http://twitter.com/4rzael">Twitter/@4rzael</a></td></tr>
<tr><th align="left">Siarhei Buntsevich</th><td><a href="https://github.com/scarry1992">GitHub/scarry1992</a></td></tr>
</tbody></table>

<a name="license"></a>
## License

MIT
