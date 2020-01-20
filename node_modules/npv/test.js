var assert = require('assert')
var npv = require('.')

describe('npv', function() {
    it('should return the version of package.json if no key is passed', function() {
        var result = npv()
        assert(result.hasOwnProperty('version'), 'result should have a property version')
    })

    it('should return properties for given keys', function() {
        var keys = ['name', 'version', 'description']
        var result = npv(keys)
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i]
            assert(result.hasOwnProperty(key), 'result should have a property '+key)
        }
    })

    it('should return null values for not existing property keys', function () {
      var keys = ['test', 'example', 'nahody']
      var result = npv(keys)
      for (var i = 0; i < keys.length; i++) {
          var key = keys[i]
          assert(result.hasOwnProperty(key), 'result should have a property '+key)
          assert(result[key] === null, 'result value of property '+key+' should be null')
      }
    })
})
