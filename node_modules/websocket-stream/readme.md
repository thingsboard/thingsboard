# websocket-stream

[![NPM](https://nodei.co/npm/websocket-stream.png?global=true)](https://nodei.co/npm/websocket-stream/)

Use HTML5 [websockets](https://developer.mozilla.org/en-US/docs/WebSockets) using the Node Streams API.

### Usage

This module works in Node or in Browsers that support WebSockets. You can use [browserify](http://github.com/substack/node-browserify) to package this module for browser use.

```javascript
var websocket = require('websocket-stream')
var ws = websocket('ws://echo.websocket.org')
process.stdin.pipe(ws)
ws.pipe(process.stdout)
```

In the example above `ws` is a duplex stream. That means you can pipe output to anything that accepts streams. You can also pipe data into streams (such as a webcam feed or audio data).

The underlying `WebSocket` instance is available as `ws.socket`.

#### Options

The available options differs depending on if you use this module in the browser or with node.js. Options can be passed in as the third or second argument - `WebSocket(address, [protocols], [options])`.

##### `options.browserBufferSize`

How much to allow the [socket.bufferedAmount](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket#Attributes) to grow before starting to throttle writes. This option has no effect in node.js.

Default: `1024 * 512` (512KiB)

##### `options.browserBufferTimeout`

How long to wait before checking if the socket buffer has drained sufficently for another write. This option has no effect in node.js.

Default: `1000` (1 second)

##### `options.objectMode`

Send each chunk on its own, and do not try to pack them in a single
websocket frame.

Default: `false`

##### `options.binary`

Always convert to `Buffer` in Node.js before sending.
Forces `options.objectMode` to `false`.

Default: `true`

##### `options.perMessageDeflate`

We recommend disabling the [per message deflate
extension](https://tools.ietf.org/html/rfc7692) to achieve the best
throughput.

Default: `true` on the client, `false` on the server.

Example:

```js
var websocket = require('websocket-stream')
var ws = websocket('ws://realtimecats.com', {
  perMessageDeflate: false
})
```

Beware that this option is ignored by browser clients. To make sure that permessage-deflate is never used, disable it on the server.

##### Other options

When used in node.js see the [ws.WebSocket documentation](https://github.com/websockets/ws/blob/master/doc/ws.md#class-wswebsocket)

### On the server

Using the [`ws`](http://npmjs.org/ws) module you can make a websocket server and use this module to get websocket streams on the server:

```javascript
var websocket = require('websocket-stream')
var wss = websocket.createServer({server: someHTTPServer}, handle)

function handle(stream, request) {
  // `request` is the upgrade request sent by the client.
  fs.createReadStream('bigdata.json').pipe(stream)
}
```

We recommend disabling the [per message deflate
extension](https://tools.ietf.org/html/rfc7692) to achieve the best
throughput:

```javascript
var websocket = require('websocket-stream')
var wss = websocket.createServer({
  perMessageDeflate: false,
  server: someHTTPServer
}, handle)

function handle(stream) {
  fs.createReadStream('bigdata.json').pipe(stream)
}
```

You can even use it on express.js with the [express-ws](https://www.npmjs.com/package/express-ws) library:

```js
const express = require('express');
const expressWebSocket = require('express-ws');
const websocketStream = require('websocket-stream/stream');
const app = express();

// extend express app with app.ws()
expressWebSocket(app, null, {
    // ws options here
    perMessageDeflate: false,
});
 
app.ws('/bigdata.json', function(ws, req) {
  // convert ws instance to stream
  const stream = websocketStream(ws, {
    // websocket-stream options here
    binary: true,
  });

  fs.createReadStream('bigdata.json').pipe(stream);
});
 
app.listen(3000);
```

## Run the tests

### Server-side tests

```
npm test
```

### Client-side tests

First start the echo server by running `node test-server.js`

Then run `npm start` and open `localhost:9966` in your browser and open the Dev Tools console to see test output.

## license

BSD LICENSE
