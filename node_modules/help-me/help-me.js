'use strict'

const fs = require('fs')
const { PassThrough, pipeline } = require('readable-stream')
const glob = require('glob')

const defaults = {
  ext: '.txt',
  help: 'help'
}

function isDirectory (path) {
  try {
    const stat = fs.lstatSync(path)
    return stat.isDirectory()
  } catch (err) {
    return false
  }
}

function helpMe (opts) {
  opts = Object.assign({}, defaults, opts)

  if (!opts.dir) {
    throw new Error('missing dir')
  }

  if (!isDirectory(opts.dir)) {
    throw new Error(`${opts.dir} is not a directory`)
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

    const out = new PassThrough()
    const re = new RegExp(args.map(function (arg) {
      return arg + '[a-zA-Z0-9]*'
    }).join('[ /]+'))

    glob(opts.dir + '/**/*' + opts.ext, function (err, files) {
      if (err) return out.emit('error', err)

      files = files.map(function (path) {
        const relative = path.replace(opts.dir, '').replace(/^\//, '')
        return { path, relative }
      }).filter(function (file) {
        return file.relative.match(re)
      })

      if (files.length === 0) {
        return out.emit('error', new Error('no such help file'))
      } else if (files.length > 1) {
        const exactMatch = files.find((file) => file.relative === `${args[0]}${opts.ext}`)
        if (!exactMatch) {
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
        files = [exactMatch]
      }

      pipeline(fs.createReadStream(files[0].path), out, () => {})
    })

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
