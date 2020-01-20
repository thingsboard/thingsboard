'use strict'

var WebSocketServer = require('ws').Server
var stream = require('./stream')

class Server extends WebSocketServer{
  constructor(opts, cb) {
    super(opts)

    var proxied = false
    this.on('newListener', function(event) {
      if (!proxied && event === 'stream') {
        proxied = true
        this.on('connection', function(conn, req) {
          this.emit('stream', stream(conn, opts), req)
        })
      }
    })

    if (cb) {
      this.on('stream', cb)
    }
  }
}

module.exports.Server = Server
module.exports.createServer = function(opts, cb) {
  return new Server(opts, cb)
}
