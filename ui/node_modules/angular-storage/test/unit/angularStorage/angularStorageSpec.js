'use strict';

describe('angularStorage', function() {

  var module;
  var dependencies;
  dependencies = [];

  var hasModule = function(module) {
    return dependencies.indexOf(module) >= 0;
  };

  beforeEach(function() {

    // Get module
    module = angular.module('angular-storage');
    dependencies = module.requires;
  });

  it('should load store module', function() {
    expect(hasModule('angular-storage.store')).to.be.ok;
  });

});
