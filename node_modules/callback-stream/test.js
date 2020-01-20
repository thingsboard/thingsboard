'use strict'

var test = require('tap').test
var callback = require('./')
var fs = require('fs')

test('call the callback after end with object mode', function (t) {
  var opts = { objectMode: true }
  var stream = callback(opts, function (err, results) {
    t.deepEqual(results, ['hello'], 'should return the ending value')
    t.end()
  })

  stream.end('hello')
})

test('support multiple writes with object mode', function (t) {
  var opts = { objectMode: true }
  var stream = callback(opts, function (err, results) {
    t.deepEqual(results, ['hello', 'world'], 'should return the ending value')
    t.end()
  })

  stream.write('hello')
  stream.end('world')
})

test('works without object mode', function (t) {
  var stream = callback(function (err, results) {
    t.equal(results.length, 1, 'should contain only one value')
    t.deepEqual(results[0].toString(), 'world', 'should return the ending value')
    t.end()
  })

  stream.end('world')
})

test('is pipeable', function (t) {
  var write = callback(function (err, results) {
    var actual = Buffer.concat(results).toString()
    var expected = fs.readFileSync('README.md').toString()
    t.equal(actual, expected, 'should have the same content of the file')
    t.end()
  })
  var read = fs.createReadStream('README.md')

  read.pipe(write)
})

test('callback.obj shortcut for objectMode', function (t) {
  var stream = callback.obj(function (err, results) {
    t.deepEqual(results, ['hello'], 'should return the ending value')
    t.end()
  })

  stream.end('hello')
})
