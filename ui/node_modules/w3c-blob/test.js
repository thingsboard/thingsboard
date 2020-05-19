var test = require('tape')

if(process.browser) {
  browser()
} else {
  node()
}

function node() {
  var Blob = (require)('./index')
    , Buffer = (require)('buffer').Buffer

  test('works as expected with strings', function(assert) {
    var str = '\u022au\u2323'

    assert.equal(new Blob([str]).size, 6)
    assert.end()
  })

  test('works as expected with array buffers', function(assert) {
    var size = Math.random() * 1024 | 1
      , ab = new ArrayBuffer(size)

    assert.equal(new Blob([ab]).size, size)
    assert.end()
  })

  test('works as expected with typed arrays', function(assert) {
    var size = Math.random() * 1024 | 1
      , ab = new Uint8Array(size)

    assert.equal(new Blob([ab]).size, size)
    assert.end()
  })

  test('works as expected with buffers', function(assert) {
    var size = Math.random() * 1024 | 1
      , ab = new Buffer(size)

    assert.equal(new Blob([ab]).size, size)
    assert.end()
  })
}

function browser() {
  var Blob = require('./browser')

  test('works as expected', function(assert) {
    var b = new Blob(['asdf'], {type: 'text/plain'})
    try {
      var URL = (global.URL || global.webkitURL || global.MozURL || global.msURL || global.MSURL)
      assert.equal(typeof URL.createObjectURL(b), 'string')
    } catch(e) {
      assert.fail('did not work')
    }
    assert.end()
  })
}
