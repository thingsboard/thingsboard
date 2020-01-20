'use strict'

var fs = require('fs')
var path = require('path')
var through = require('through2')
var globStream = require('glob-stream')
var concat = require('callback-stream')
var xtend = require('xtend')

var defaults = {
  dir: path.join(path.dirname(require.main.filename), 'doc'),
  ext: '.txt',
  help: 'help'
}

function helpMe (opts) {
  opts = xtend(defaults, opts)

  if (!opts.dir) {
    throw new Error('missing directory')
  }

  return {
    createStream: createStream,
    toStdout: toStdout
  }

  function createStream (args) {
    if (typeof args === 'string') {
      args = args.split(' ')
    } else if (!args || args.length === 0) {
      args = [opts.help]
    }

    var out = through()
    var gs = globStream([opts.dir + '/**/*' + opts.ext])
    var re = new RegExp(args.map(function (arg) {
      return arg + '[a-zA-Z0-9]*'
    }).join('[ /]+'))

    gs.pipe(concat({ objectMode: true }, function (err, files) {
      if (err) return out.emit('error', err)

      files = files.map(function (file) {
        file.relative = file.path.replace(file.base, '').replace(/^\//, '')
        return file
      }).filter(function (file) {
        return file.relative.match(re)
      })

      if (files.length === 0) {
        return out.emit('error', new Error('no such help file'))
      } else if (files.length > 1) {
        out.write('There are ' + files.length + ' help pages ')
        out.write('that matches the given request, please disambiguate:\n')
        files.forEach(function (file) {
          out.write('  * ')
          out.write(file.relative.replace(opts.ext, ''))
          out.write('\n')
        })
        out.end()
        return
      }

      fs.createReadStream(files[0].path)
        .on('error', function (err) {
          out.emit('error', err)
        })
        .pipe(out)
    }))

    return out
  }

  function toStdout (args) {
    createStream(args)
      .on('error', function () {
        console.log('no such help file\n')
        toStdout()
      })
      .pipe(process.stdout)
  }
}

module.exports = helpMe
