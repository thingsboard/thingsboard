'use strict'

const crypto = require('crypto')
const figgyPudding = require('figgy-pudding')
const MiniPass = require('minipass')

const SPEC_ALGORITHMS = ['sha256', 'sha384', 'sha512']

const BASE64_REGEX = /^[a-z0-9+/]+(?:=?=?)$/i
const SRI_REGEX = /^([^-]+)-([^?]+)([?\S*]*)$/
const STRICT_SRI_REGEX = /^([^-]+)-([A-Za-z0-9+/=]{44,88})(\?[\x21-\x7E]*)*$/
const VCHAR_REGEX = /^[\x21-\x7E]+$/

const SsriOpts = figgyPudding({
  algorithms: { default: ['sha512'] },
  error: { default: false },
  integrity: {},
  options: { default: [] },
  pickAlgorithm: { default: () => getPrioritizedHash },
  sep: { default: ' ' },
  single: { default: false },
  size: {},
  strict: { default: false }
})

const getOptString = options => !options || !options.length ? ''
  : `?${options.join('?')}`

const _onEnd = Symbol('_onEnd')
const _getOptions = Symbol('_getOptions')
class IntegrityStream extends MiniPass {
  constructor (opts) {
    super()
    this.size = 0
    this.opts = opts

    // may be overridden later, but set now for class consistency
    this[_getOptions]()

    // options used for calculating stream.  can't be changed.
    this.algorithms = Array.from(
      new Set(opts.algorithms.concat(this.algorithm ? [this.algorithm] : []))
    )
    this.hashes = this.algorithms.map(crypto.createHash)
  }

  [_getOptions] () {
    const opts = this.opts
    // For verification
    this.sri = opts.integrity ? parse(opts.integrity, opts) : null
    this.expectedSize = opts.size
    this.goodSri = this.sri ? !!Object.keys(this.sri).length : false
    this.algorithm = this.goodSri ? this.sri.pickAlgorithm(opts) : null
    this.digests = this.goodSri ? this.sri[this.algorithm] : null
    this.optString = getOptString(opts.options)
  }

  emit (ev, data) {
    if (ev === 'end') this[_onEnd]()
    return super.emit(ev, data)
  }

  write (data) {
    this.size += data.length
    this.hashes.forEach(h => h.update(data))
    return super.write(data)
  }

  [_onEnd] () {
    if (!this.goodSri) {
      this[_getOptions]()
    }
    const newSri = parse(this.hashes.map((h, i) => {
      return `${this.algorithms[i]}-${h.digest('base64')}${this.optString}`
    }).join(' '), this.opts)
    // Integrity verification mode
    const match = this.goodSri && newSri.match(this.sri, this.opts)
    if (typeof this.expectedSize === 'number' && this.size !== this.expectedSize) {
      const err = new Error(`stream size mismatch when checking ${this.sri}.\n  Wanted: ${this.expectedSize}\n  Found: ${this.size}`)
      err.code = 'EBADSIZE'
      err.found = this.size
      err.expected = this.expectedSize
      err.sri = this.sri
      this.emit('error', err)
    } else if (this.sri && !match) {
      const err = new Error(`${this.sri} integrity checksum failed when using ${this.algorithm}: wanted ${this.digests} but got ${newSri}. (${this.size} bytes)`)
      err.code = 'EINTEGRITY'
      err.found = newSri
      err.expected = this.digests
      err.algorithm = this.algorithm
      err.sri = this.sri
      this.emit('error', err)
    } else {
      this.emit('size', this.size)
      this.emit('integrity', newSri)
      match && this.emit('verified', match)
    }
  }
}

class Hash {
  get isHash () { return true }
  constructor (hash, opts) {
    opts = SsriOpts(opts)
    const strict = !!opts.strict
    this.source = hash.trim()

    // set default values so that we make V8 happy to
    // always see a familiar object template.
    this.digest = ''
    this.algorithm = ''
    this.options = []

    // 3.1. Integrity metadata (called "Hash" by ssri)
    // https://w3c.github.io/webappsec-subresource-integrity/#integrity-metadata-description
    const match = this.source.match(
      strict
        ? STRICT_SRI_REGEX
        : SRI_REGEX
    )
    if (!match) { return }
    if (strict && !SPEC_ALGORITHMS.some(a => a === match[1])) { return }
    this.algorithm = match[1]
    this.digest = match[2]

    const rawOpts = match[3]
    if (rawOpts) {
      this.options = rawOpts.slice(1).split('?')
    }
  }

  hexDigest () {
    return this.digest && Buffer.from(this.digest, 'base64').toString('hex')
  }

  toJSON () {
    return this.toString()
  }

  toString (opts) {
    opts = SsriOpts(opts)
    if (opts.strict) {
      // Strict mode enforces the standard as close to the foot of the
      // letter as it can.
      if (!(
        // The spec has very restricted productions for algorithms.
        // https://www.w3.org/TR/CSP2/#source-list-syntax
        SPEC_ALGORITHMS.some(x => x === this.algorithm) &&
        // Usually, if someone insists on using a "different" base64, we
        // leave it as-is, since there's multiple standards, and the
        // specified is not a URL-safe variant.
        // https://www.w3.org/TR/CSP2/#base64_value
        this.digest.match(BASE64_REGEX) &&
        // Option syntax is strictly visual chars.
        // https://w3c.github.io/webappsec-subresource-integrity/#grammardef-option-expression
        // https://tools.ietf.org/html/rfc5234#appendix-B.1
        this.options.every(opt => opt.match(VCHAR_REGEX))
      )) {
        return ''
      }
    }
    const options = this.options && this.options.length
      ? `?${this.options.join('?')}`
      : ''
    return `${this.algorithm}-${this.digest}${options}`
  }
}

class Integrity {
  get isIntegrity () { return true }
  toJSON () {
    return this.toString()
  }

  toString (opts) {
    opts = SsriOpts(opts)
    let sep = opts.sep || ' '
    if (opts.strict) {
      // Entries must be separated by whitespace, according to spec.
      sep = sep.replace(/\S+/g, ' ')
    }
    return Object.keys(this).map(k => {
      return this[k].map(hash => {
        return Hash.prototype.toString.call(hash, opts)
      }).filter(x => x.length).join(sep)
    }).filter(x => x.length).join(sep)
  }

  concat (integrity, opts) {
    opts = SsriOpts(opts)
    const other = typeof integrity === 'string'
      ? integrity
      : stringify(integrity, opts)
    return parse(`${this.toString(opts)} ${other}`, opts)
  }

  hexDigest () {
    return parse(this, { single: true }).hexDigest()
  }

  // add additional hashes to an integrity value, but prevent
  // *changing* an existing integrity hash.
  merge (integrity, opts) {
    opts = SsriOpts(opts)
    const other = parse(integrity, opts)
    for (const algo in other) {
      if (this[algo]) {
        if (!this[algo].find(hash =>
          other[algo].find(otherhash =>
            hash.digest === otherhash.digest))) {
          throw new Error('hashes do not match, cannot update integrity')
        }
      } else {
        this[algo] = other[algo]
      }
    }
  }

  match (integrity, opts) {
    opts = SsriOpts(opts)
    const other = parse(integrity, opts)
    const algo = other.pickAlgorithm(opts)
    return (
      this[algo] &&
      other[algo] &&
      this[algo].find(hash =>
        other[algo].find(otherhash =>
          hash.digest === otherhash.digest
        )
      )
    ) || false
  }

  pickAlgorithm (opts) {
    opts = SsriOpts(opts)
    const pickAlgorithm = opts.pickAlgorithm
    const keys = Object.keys(this)
    if (!keys.length) {
      throw new Error(`No algorithms available for ${
        JSON.stringify(this.toString())
      }`)
    }
    return keys.reduce((acc, algo) => {
      return pickAlgorithm(acc, algo) || acc
    })
  }
}

module.exports.parse = parse
function parse (sri, opts) {
  opts = SsriOpts(opts)
  if (typeof sri === 'string') {
    return _parse(sri, opts)
  } else if (sri.algorithm && sri.digest) {
    const fullSri = new Integrity()
    fullSri[sri.algorithm] = [sri]
    return _parse(stringify(fullSri, opts), opts)
  } else {
    return _parse(stringify(sri, opts), opts)
  }
}

function _parse (integrity, opts) {
  // 3.4.3. Parse metadata
  // https://w3c.github.io/webappsec-subresource-integrity/#parse-metadata
  if (opts.single) {
    return new Hash(integrity, opts)
  }
  return integrity.trim().split(/\s+/).reduce((acc, string) => {
    const hash = new Hash(string, opts)
    if (hash.algorithm && hash.digest) {
      const algo = hash.algorithm
      if (!acc[algo]) { acc[algo] = [] }
      acc[algo].push(hash)
    }
    return acc
  }, new Integrity())
}

module.exports.stringify = stringify
function stringify (obj, opts) {
  opts = SsriOpts(opts)
  if (obj.algorithm && obj.digest) {
    return Hash.prototype.toString.call(obj, opts)
  } else if (typeof obj === 'string') {
    return stringify(parse(obj, opts), opts)
  } else {
    return Integrity.prototype.toString.call(obj, opts)
  }
}

module.exports.fromHex = fromHex
function fromHex (hexDigest, algorithm, opts) {
  opts = SsriOpts(opts)
  const optString = getOptString(opts.options)
  return parse(
    `${algorithm}-${
      Buffer.from(hexDigest, 'hex').toString('base64')
    }${optString}`, opts
  )
}

module.exports.fromData = fromData
function fromData (data, opts) {
  opts = SsriOpts(opts)
  const algorithms = opts.algorithms
  const optString = getOptString(opts.options)
  return algorithms.reduce((acc, algo) => {
    const digest = crypto.createHash(algo).update(data).digest('base64')
    const hash = new Hash(
      `${algo}-${digest}${optString}`,
      opts
    )
    /* istanbul ignore else - it would be VERY strange if the string we
     * just calculated with an algo did not have an algo or digest.
     */
    if (hash.algorithm && hash.digest) {
      const algo = hash.algorithm
      if (!acc[algo]) { acc[algo] = [] }
      acc[algo].push(hash)
    }
    return acc
  }, new Integrity())
}

module.exports.fromStream = fromStream
function fromStream (stream, opts) {
  opts = SsriOpts(opts)
  const istream = integrityStream(opts)
  return new Promise((resolve, reject) => {
    stream.pipe(istream)
    stream.on('error', reject)
    istream.on('error', reject)
    let sri
    istream.on('integrity', s => { sri = s })
    istream.on('end', () => resolve(sri))
    istream.on('data', () => {})
  })
}

module.exports.checkData = checkData
function checkData (data, sri, opts) {
  opts = SsriOpts(opts)
  sri = parse(sri, opts)
  if (!Object.keys(sri).length) {
    if (opts.error) {
      throw Object.assign(
        new Error('No valid integrity hashes to check against'), {
          code: 'EINTEGRITY'
        }
      )
    } else {
      return false
    }
  }
  const algorithm = sri.pickAlgorithm(opts)
  const digest = crypto.createHash(algorithm).update(data).digest('base64')
  const newSri = parse({ algorithm, digest })
  const match = newSri.match(sri, opts)
  if (match || !opts.error) {
    return match
  } else if (typeof opts.size === 'number' && (data.length !== opts.size)) {
    const err = new Error(`data size mismatch when checking ${sri}.\n  Wanted: ${opts.size}\n  Found: ${data.length}`)
    err.code = 'EBADSIZE'
    err.found = data.length
    err.expected = opts.size
    err.sri = sri
    throw err
  } else {
    const err = new Error(`Integrity checksum failed when using ${algorithm}: Wanted ${sri}, but got ${newSri}. (${data.length} bytes)`)
    err.code = 'EINTEGRITY'
    err.found = newSri
    err.expected = sri
    err.algorithm = algorithm
    err.sri = sri
    throw err
  }
}

module.exports.checkStream = checkStream
function checkStream (stream, sri, opts) {
  opts = SsriOpts(opts)
  const checker = integrityStream(opts.concat({
    integrity: sri
  }))
  return new Promise((resolve, reject) => {
    stream.pipe(checker)
    stream.on('error', reject)
    checker.on('error', reject)
    let sri
    checker.on('verified', s => { sri = s })
    checker.on('end', () => resolve(sri))
    checker.on('data', () => {})
  })
}

module.exports.integrityStream = integrityStream
function integrityStream (opts) {
  return new IntegrityStream(SsriOpts(opts))
}

module.exports.create = createIntegrity
function createIntegrity (opts) {
  opts = SsriOpts(opts)
  const algorithms = opts.algorithms
  const optString = getOptString(opts.options)

  const hashes = algorithms.map(crypto.createHash)

  return {
    update: function (chunk, enc) {
      hashes.forEach(h => h.update(chunk, enc))
      return this
    },
    digest: function (enc) {
      const integrity = algorithms.reduce((acc, algo) => {
        const digest = hashes.shift().digest('base64')
        const hash = new Hash(
          `${algo}-${digest}${optString}`,
          opts
        )
        /* istanbul ignore else - it would be VERY strange if the hash we
         * just calculated with an algo did not have an algo or digest.
         */
        if (hash.algorithm && hash.digest) {
          const algo = hash.algorithm
          if (!acc[algo]) { acc[algo] = [] }
          acc[algo].push(hash)
        }
        return acc
      }, new Integrity())

      return integrity
    }
  }
}

const NODE_HASHES = new Set(crypto.getHashes())

// This is a Best Effort™ at a reasonable priority for hash algos
const DEFAULT_PRIORITY = [
  'md5', 'whirlpool', 'sha1', 'sha224', 'sha256', 'sha384', 'sha512',
  // TODO - it's unclear _which_ of these Node will actually use as its name
  //        for the algorithm, so we guesswork it based on the OpenSSL names.
  'sha3',
  'sha3-256', 'sha3-384', 'sha3-512',
  'sha3_256', 'sha3_384', 'sha3_512'
].filter(algo => NODE_HASHES.has(algo))

function getPrioritizedHash (algo1, algo2) {
  return DEFAULT_PRIORITY.indexOf(algo1.toLowerCase()) >= DEFAULT_PRIORITY.indexOf(algo2.toLowerCase())
    ? algo1
    : algo2
}
