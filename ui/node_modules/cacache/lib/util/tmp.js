'use strict'

const util = require('util')

const figgyPudding = require('figgy-pudding')
const fixOwner = require('./fix-owner')
const path = require('path')
const rimraf = util.promisify(require('rimraf'))
const uniqueFilename = require('unique-filename')
const { disposer } = require('./disposer')

const TmpOpts = figgyPudding({
  tmpPrefix: {}
})

module.exports.mkdir = mktmpdir

function mktmpdir (cache, opts) {
  opts = TmpOpts(opts)
  const tmpTarget = uniqueFilename(path.join(cache, 'tmp'), opts.tmpPrefix)
  return fixOwner.mkdirfix(cache, tmpTarget).then(() => {
    return tmpTarget
  })
}

module.exports.withTmp = withTmp

function withTmp (cache, opts, cb) {
  if (!cb) {
    cb = opts
    opts = null
  }
  opts = TmpOpts(opts)

  return disposer(mktmpdir(cache, opts), rimraf, cb)
}

module.exports.fix = fixtmpdir

function fixtmpdir (cache) {
  return fixOwner(cache, path.join(cache, 'tmp'))
}
