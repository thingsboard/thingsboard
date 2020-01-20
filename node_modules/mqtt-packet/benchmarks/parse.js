
var mqtt = require('../')
var parser = mqtt.parser()
var max = 10000000
var i
var start = Date.now() / 1000
var time

for (i = 0; i < max; i++) {
  parser.parse(Buffer.from([
    48, 10, // Header (publish)
    0, 4, // Topic length
    116, 101, 115, 116, // Topic (test)
    116, 101, 115, 116 // Payload (test)
  ]))
}

time = Date.now() / 1000 - start
console.log('Total packets', max)
console.log('Total time', Math.round(time * 100) / 100)
console.log('Packet/s', max / time)
