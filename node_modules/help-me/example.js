'use strict'

var commist = require('commist')()
var help = require('./')()

commist.register('help', help.toStdout)

commist.parse(process.argv.splice(2))
