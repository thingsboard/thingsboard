/* global describe, it */
'use strict'
import eslintConfigAngular from '../'
import {expect} from 'chai'
import isArray from 'is-array'
import isPlainObj from 'is-plain-obj'

describe('eslint-config-angular', () => {
  describe('main', () => {
    it('should be an object', () => {
      expect(isPlainObj(eslintConfigAngular)).to.eql(true)
    })
  })

  describe('globals', () => {
    it('should be an object', () => {
      expect(isPlainObj(eslintConfigAngular.globals)).to.eql(true)
    })
  })

  describe('plugins', () => {
    it('should be an array', () => {
      expect(isArray(eslintConfigAngular.plugins)).to.eql(true)
    })
  })

  describe('rules', () => {
    it('should be an object', () => {
      expect(isPlainObj(eslintConfigAngular.rules)).to.eql(true)
    })
  })
})
