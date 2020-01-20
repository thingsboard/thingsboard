'use strict'

var test = require('tape')
var concat = require('concat-stream')
var fs = require('fs')
var helpMe = require('./')

test('show the doc/help.txt from the require.main folder if no options are passed', function (t) {
  t.plan(2)

  helpMe()
    .createStream()
    .pipe(concat(function (data) {
      fs.readFile('./doc/help.txt', function (err, expected) {
        t.error(err)
        t.equal(data.toString(), expected.toString())
      })
    }))
})

test('show a generic help.txt from a folder to a stream', function (t) {
  t.plan(2)

  helpMe({
    dir: 'fixture/basic'
  }).createStream()
    .pipe(concat(function (data) {
      fs.readFile('fixture/basic/help.txt', function (err, expected) {
        t.error(err)
        t.equal(data.toString(), expected.toString())
      })
    }))
})

test('custom help command with an array', function (t) {
  t.plan(2)

  helpMe({
    dir: 'fixture/basic'
  }).createStream(['hello'])
    .pipe(concat(function (data) {
      fs.readFile('fixture/basic/hello.txt', function (err, expected) {
        t.error(err)
        t.equal(data.toString(), expected.toString())
      })
    }))
})

test('custom help command without an ext', function (t) {
  t.plan(2)

  helpMe({
    dir: 'fixture/no-ext',
    ext: ''
  }).createStream(['hello'])
    .pipe(concat(function (data) {
      fs.readFile('fixture/no-ext/hello', function (err, expected) {
        t.error(err)
        t.equal(data.toString(), expected.toString())
      })
    }))
})

test('custom help command with a string', function (t) {
  t.plan(2)

  helpMe({
    dir: 'fixture/basic'
  }).createStream('hello')
    .pipe(concat(function (data) {
      fs.readFile('fixture/basic/hello.txt', function (err, expected) {
        t.error(err)
        t.equal(data.toString(), expected.toString())
      })
    }))
})

test('missing help file', function (t) {
  t.plan(1)

  helpMe({
    dir: 'fixture/basic'
  }).createStream('abcde')
    .on('error', function (err) {
      t.equal(err.message, 'no such help file')
    })
    .resume()
})

test('custom help command with an array', function (t) {
  var helper = helpMe({
    dir: 'fixture/shortnames'
  })

  t.test('abbreviates two words in one', function (t) {
    t.plan(2)

    helper
      .createStream(['world'])
      .pipe(concat(function (data) {
        fs.readFile('fixture/shortnames/hello world.txt', function (err, expected) {
          t.error(err)
          t.equal(data.toString(), expected.toString())
        })
      }))
  })

  t.test('abbreviates three words in two', function (t) {
    t.plan(2)

    helper
      .createStream(['abcde', 'fghi'])
      .pipe(concat(function (data) {
        fs.readFile('fixture/shortnames/abcde fghi lmno.txt', function (err, expected) {
          t.error(err)
          t.equal(data.toString(), expected.toString())
        })
      }))
  })

  t.test('abbreviates a word', function (t) {
    t.plan(2)

    helper
      .createStream(['abc', 'fg'])
      .pipe(concat(function (data) {
        fs.readFile('fixture/shortnames/abcde fghi lmno.txt', function (err, expected) {
          t.error(err)
          t.equal(data.toString(), expected.toString())
        })
      }))
  })

  t.test('abbreviates a word using strings', function (t) {
    t.plan(2)

    helper
      .createStream('abc fg')
      .pipe(concat(function (data) {
        fs.readFile('fixture/shortnames/abcde fghi lmno.txt', function (err, expected) {
          t.error(err)
          t.equal(data.toString(), expected.toString())
        })
      }))
  })

  t.test('print a disambiguation', function (t) {
    t.plan(1)

    var expected = '' +
      'There are 2 help pages that matches the given request, please disambiguate:\n' +
      '  * abcde fghi lmno\n' +
      '  * abcde hello\n'

    helper
      .createStream(['abc'])
      .pipe(concat({ encoding: 'string' }, function (data) {
        t.equal(data, expected)
      }))
  })
})

test('support for help files organized in folders', function (t) {
  var helper = helpMe({
    dir: 'fixture/dir'
  })

  t.test('passing an array', function (t) {
    t.plan(2)

    helper
      .createStream(['a', 'b'])
      .pipe(concat(function (data) {
        fs.readFile('fixture/dir/a/b.txt', function (err, expected) {
          t.error(err)
          t.equal(data.toString(), expected.toString())
        })
      }))
  })

  t.test('passing a string', function (t) {
    t.plan(2)

    helper
      .createStream('a b')
      .pipe(concat(function (data) {
        fs.readFile('fixture/dir/a/b.txt', function (err, expected) {
          t.error(err)
          t.equal(data.toString(), expected.toString())
        })
      }))
  })
})
