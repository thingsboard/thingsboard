'use strict'

const figgyPudding = require('figgy-pudding')
const index = require('./lib/entry-index')
const memo = require('./lib/memoization')
const write = require('./lib/content/write')
const Flush = require('minipass-flush')
const { PassThrough } = require('minipass-collect')
const Pipeline = require('minipass-pipeline')

const PutOpts = figgyPudding({
  algorithms: {
    default: ['sha512']
  },
  integrity: {},
  memoize: {},
  metadata: {},
  pickAlgorithm: {},
  size: {},
  tmpPrefix: {},
  single: {},
  sep: {},
  error: {},
  strict: {}
})

module.exports = putData

function putData (cache, key, data, opts) {
  opts = PutOpts(opts)
  return write(cache, data, opts).then((res) => {
    return index
      .insert(cache, key, res.integrity, opts.concat({ size: res.size }))
      .then((entry) => {
        if (opts.memoize) {
          memo.put(cache, entry, data, opts)
        }
        return res.integrity
      })
  })
}

module.exports.stream = putStream

function putStream (cache, key, opts) {
  opts = PutOpts(opts)
  let integrity
  let size

  let memoData
  const pipeline = new Pipeline()
  // first item in the pipeline is the memoizer, because we need
  // that to end first and get the collected data.
  if (opts.memoize) {
    const memoizer = new PassThrough().on('collect', data => {
      memoData = data
    })
    pipeline.push(memoizer)
  }

  // contentStream is a write-only, not a passthrough
  // no data comes out of it.
  const contentStream = write.stream(cache, opts)
    .on('integrity', (int) => {
      integrity = int
    })
    .on('size', (s) => {
      size = s
    })

  pipeline.push(contentStream)

  // last but not least, we write the index and emit hash and size,
  // and memoize if we're doing that
  pipeline.push(new Flush({
    flush () {
      return index
        .insert(cache, key, integrity, opts.concat({ size }))
        .then((entry) => {
          if (opts.memoize && memoData) {
            memo.put(cache, entry, memoData, opts)
          }
          if (integrity) {
            pipeline.emit('integrity', integrity)
          }
          if (size) {
            pipeline.emit('size', size)
          }
        })
    }
  }))

  return pipeline
}
