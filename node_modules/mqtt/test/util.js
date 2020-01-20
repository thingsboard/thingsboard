'use strict'

var through = require('through2')

module.exports.testStream = function () {
  return through(function (buf, enc, cb) {
    var that = this
    setImmediate(function () {
      that.push(buf)
      cb()
    })
  })
}
