'use strict'

require('should')

module.exports = function abstractStoreTest (build) {
  var store

  beforeEach(function (done) {
    build(function (err, _store) {
      store = _store
      done(err)
    })
  })

  afterEach(function (done) {
    store.close(done)
  })

  it('should put and stream in-flight packets', function (done) {
    var packet = {
      topic: 'hello',
      payload: 'world',
      qos: 1,
      messageId: 42
    }

    store.put(packet, function () {
      store
        .createStream()
        .on('data', function (data) {
          data.should.eql(packet)
          done()
        })
    })
  })

  it('should support destroying the stream', function (done) {
    var packet = {
      topic: 'hello',
      payload: 'world',
      qos: 1,
      messageId: 42
    }

    store.put(packet, function () {
      var stream = store.createStream()
      stream.on('close', done)
      stream.destroy()
    })
  })

  it('should add and del in-flight packets', function (done) {
    var packet = {
      topic: 'hello',
      payload: 'world',
      qos: 1,
      messageId: 42
    }

    store.put(packet, function () {
      store.del(packet, function () {
        store
          .createStream()
          .on('data', function () {
            done(new Error('this should never happen'))
          })
          .on('end', done)
      })
    })
  })

  it('should replace a packet when doing put with the same messageId', function (done) {
    var packet1 = {
      cmd: 'publish', // added
      topic: 'hello',
      payload: 'world',
      qos: 2,
      messageId: 42
    }
    var packet2 = {
      cmd: 'pubrel', // added
      qos: 2,
      messageId: 42
    }

    store.put(packet1, function () {
      store.put(packet2, function () {
        store
          .createStream()
          .on('data', function (data) {
            data.should.eql(packet2)
            done()
          })
      })
    })
  })

  it('should return the original packet on del', function (done) {
    var packet = {
      topic: 'hello',
      payload: 'world',
      qos: 1,
      messageId: 42
    }

    store.put(packet, function () {
      store.del({ messageId: 42 }, function (err, deleted) {
        if (err) {
          throw err
        }
        deleted.should.eql(packet)
        done()
      })
    })
  })

  it('should get a packet with the same messageId', function (done) {
    var packet = {
      topic: 'hello',
      payload: 'world',
      qos: 1,
      messageId: 42
    }

    store.put(packet, function () {
      store.get({ messageId: 42 }, function (err, fromDb) {
        if (err) {
          throw err
        }
        fromDb.should.eql(packet)
        done()
      })
    })
  })
}
