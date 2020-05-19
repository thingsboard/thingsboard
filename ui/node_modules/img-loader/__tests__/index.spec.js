/* eslint-env mocha */
'use strict'
var assert = require('assert')
var fs = require('fs')
var path = require('path')
var gifsicle = require('imagemin-gifsicle')
var svgo = require('imagemin-svgo')
var run = require('./run-webpack')

var fixtureGif = fs.readFileSync(path.resolve(__dirname, './fixture.gif'))
var fixtureSvg = fs.readFileSync(path.resolve(__dirname, './fixture.svg'))

describe('img-loader', () => {
  it('passes the img though unchanged by default', function () {
    return run('./fixture.gif').then(function (image) {
      assert(image.equals(fixtureGif), 'gif should be unchanged')
    })
  })

  it('can apply optimizations for gif', function () {
    return run('./fixture.gif', {
      plugins: [ gifsicle({}) ]
    }).then(function (image) {
      assert(!image.equals(fixtureGif), 'gif should be changed')
      assert(image.length < fixtureGif.length, 'optimized gif should be smaller')
    })
  })

  it('can apply optimizations for svg', function () {
    return run('./fixture.svg', {
      plugins: [ svgo({}) ]
    }).then(function (image) {
      assert(!image.equals(fixtureSvg), 'svg should be changed')
      assert(image.length < fixtureSvg.length, 'optimized svg should be smaller')
      assert.strictEqual(image.toString('utf8'), '<svg/>')
    })
  })

  it('can use a function for plugins', function () {
    var context
    return run('./fixture.svg', {
      plugins: function (ctx) {
        context = ctx
        return [ svgo({}) ]
      }
    }).then(function (image) {
      assert.strictEqual(path.basename(context.resourcePath), 'fixture.svg')
      assert(image.length < fixtureSvg.length, 'optimized svg should be smaller')
    })
  })
})
