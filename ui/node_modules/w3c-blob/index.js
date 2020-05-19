module.exports = Blob

var Buffer = require('buffer').Buffer
  , str = {}.toString.call.bind({}.toString)

function Blob(parts, properties) {
  properties = properties || {}
  this.type = properties.type || ''
  var size = 0
  for(var i = 0, len = parts.length; i < len; ++i) {
    size += typeof parts[i] === 'string' ? Buffer.byteLength(parts[i]) :
            str(parts[i]).indexOf('ArrayBuffer') > -1 ? parts[i].byteLength :
            parts[i].buffer ? parts[i].buffer.byteLength :
            parts[i].length
  }
  this.size = size
}

var cons = Blob
  , proto = cons.prototype

proto.slice = function(start, end) {
  var b = new Blob([], {type: this.type})
  b.size = (end || this.size) - (start || 0)
  return b
}
