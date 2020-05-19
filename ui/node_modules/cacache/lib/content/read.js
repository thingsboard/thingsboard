'use strict'

const util = require('util')

const figgyPudding = require('figgy-pudding')
const fs = require('graceful-fs')
const fsm = require('fs-minipass')
const ssri = require('ssri')
const contentPath = require('./path')
const Pipeline = require('minipass-pipeline')

const lstat = util.promisify(fs.lstat)
const readFile = util.promisify(fs.readFile)

const ReadOpts = figgyPudding({
  size: {}
})

module.exports = read

const MAX_SINGLE_READ_SIZE = 64 * 1024 * 1024
function read (cache, integrity, opts) {
  opts = ReadOpts(opts)
  return withContentSri(cache, integrity, (cpath, sri) => {
    // get size
    return lstat(cpath).then(stat => ({ stat, cpath, sri }))
  }).then(({ stat, cpath, sri }) => {
    if (typeof opts.size === 'number' && stat.size !== opts.size) {
      throw sizeError(opts.size, stat.size)
    }
    if (stat.size > MAX_SINGLE_READ_SIZE) {
      return readPipeline(cpath, stat.size, sri, new Pipeline()).concat()
    }

    return readFile(cpath, null).then((data) => {
      if (!ssri.checkData(data, sri)) {
        throw integrityError(sri, cpath)
      }
      return data
    })
  })
}

const readPipeline = (cpath, size, sri, stream) => {
  stream.push(
    new fsm.ReadStream(cpath, {
      size,
      readSize: MAX_SINGLE_READ_SIZE
    }),
    ssri.integrityStream({
      integrity: sri,
      size
    })
  )
  return stream
}

module.exports.sync = readSync

function readSync (cache, integrity, opts) {
  opts = ReadOpts(opts)
  return withContentSriSync(cache, integrity, (cpath, sri) => {
    const data = fs.readFileSync(cpath)
    if (typeof opts.size === 'number' && opts.size !== data.length) {
      throw sizeError(opts.size, data.length)
    }

    if (ssri.checkData(data, sri)) {
      return data
    }

    throw integrityError(sri, cpath)
  })
}

module.exports.stream = readStream
module.exports.readStream = readStream

function readStream (cache, integrity, opts) {
  opts = ReadOpts(opts)

  const stream = new Pipeline()
  withContentSri(cache, integrity, (cpath, sri) => {
    // just lstat to ensure it exists
    return lstat(cpath).then((stat) => ({ stat, cpath, sri }))
  }).then(({ stat, cpath, sri }) => {
    if (typeof opts.size === 'number' && opts.size !== stat.size) {
      return stream.emit('error', sizeError(opts.size, stat.size))
    }
    readPipeline(cpath, stat.size, sri, stream)
  }, er => stream.emit('error', er))

  return stream
}

let copyFile
if (fs.copyFile) {
  module.exports.copy = copy
  module.exports.copy.sync = copySync
  copyFile = util.promisify(fs.copyFile)
}

function copy (cache, integrity, dest, opts) {
  opts = ReadOpts(opts)
  return withContentSri(cache, integrity, (cpath, sri) => {
    return copyFile(cpath, dest)
  })
}

function copySync (cache, integrity, dest, opts) {
  opts = ReadOpts(opts)
  return withContentSriSync(cache, integrity, (cpath, sri) => {
    return fs.copyFileSync(cpath, dest)
  })
}

module.exports.hasContent = hasContent

function hasContent (cache, integrity) {
  if (!integrity) {
    return Promise.resolve(false)
  }
  return withContentSri(cache, integrity, (cpath, sri) => {
    return lstat(cpath).then((stat) => ({ size: stat.size, sri, stat }))
  }).catch((err) => {
    if (err.code === 'ENOENT') {
      return false
    }
    if (err.code === 'EPERM') {
      if (process.platform !== 'win32') {
        throw err
      } else {
        return false
      }
    }
  })
}

module.exports.hasContent.sync = hasContentSync

function hasContentSync (cache, integrity) {
  if (!integrity) {
    return false
  }
  return withContentSriSync(cache, integrity, (cpath, sri) => {
    try {
      const stat = fs.lstatSync(cpath)
      return { size: stat.size, sri, stat }
    } catch (err) {
      if (err.code === 'ENOENT') {
        return false
      }
      if (err.code === 'EPERM') {
        if (process.platform !== 'win32') {
          throw err
        } else {
          return false
        }
      }
    }
  })
}

function withContentSri (cache, integrity, fn) {
  const tryFn = () => {
    const sri = ssri.parse(integrity)
    // If `integrity` has multiple entries, pick the first digest
    // with available local data.
    const algo = sri.pickAlgorithm()
    const digests = sri[algo]

    if (digests.length <= 1) {
      const cpath = contentPath(cache, digests[0])
      return fn(cpath, digests[0])
    } else {
      // Can't use race here because a generic error can happen before a ENOENT error, and can happen before a valid result
      return Promise
        .all(sri[sri.pickAlgorithm()].map((meta) => {
          return withContentSri(cache, meta, fn)
            .catch((err) => {
              if (err.code === 'ENOENT') {
                return Object.assign(
                  new Error('No matching content found for ' + sri.toString()),
                  { code: 'ENOENT' }
                )
              }
              return err
            })
        }))
        .then((results) => {
          // Return the first non error if it is found
          const result = results.find((r) => !(r instanceof Error))
          if (result) {
            return result
          }

          // Throw the No matching content found error
          const enoentError = results.find((r) => r.code === 'ENOENT')
          if (enoentError) {
            throw enoentError
          }

          // Throw generic error
          const genericError = results.find((r) => r instanceof Error)
          if (genericError) {
            throw genericError
          }
        })
    }
  }

  return new Promise((resolve, reject) => {
    try {
      tryFn()
        .then(resolve)
        .catch(reject)
    } catch (err) {
      reject(err)
    }
  })
}

function withContentSriSync (cache, integrity, fn) {
  const sri = ssri.parse(integrity)
  // If `integrity` has multiple entries, pick the first digest
  // with available local data.
  const algo = sri.pickAlgorithm()
  const digests = sri[algo]
  if (digests.length <= 1) {
    const cpath = contentPath(cache, digests[0])
    return fn(cpath, digests[0])
  } else {
    let lastErr = null
    for (const meta of sri[sri.pickAlgorithm()]) {
      try {
        return withContentSriSync(cache, meta, fn)
      } catch (err) {
        lastErr = err
      }
    }
    if (lastErr) {
      throw lastErr
    }
  }
}

function sizeError (expected, found) {
  const err = new Error(`Bad data size: expected inserted data to be ${expected} bytes, but got ${found} instead`)
  err.expected = expected
  err.found = found
  err.code = 'EBADSIZE'
  return err
}

function integrityError (sri, path) {
  const err = new Error(`Integrity verification failed for ${sri} (${path})`)
  err.code = 'EINTEGRITY'
  err.sri = sri
  err.path = path
  return err
}
