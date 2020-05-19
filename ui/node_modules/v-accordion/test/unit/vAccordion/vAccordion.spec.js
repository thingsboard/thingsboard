'use strict';

describe('vAccordion', function () {

  var dependencies = [];

  var hasModule = function(module) {
    return dependencies.indexOf(module) >= 0;
  };



  beforeEach(function () {
    dependencies = angular.module('vAccordion').requires;
  });

  
  
  it('should load config module', function () {
    expect(hasModule('vAccordion.config')).toBe(true);
  });


  it('should load directives module', function () {
    expect(hasModule('vAccordion.directives')).toBe(true);
  });

});