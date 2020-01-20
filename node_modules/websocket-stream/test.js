var test = require('tape')
var websocket = require('./')
var echo = require("./echo-server")
var WebSocketServer = require('ws').Server
var http = require('http')
var concat = require('concat-stream')
var Buffer = require('safe-buffer').Buffer
var ts = require('typescript')

test('echo server', function(t) {

  echo.start(function() {
    var client = websocket(echo.url)

    client.on('error', console.error)

    client.on('data', function(data) {
      t.ok(Buffer.isBuffer(data), 'is a buffer')
      t.equal(data.toString(), 'hello world')
      client.end()
      echo.stop(function() {
        t.end()
      })
    })

    client.write('hello world')
  })

})

test('emitting not connected errors', function(t) {

  echo.start(function() {
    var client = websocket(echo.url)

    client.on('error', function() {
      echo.stop(function() {
        t.true(true, 'should emit error')
        t.end()
      })
    })

    client.once('data', function(data) {
      client.end()
      client.write('abcde')
    })

    client.write('hello world')
  })

})

test('passes options to websocket constructor', function(t) {
  t.plan(3)

  opts = {
    verifyClient: function verifyClient(info) {
      t.equal(info.req.headers['x-custom-header'], 'Custom Value')
      return true
    }
  }
  echo.start(opts, function() {
    var options = {headers: {'x-custom-header': 'Custom Value'}}
    var client = websocket(echo.url, options)

    client.on('error', console.error)

    client.on('data', function(data) {
      t.ok(Buffer.isBuffer(data), 'is a buffer')
      t.equal(data.toString(), 'hello world')
      client.end()
      echo.stop(function() {})
    })

    client.write('hello world')
  })

})


test('destroy', function(t) {
  t.plan(1)

  echo.start(function() {
    var client = websocket(echo.url, echo.options)

    client.on('close', function() {
      echo.stop(function() {
        t.pass('destroyed')
      })
    })

    setTimeout(function() {
      client.destroy()
    }, 200)
  })

})

test('drain', function(t) {
  t.plan(1)

  echo.start(function() {
    var client = websocket(echo.url, echo.options)

    client.on('drain', function() {
      client.destroy()
      echo.stop(function() {
        t.pass('drained')
      })
    })

    // write until buffer is full
    while (client.write('foobar')) {}
  })

})

test('emit sending errors if the socket is closed by the other party', function(t) {

  var server = http.createServer()
  var wss = new WebSocketServer({ server: server })

  server.listen(8344, function() {
    var client = websocket('ws://localhost:8344')

    wss.on('connection', function(ws) {
      var stream = websocket(ws)

      client.destroy()

      setTimeout(function() {
        stream.write('hello world')
      }, 50)

      stream.on('error', function(err) {
        t.ok(err, 'client errors')
        server.close(t.end.bind(t))
      })
    })
  })
})

test('destroy client pipe should close server pipe', function(t) {
  t.plan(1)

  var clientDestroy = function() {
    var client = websocket(echo.url, echo.options)
    client.on('data', function(o) {
      client.destroy()
    })
    client.write(Buffer.from('hello'))
  }

  var opts = {}
  var server = http.createServer()
  opts.server = server
  var wss = new WebSocketServer(opts)
  wss.on('connection', function(ws) {
    var stream = websocket(ws)
    stream.on('close', function() {
      server.close(function() {
        t.pass('close is called')
      })
    })
    stream.pipe(stream)
  })
  server.listen(echo.port, clientDestroy)
})


test('error on socket should forward it to pipe', function(t) {
  t.plan(1)

  var clientConnect = function() {
    websocket(echo.url, echo.options)
  }

  var opts = {}
  var server = http.createServer()
  opts.server = server
  var wss = new WebSocketServer(opts)
  wss.on('connection', function(ws) {
    var stream = websocket(ws)
    stream.on('error', function() {
      server.close(function() {
        t.pass('error is called')
      })
    })
    stream.socket.emit('error', new Error('Fake error'))
  })
  server.listen(echo.port, clientConnect)
})

test('stream end', function(t) {
  t.plan(1)

  var server = http.createServer()
  websocket.createServer({ server: server }, handle)

  function handle (stream) {
    stream.pipe(concat(function (body) {
      t.equal(body.toString(), 'pizza cats\n')
      server.close()
    }))
  }
  server.listen(0, function () {
    var w = websocket('ws://localhost:' + server.address().port)
    w.end('pizza cats\n')
  })
})

test('stream handlers should fire once per connection', function(t) {
  t.plan(2)

  var server = http.createServer()
  var wss = websocket.createServer({ server: server }, function() {
    server.close(function() {
      t.equal(m, 1)
    })
  })

  var m = 0
  wss.on('stream', function(stream, request) {
    t.ok(request instanceof http.IncomingMessage)
    m++
  })
  server.listen(0, function() {
    var w = websocket('ws://localhost:' + server.address().port)
    w.end('pizza cats\n')
  })
})

test('client with writev', function(t) {
  var server = http.createServer()

  var str = ''
  var wss = websocket.createServer({
    server: server
  }, function (stream) {
    stream.once('data', function(data) {
      t.ok(Buffer.isBuffer(data), 'is a buffer')
      t.equal(data.toString(), 'hello world')

      stream.once('data', function(data) {
        t.ok(Buffer.isBuffer(data), 'is a buffer')
        t.equal(data.toString(), str)
        stream.end()
        server.close()
        t.end()
      })
    })
  })

  server.listen(8352, function () {
    var client = websocket('ws://localhost:8352', {
      objectMode: false
    })

    client.on('error', console.error)

    client.once('connect', function () {
      client.cork()
      do {
        str += 'foobar'
      } while (client.write('foobar'))
      client.uncork()
    })

    client.write('hello world')
  })
})

test('server with writev', function(t) {
  var server = http.createServer()

  var str = ''
  var wss = websocket.createServer({
    server: server,
    objectMode: false
  }, function (stream) {
    stream.cork()
    do {
      str += 'foobar'
    } while (stream.write('foobar'))
    stream.uncork()
  })

  server.listen(8352, function () {
    var client = websocket('ws://localhost:8352')

    client.on('error', console.error)

    client.once('data', function(data) {
      t.ok(Buffer.isBuffer(data), 'is a buffer')
      t.equal(data.toString(), str)
      client.end()
      server.close()
      t.end()
    })
  })
})

test('stop echo', function(t) {
  echo.stop(function() {
    t.end()
  })
})

test('typescript compilation', function(t) {
  function compile(fileNames, options) {
    const program = ts.createProgram(fileNames, options)
    const emitResult = program.emit()

    const allDiagnostics = ts.getPreEmitDiagnostics(program).concat(emitResult.diagnostics)

    return allDiagnostics.map(diagnostic => {
      if (diagnostic.file) {
        let { line, character } = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start)
        let message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n")
        return `${diagnostic.file.fileName} (${line + 1},${character + 1}): ${message}`
      } else {
        return `${ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n")}`
      }
    });
  }

  const messages = compile(['./ts-tests.ts'], {
    noEmit: true,
    noImplicitAny: true,
    target: ts.ScriptTarget.ES5,
    module: ts.ModuleKind.CommonJS
  })

  t.equal(messages.length, 0, 'no errors emitted')
  t.end()
})

