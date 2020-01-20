'use strict'

var Writable = require('readable-stream').Writable
var inherits = require('inherits')

function CallbackStream (options, callback) {
  if (!(this instanceof CallbackStream)) {
    return new CallbackStream(options, callback)
  }

  if (typeof options === 'function') {
    callback = options
    options = {}
  }

  Writable.call(this, options)

  this.results = []
  this.callback = callback

  this.on('finish', deliversCallback)
  this.once('pipe', handlePipe)
}

function deliversCallback () {
  this.callback(null, this.results)
}

function handlePipe (source) {
  source.on('error', this.callback)
}

inherits(CallbackStream, Writable)

CallbackStream.prototype._write = function (data, encoding, done) {
  this.results.push(data)
  done()
}

CallbackStream.obj = function (options, callback) {
  if (typeof options === 'function') {
    callback = options
    options = {}
  }

  options.objectMode = true

  return new CallbackStream(options, callback)
}

module.exports = CallbackStream
