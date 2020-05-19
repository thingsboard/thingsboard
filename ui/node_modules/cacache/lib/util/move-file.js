'use strict'

const fs = require('graceful-fs')
const util = require('util')
const chmod = util.promisify(fs.chmod)
const unlink = util.promisify(fs.unlink)
const stat = util.promisify(fs.stat)
const move = require('move-concurrently')
const pinflight = require('promise-inflight')

module.exports = moveFile

function moveFile (src, dest) {
  // This isn't quite an fs.rename -- the assumption is that
  // if `dest` already exists, and we get certain errors while
  // trying to move it, we should just not bother.
  //
  // In the case of cache corruption, users will receive an
  // EINTEGRITY error elsewhere, and can remove the offending
  // content their own way.
  //
  // Note that, as the name suggests, this strictly only supports file moves.
  return new Promise((resolve, reject) => {
    fs.link(src, dest, (err) => {
      if (err) {
        if (err.code === 'EEXIST' || err.code === 'EBUSY') {
          // file already exists, so whatever
        } else if (err.code === 'EPERM' && process.platform === 'win32') {
          // file handle stayed open even past graceful-fs limits
        } else {
          return reject(err)
        }
      }
      return resolve()
    })
  })
    .then(() => {
      // content should never change for any reason, so make it read-only
      return Promise.all([
        unlink(src),
        process.platform !== 'win32' && chmod(dest, '0444')
      ])
    })
    .catch(() => {
      return pinflight('cacache-move-file:' + dest, () => {
        return stat(dest).catch((err) => {
          if (err.code !== 'ENOENT') {
            // Something else is wrong here. Bail bail bail
            throw err
          }
          // file doesn't already exist! let's try a rename -> copy fallback
          return move(src, dest, { Promise, fs })
        })
      })
    })
}
