
var server = require('./server.js')

module.exports = require('./stream.js')
module.exports.Server = server.Server
module.exports.createServer = server.createServer
