/*
The MIT License (MIT)

Copyright (c) 2014 Matteo Collina

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

'use strict'

var leven = require('leven')

function commist () {
  var commands = []

  function lookup (array) {
    if (typeof array === 'string') { array = array.split(' ') }

    return commands.map(function (cmd) {
      return cmd.match(array)
    }).filter(function (match) {
      return match.partsNotMatched === 0
    }).sort(function (a, b) {
      if (a.inputNotMatched > b.inputNotMatched) { return 1 }

      if (a.inputNotMatched === b.inputNotMatched && a.totalDistance > b.totalDistance) { return 1 }

      return -1
    }).map(function (match) {
      return match.cmd
    })
  }

  function parse (args) {
    var matching = lookup(args)

    if (matching.length > 0) {
      matching[0].call(args)

      // return null if there is nothing left to do
      return null
    }

    return args
  }

  function register (inputCommand, func) {
    var commandOptions = {
      command: inputCommand,
      strict: false,
      func: func
    }

    if (typeof inputCommand === 'object') {
      commandOptions = Object.assign(commandOptions, inputCommand)
    }

    var matching = lookup(commandOptions.command)

    matching.forEach(function (match) {
      if (match.string === commandOptions.command) { throw new Error('command already registered: ' + commandOptions.command) }
    })

    commands.push(new Command(commandOptions))

    return this
  }

  return {
    register: register,
    parse: parse,
    lookup: lookup
  }
}

function Command (options) {
  this.string = options.command
  this.strict = options.strict
  this.parts = this.string.split(' ')
  this.length = this.parts.length
  this.func = options.func

  this.parts.forEach(function (part) {
    if (part.length < 3) { throw new Error('command words must be at least 3 chars: ' + options.command) }
  })
}

Command.prototype.call = function call (argv) {
  this.func(argv.slice(this.length))
}

Command.prototype.match = function match (string) {
  return new CommandMatch(this, string)
}

function CommandMatch (cmd, array) {
  this.cmd = cmd
  this.distances = cmd.parts.map(function (elem, i) {
    if (array[i] !== undefined) {
      if (cmd.strict) {
        return elem === array[i] ? 0 : undefined
      } else {
        return leven(elem, array[i])
      }
    } else { return undefined }
  }).filter(function (distance, i) {
    return distance !== undefined && distance < cmd.parts[i].length - 2
  })

  this.partsNotMatched = cmd.length - this.distances.length
  this.inputNotMatched = array.length - this.distances.length
  this.totalDistance = this.distances.reduce(function (acc, i) { return acc + i }, 0)
}

module.exports = commist
