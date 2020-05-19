/*global waits, runs, iit, browserTrigger, beforeEach, afterEach, describe, it, inject, expect, module, angular, $*/

describe('angular-carousel.shifty', function () {
  'use strict';

  describe("compatibility with requirejs", function(){
    var loadShifty = function() {
      module('angular-carousel.shifty');
    };
    it("should not throw an exception when load the shifty within requirejs environment", function(){
      expect(loadShifty).not.toThrow();
    });

    it("should not throw an exception when inject `Tweenable` within requirejs environment", function(){
      loadShifty();
      expect(function() {inject(function(Tweenable){});}).not.toThrow();
    });
  });

});
