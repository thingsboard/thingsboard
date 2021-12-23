'use strict'

const commist = require('commist')()
const help = require('./')()

commist.register('help', help.toStdout)

commist.parse(process.argv.splice(2))
