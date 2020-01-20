
var mqtt = require('../')
var max = 1000000
var i = 0
var start = Date.now()
var time
var buf = Buffer.allocUnsafe(10)
var net = require('net')
var server = net.createServer(handle)
var dest

buf.fill('test')

function handle (sock) {
  sock.resume()
}

server.listen(0, function () {
  dest = net.connect(server.address())

  dest.on('connect', tickWait)
  dest.on('drain', tickWait)
  dest.on('finish', function () {
    time = Date.now() - start
    console.log('Total time', time)
    console.log('Total packets', max)
    console.log('Packet/s', max / time * 1000)
    server.close()
  })
})

function tickWait () {
  // console.log('tickWait', i)
  var res = true
  // var toSend = new Buffer(5 + buf.length)

  for (; i < max && res; i++) {
    res = dest.write(mqtt.generate({
      cmd: 'publish',
      topic: 'test',
      payload: buf
    }))
    // buf.copy(toSend, 5)
    // res = dest.write(toSend, 'buffer')
    // console.log(res)
  }

  if (i >= max) {
    dest.end()
  }
}
