'use strict'

var http = require('http')
var websocket = require('./')
var server = null

var port = module.exports.port = 8343
var url = module.exports.url = 'ws://localhost:' + module.exports.port

module.exports.start = function(opts, cb) {
  if (server) {
    cb(new Error('already started'));
    return;
  }

  if (typeof opts == 'function') {
    cb = opts;
    opts = {};
  }

  server = http.createServer()
  opts.server = server

  websocket.createServer(opts, echo)

  server.listen(port, cb)

  function echo(stream) {
    stream.pipe(stream)
  }
}

module.exports.stop = function(cb) {
  if (!server) {
    cb(new Error('not started'))
    return
  }

  server.close(cb)
  server = null
}

if (!module.parent) {
  module.exports.start(function(err) {
    if (err) {
      console.error(err);
      return;
    }
    console.log('Echo server started on port ' + port);
  });
}
