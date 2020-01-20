var http = require('http')
var websocket = require('./')
var echo = require('./echo-server.js')
var WebSocketServer = require('ws').Server
var Buffer = require('safe-buffer').Buffer

echo.start(function(){
  console.log('echo server is running')
})

function forBare (opts) {
  var server = http.createServer()

  websocket.createServer({
    server: server,
    binary: opts.binary
  }, sendString)

  server.listen(opts.port)

  function sendString (stream) {
    stream.write('hello world')
  }
}

forBare({
  port: 8344,
  binary: false
})

forBare({
  port: 8345
})

function checkIfDataIsBinary () {
  var server = http.createServer()
  var wss = new WebSocketServer({
    server: server
  })

  server.listen(8346)

  wss.on('connection', waitFor)

  function waitFor (ws) {
    ws.on('message', function (data) {
      if (!Buffer.isBuffer(data)) {
        ws.send(Buffer.from('fail'))
      } else {
        ws.send(Buffer.from('success'))
      }
    })
  }
}

checkIfDataIsBinary()
