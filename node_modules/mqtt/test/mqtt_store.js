'use strict'

var mqtt = require('../lib/connect')

describe('store in lib/connect/index.js (webpack entry point)', function () {
  it('should create store', function (done) {
    done(null, new mqtt.Store())
  })
})
