/*global waits, runs, iit, browserTrigger, beforeEach, afterEach, describe, it, inject, expect, module, angular, $*/

describe('carousel', function () {
  'use strict';

  var scope, $compile, $sandbox;


  //$('body').append("<link href='/base/dist/angular-carousel.min.css' rel='stylesheet' type='text/css'>");
  /*$('body').append("<style>ul,li {padding:0;margin:0;width:200px !important} " +
      ".rn-carousel-animate { -webkit-transition: -webkit-transform 0.001s ease-out; " +
      "-moz-transition: -moz-transform 0.001s ease-out; transition: transform 0.001s ease-out;} "+
      ".rn-carousel-noanimate {-webkit-transition: none;-moz-transition: none;-ms-transition: none;" +
      "-o-transition: none;transition: none;}</style>");*/

  //console.log(document.location);
  beforeEach(
    module('angular-carousel')
  );

  beforeEach(inject(function ($rootScope, _$compile_) {
      scope = $rootScope;
      $compile = _$compile_;
      // $('body').css({
      //   padding: 0,
      //   margin:0
      // });
     // $sandbox = $('<div id="sandbox"></div>').appendTo($('body'));
  }));

  afterEach(function() {
    //$sandbox.remove();
    scope.$destroy();
  });

 function compileTpl(overrideOptions) {
    var options = {
      useIndex: false,
      useIndicator: false,
      useControl: false,
      useBuffer: false,
      nbItems: 25,
      useWatch: false
    };
    if (overrideOptions) angular.extend(options, overrideOptions);
    var sampleData = {
      scope: {
        items: [],
        localIndex: 5
      }
    };
    for (var i=0; i<options.nbItems; i++) {
      sampleData.scope.items.push({
        text: 'slide #' + i,
        id: i
      });
    }
    var tpl = '<ul rn-carousel ';
    if (options.useIndicator) tpl += ' rn-carousel-indicator ';
    if (options.useControl) tpl += ' rn-carousel-control ';
    if (options.useBuffer) tpl += ' rn-carousel-buffered ';
    if (options.useWatch) tpl += ' rn-carousel-watch ';
    if (options.useIndex) tpl += ' rn-carousel-index="' + options.useIndex + '" ';
    tpl += '><li class="test" style="width:200px" ng-repeat="item in items" id="slide-{{ item.id }}">{{ item.text }}</li></ul>';
    angular.extend(scope, sampleData.scope);
 //   var $element = $(tpl).appendTo($sandbox);
    var $element = $compile(tpl)(scope);
    scope.$digest();
    return $element;
  }

  function getElmTransform(elm) {
    var curMatrix = elm.css('-webkit-transform');
    if (!curMatrix) curMatrix = elm.css('transform');
    return curMatrix;
  }
  function validCSStransform(elm) {
    var expectedPosition = (elm.offsetWidth * elm.scope().carouselCollection.index * -1),
        expectedMatrix = 'matrix(1, 0, 0, 1, ' + expectedPosition + ', 0)',
        curMatrix = getElmTransform(elm);
    expect(curMatrix).toBe(expectedMatrix);
  }
/*
  it('should load test', function() {
    expect(1).toBe(1);
  });

  describe('directive', function () {
    it('should add a wrapper div around the ul/li', function () {
        var elm = compileTpl();
        expect(elm.parent().hasClass('rn-carousel-container')).toBe(true);
    });
    it('should add a class to the ul', function () {
        var elm = compileTpl();
        expect(elm.hasClass('rn-carousel-slides')).toBe(true);
    });
    it('should have enough slides', function () {
        var elm = compileTpl();
        expect(elm.find('li').length).toBe(scope.items.length);
    });
    it('generated container outerWidth should match the ul outerWidth', function () {
        var elm = compileTpl();
        expect(elm.parent()[0].offsetWidth).toBe(elm[0].offsetWidth);
    });
  });

  describe('directive with a data-bound index defined', function () {
    it('the index attribute should be used to position the first visible slide', function () {
        var elm = compileTpl({useIndex: 'localIndex'});
        waitAndCheck(function() {
          validCSStransform(elm);
        }, 200);
    });
    it('index change should update the carousel position', function () {
        var elm = compileTpl({useIndex: 'localIndex'});
        scope.localIndex = 5;
        scope.$digest();
        waitAndCheck(function() {
          validCSStransform(elm);
        }, 200);
    });
    it('carousel index should be bound to local index', function () {
        var elm = compileTpl({useIndex: 'localIndex'});
        scope.localIndex = 5;
        scope.$digest();
        expect(elm.scope().carouselCollection.index).toBe(scope.localIndex);
    });
  });

  describe('directive with a numeric index defined', function () {
    it('the index attribute should be used to position the first visible slide', function () {
        var elm = compileTpl({useIndex: 5});
        waitAndCheck(function() {
          validCSStransform(elm);
        }, 200);
    });
    it('index change should update the carousel position', function () {
        // check watcher present even if index is not a bindable attribute
        var elm = compileTpl({useIndex: 5});
        elm.scope().carouselCollection.goToIndex(9);
        scope.$digest();
        waitAndCheck(function() {
          validCSStransform(elm);
        }, 200);
    });
    it('index out of range should set the carousel to last slide', function () {
        var elm = compileTpl({useIndex: 100});
        expect(elm.scope().carouselCollection.index).toBe(scope.items.length - 1);
        expect(elm.find('li').length).toBe(scope.items.length);
        expect(elm.find('li:last')[0].id).toBe('slide-' + (scope.items.length - 1));
    });
    it('negative index should set the carousel to first slide', function () {
        var elm = compileTpl({useIndex: -100});
        expect(elm.scope().carouselCollection.index).toBe(0);
        expect(elm.find('li').length).toBe(scope.items.length);
        expect(elm.find('li')[0].id).toBe('slide-0');
    });
  });

  describe('directive with no index defined', function () {
    it('should add a wrapper div around the ul/li', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.parent().hasClass('rn-carousel-container')).toBe(true);
    });
    it('should add a class to the ul', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.hasClass('rn-carousel-slides')).toBe(true);
    });
    it('should have enough slides', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.find('li').length).toBe(scope.items.length);
    });
    it('generated container outerWidth should match the ul outerWidth', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.parent().outerWidth()).toBe(elm.outerWidth());
    });
    it('the index attribute should be used to position the first visible slide', function () {
        var elm = compileTpl({useIndex:false});
        validCSStransform(elm);
    });
  });

  describe('indicator directive', function () {
    it('should add an indicator div', function () {
        var elm = compileTpl({useIndicator: true});
        expect(elm.parent().find('.rn-carousel-indicator').length).toBe(1);
    });
    it('should add enough indicators', function () {
        var elm = compileTpl({useIndicator: true});
        expect(elm.parent().find('.rn-carousel-indicator span').length).toBe(scope.items.length);
    });
    it('should have an active indicator based on the carousel index', function () {
        var elm = compileTpl({useIndicator: true});
        expect(elm.parent().find('.rn-carousel-indicator span:nth-of-type(' + (elm.scope().carouselCollection.index + 1) + ')').hasClass('active')).toBe(true);
    });
    it('should update the active indicator when local index changes', function () {
        var elm = compileTpl({useIndicator: true, useIndex: 'localIndex'});
        scope.localIndex = 2;
        scope.$digest();
        expect(elm.parent().find('.rn-carousel-indicator span:nth-of-type(' + (scope.localIndex + 1) + ')').hasClass('active')).toBe(true);
    });
  });

  describe('controls directive', function () {
    it('should add an controls div', function () {
        var elm = compileTpl({useControl: true});
        expect(elm.parent().find('.rn-carousel-controls').length).toBe(1);
    });
    it('should have next control but not back', function () {
        var elm = compileTpl({useControl: true});
        expect(elm.parent().find('.rn-carousel-control-next').length).toBe(1);
        expect(elm.parent().find('.rn-carousel-control-back').length).toBe(0);
    });
    it('should have next and back controls when local index changes', function () {
        var elm = compileTpl({useControl: true, useIndex: 'localIndex'});
        scope.localIndex = 1;
        scope.$digest();
        expect(elm.parent().find('.rn-carousel-control').length).toBe(2);
    });
    it('should have only back controls when local index is at the end', function () {
        var elm = compileTpl({useControl: true, useIndex: 'localIndex'});
        scope.localIndex = scope.items.length - 1;
        scope.$digest();
        expect(elm.parent().find('.rn-carousel-control-next').length).toBe(0);
        expect(elm.parent().find('.rn-carousel-control-back').length).toBe(1);
    });
  });

  describe('directive with no index defined', function () {
    it('should add a wrapper div around the ul/li', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.parent().hasClass('rn-carousel-container')).toBe(true);
    });
    it('should add a class to the ul', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.hasClass('rn-carousel-slides')).toBe(true);
    });
    it('should have enough slides', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.find('li').length).toBe(scope.items.length);
    });
    it('generated container outerWidth should match the ul outerWidth', function () {
        var elm = compileTpl({useIndex:false});
        expect(elm.parent().outerWidth()).toBe(elm.outerWidth());
    });
    it('the index attribute should be used to position the first visible slide', function () {
        var elm = compileTpl({useIndex:false});
        validCSStransform(elm);
    });
  });

  describe('buffered carousel', function () {
    it('should minimize the DOM', function () {
        var elm = compileTpl({useBuffer: true});
        expect(elm.find('li').length).toBe(3);
    });
    // TODO
    it('should position the buffered slides correctly', function () {
        var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
        scope.localIndex = 5;
        scope.$digest();
        expect(elm.find('li')[0].id).toBe('slide-' + (scope.localIndex - 1));
    });
    it('should position the buffered slides correctly even if index is zero', function () {
        var elm = compileTpl({useBuffer: true, useIndex: '0'});
        expect(elm.find('li').length).toBe(3);
        expect(elm.find('li')[0].id).toBe('slide-0');
    });
    it('should position the buffered slides correctly with a out of range index', function () {
        var elm = compileTpl({useBuffer: true, useIndex: '100'});
        expect(elm.scope().carouselCollection.index).toBe(scope.items.length - 1);
        var firstId = scope.items.length - 3;
        expect(elm.find('li').length).toBe(3);
        expect(elm.find('li')[0].id).toBe('slide-' + firstId);
        expect(elm.find('li:last')[0].id).toBe('slide-' + (firstId + 3 - 1));
    });
    it('should position the buffered slides correctly with a negative index', function () {
        var elm = compileTpl({useBuffer: true, useIndex: '-100'});
        expect(elm.scope().carouselCollection.index).toBe(0);
        expect(elm.find('li').length).toBe(3);
        expect(elm.find('li')[0].id).toBe('slide-0');
        expect(elm.find('li:last')[0].id).toBe('slide-' + (3 - 1));
    });
  });

  describe('index property on standard carousel', function () {
    it('should be at 0 on start', function () {
        var elm = compileTpl();
        expect(elm.scope().carouselCollection.index).toBe(0);
    });
    it('should be set at initial position', function () {
        var elm = compileTpl({useIndex: 'localIndex'});
        expect(elm.scope().carouselCollection.index).toBe(scope.localIndex);
    });
    it('should follow carousel position', function () {
        var elm = compileTpl({useIndex: 'localIndex'});
        scope.localIndex = scope.items.length - 1;
        scope.$digest();
        expect(elm.scope().carouselCollection.index).toBe(scope.items.length - 1);
    });
  });

  describe('index property on buffered carousel', function () {
    it('should be at 0 on start', function () {
        var elm = compileTpl({useBuffer: true});
        expect(elm.find('li')[0].id).toBe('slide-0');
        expect(elm.scope().carouselCollection.index).toBe(0);
    });
    it('should be set correctly at initial position', function () {
        var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
        expect(elm.scope().carouselCollection.index).toBe(scope.localIndex);
        expect(elm.find('li')[0].id).toBe('slide-' + (scope.localIndex - 1));
    });
    it('should be last item of buffer if carousel last slide', function () {
        var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
        scope.localIndex = scope.items.length - 1;
        scope.$digest();
        waitAndCheck(function() {
          expect(elm.scope().carouselCollection.index).toBe(scope.localIndex);
          expect(elm.find('li')[0].id).toBe('slide-' + (scope.localIndex - 2));
        });
    });
    it('should be last item of buffer if carousel last slide', function () {
        var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
        scope.localIndex = 100;
        scope.$digest();
        waitAndCheck(function() {
          expect(elm.scope().carouselCollection.index).toBe(scope.items.length - 1);
          expect(elm.find('li')[0].id).toBe('slide-' + (scope.localIndex-2));
        });
    });
    it('should display first slide when reset local index to 0', function () {
        var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
        scope.localIndex = 5;
        scope.$digest();
        scope.localIndex = 0;
        scope.$digest();
        expect(elm.position().left).toBe(0);
        expect(elm.css('left')).toBe('auto');
    });
  });

  // TODO
  // describe('collection update', function () {
  //    it('standard carousel should display first slide when we reset the collection', function () {
  //       var elm = compileTpl({useIndex: 'localIndex'});
  //       scope.localIndex = 5;
  //       scope.$digest();
  //       scope.items = [{id:1}, {id:2}];
  //       scope.$digest();
  //       expect(elm.position().left).toBe(0);
  //       expect(elm.css('left')).toBe('auto');
  //       expect(elm.scope().activeIndex).toBe(0);
  //   });
  //   it('buffered carousel should display first slide when we reset the collection', function () {
  //       var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
  //       scope.localIndex = 5;
  //       scope.$digest();
  //       scope.items = [{id:1}, {id:2}];
  //       scope.$digest();
  //       expect(elm.position().left).toBe(0);
  //       expect(elm.css('left')).toBe('auto');
  //       expect(elm.scope().activeIndex).toBe(0);
  //   });
  // });

  function fakeMove(elm, distance) {
    // trigger a carousel swipe movement
    var startX = 100,
        startY = 10,
        endX = distance + startX;

    browserTrigger(elm, 'touchstart', [], startX, startY);
    browserTrigger(elm, 'touchmove', [], endX, startY);
    browserTrigger(elm, 'touchmove', [], endX, startY);
    browserTrigger(elm, 'touchend', [], endX, startY);
  }
  function waitAndCheck(cb, delay) {
    waits(delay || 100);
    runs(cb);
  }
  describe('swipe behaviour', function () {
    var minMove;
    beforeEach(function() {
        minMove = 31;
    });
    it('should not show prev slide if swipe backwards at index 0', function() {
        // yes, backwards swipe means positive pixels count :)
        var elm = compileTpl();
        fakeMove(elm, minMove);
        expect(elm.scope().carouselCollection.index).toBe(0);
    });
    it('should not show next slide if swipe forward at last slide', function() {
        var elm = compileTpl();
        elm.scope().carouselCollection.goToIndex(scope.items.length - 1);
        fakeMove(elm, -minMove);
        expect(elm.scope().carouselCollection.index).toBe(scope.items.length - 1);
    });
    it('should move slide backward if backwards swipe at index > 0', function() {
        var elm = compileTpl({useIndex: 1});
        fakeMove(elm, minMove);
        expect(elm.scope().carouselCollection.index).toBe(0);
    });
    it('should move to next slide on swipe forward', function() {
        var elm = compileTpl();
        fakeMove(elm, -minMove);
        expect(elm.scope().carouselCollection.index).toBe(1);
    });
    it('should not move to next slide on too little swipe forward', function() {
        var elm = compileTpl();
        fakeMove(elm, -12);
        expect(elm.scope().carouselCollection.index).toBe(0);
    });
    it('should not move to prev slide on too little swipe backward', function() {
        var elm = compileTpl({useIndex: 1});
        fakeMove(elm, 12);
        expect(elm.scope().carouselCollection.index).toBe(1);
    });
    it('should follow multiple moves', function() {
        var elm = compileTpl();
       // var minMove = -(elm.outerWidth() * 0.1 + 1);
        fakeMove(elm, -minMove);
        //console.log(minMove, elm.scope().carouselCollection.index);
        fakeMove(elm,-minMove);
        fakeMove(elm, -minMove);
        expect(elm.scope().carouselCollection.index).toBe(3);
        fakeMove(elm, minMove);
        fakeMove(elm, minMove);
        expect(elm.scope().carouselCollection.index).toBe(1);
        fakeMove(elm, minMove);
        fakeMove(elm, minMove);
        fakeMove(elm, minMove);
        expect(elm.scope().carouselCollection.index).toBe(0);
    });
  });

  describe('swipe buffered behaviour', function () {
    it('should follow multiple moves and buffer accordingly', function() {
        var elm = compileTpl({useBuffer: true});
        var minMove = -(elm.outerWidth() * 0.1 + 1);
        fakeMove(elm, minMove);

        waitAndCheck(function() {
          expect(elm.scope().carouselCollection.index).toBe(1);
          expect(elm.find('li')[0].id).toBe('slide-0');
          fakeMove(elm, minMove);
          waitAndCheck(function() {
            expect(elm.scope().carouselCollection.index).toBe(2);
            expect(elm.find('li')[0].id).toBe('slide-1');
            fakeMove(elm, -minMove);
            waitAndCheck(function() {
              expect(elm.scope().carouselCollection.index).toBe(1);
              expect(elm.find('li')[0].id).toBe('slide-0');
              fakeMove(elm, -minMove);
              waitAndCheck(function() {
                expect(elm.scope().carouselCollection.index).toBe(0);
                expect(elm.find('li')[0].id).toBe('slide-0');
              });
            });
          });
        });
    });
  });

  describe('collection watch', function () {
    describe('standard watch (no deep)', function () {
      it('it should display first slide when we reset the collection', function () {
        var elm = compileTpl({useIndex: 'localIndex'});
        scope.localIndex = 5;
        scope.$digest();
        expect(elm.scope().carouselCollection.index).toBe(5);
        scope.items = [{id:1}, {id:2}];
        scope.$digest();
        expect(elm.position().left).toBe(0);
        expect(elm.css('left')).toBe('auto');
        expect(elm.scope().carouselCollection.index).toBe(0);
      });
      it('should NOT update slides when collection changes partially', function() {
        var elm = compileTpl();
        var originalLength = scope.items.length;
        expect(elm.find('li').length).toBe(originalLength);
        scope.items.push({'text': 'new item', 'id': 999});
        scope.$digest();
        expect(elm.find('li').length).toBe(originalLength);
        expect(elm.find('li').last()[0].id).toBe('slide-' + (originalLength - 1));
        scope.items.pop();
        scope.items.pop();
        scope.items.pop();
        scope.$digest();
        expect(elm.find('li').length).toBe(originalLength);
        expect(elm.find('li').last()[0].id).toBe('slide-' + (originalLength - 1));
      });
    });
    describe('standard watch (no deep) + buffer', function () {
      it('it should display first slide when we reset the collection', function () {
        var elm = compileTpl({useBuffer: true, useIndex: 'localIndex'});
        scope.localIndex = 5;
        scope.$digest();
        expect(elm.scope().carouselCollection.index).toBe(5);
        scope.items = [{id:1}, {id:2}];
        scope.$digest();
        expect(elm.position().left).toBe(0);
        expect(elm.css('left')).toBe('auto');
        expect(elm.scope().carouselCollection.index).toBe(0);
      });
      it('should NOT update slides when collection changes partially', function() {
        var elm = compileTpl({useBuffer: true});
        var originalLength = elm.scope().carouselCollection.bufferSize;
        expect(elm.find('li').length).toBe(originalLength);
        scope.items.push({'text': 'new item', 'id': 999});
        scope.$digest();
        expect(elm.find('li').length).toBe(originalLength);
        expect(elm.find('li').last()[0].id).toBe('slide-' + (originalLength - 1));
        scope.items.pop();
        scope.items.pop();
        scope.items.pop();
        scope.$digest();
        expect(elm.find('li').length).toBe(originalLength);
        expect(elm.find('li').last()[0].id).toBe('slide-' + (originalLength - 1));
      });
    });
    describe('deep watch', function () {
      it('should display first slide when we reset the collection', function () {
          var elm = compileTpl({useIndex: 'localIndex', useWatch: true});
          scope.localIndex = 5;
          scope.$digest();
          expect(elm.scope().carouselCollection.index).toBe(5);
          scope.items = [{id:1}, {id:2}];
          scope.$digest();
          expect(elm.position().left).toBe(0);
          expect(elm.css('left')).toBe('auto');
          expect(elm.scope().carouselCollection.index).toBe(0);
      });
      it('should update slides when collection changes partially', function() {
        var elm = compileTpl({useWatch: true});
        expect(elm.find('li').length).toBe(scope.items.length);
        scope.items.push({'text': 'new item', 'id': 999});
        scope.$digest();
        expect(elm.find('li').length).toBe(scope.items.length);
        expect(elm.find('li').last()[0].id).toBe('slide-999');
        scope.items.pop();
        scope.items.pop();
        scope.items.pop();
        scope.$digest();
        expect(elm.find('li').length).toBe(scope.items.length);
        expect(elm.find('li').last()[0].id).toBe('slide-' + scope.items[scope.items.length - 1].id);
      });
    });
    describe('deep watch + buffer', function () {
      it('should display first slide when we reset the collection', function () {
          var elm = compileTpl({userBuffer:true, useIndex: 'localIndex', useWatch: true});
          scope.localIndex = 5;
          scope.$digest();
          expect(elm.scope().carouselCollection.index).toBe(5);
          scope.items = [{id:1}, {id:2}];
          scope.$digest();
          expect(elm.position().left).toBe(0);
          expect(elm.css('left')).toBe('auto');
          expect(elm.scope().carouselCollection.index).toBe(0);
      });
      it('should update slides when collection changes partially', function() {
        var elm = compileTpl({userBuffer:true, useWatch: true});
        expect(elm.find('li').length).toBe(scope.items.length);
        scope.items.push({'text': 'new item', 'id': 999});
        scope.$digest();
        expect(elm.find('li').length).toBe(scope.items.length);
        expect(elm.find('li').last()[0].id).toBe('slide-999');
        scope.items.pop();
        scope.items.pop();
        scope.items.pop();
        scope.$digest();
        expect(elm.find('li').length).toBe(scope.items.length);
        expect(elm.find('li').last()[0].id).toBe('slide-' + scope.items[scope.items.length - 1].id);
      });
    });
    
  });
  // describe('delayed collection and index', function () {
  //   it('should follow multiple moves and buffer accordingly', function() {

  // describe('swipe buffered + index behaviour', function () {
  //   it('should initialise buffer start correctly when index is set', function() {
  //       var elm = compileTpl({useBuffer: true, useIndex: "localIndex", nbItems: 5});
  //       scope.localIndex = 2;
  //       scope.$digest();
  //       expect(elm.scope().carouselBufferStart).toBe(1);
  //   });
  //   it('should initialise buffer start correctly when index is set at 0', function() {
  //       var elm = compileTpl({useBuffer: true, useIndex: "localIndex", nbItems: 5});
  //       scope.localIndex = 0;
  //       scope.$digest();
  //       expect(elm.scope().carouselBufferStart).toBe(0);
  //   });
    // it('should initialise buffer start correctly when index is set at last item', function() {
    //     var nbItems = 5;
    //     var elm = compileTpl({useBuffer: true, useIndex: "localIndex", nbItems: 5});
    //     scope.localIndex = nbItems-1;
    //     scope.$digest();
    //     console.log(elm.scope().activeIndex);
    //     waits(10);
    //     runs(function() {
    //         expect(elm.scope().carouselBufferStart).toBe(nbItems - elm.scope().carouselBufferSize);
    //     });
    // });
    // it('buffer position should update when local index changes', function() {
    //     var elm = compileTpl({useBuffer: true, useIndex: "localIndex", nbItems: 5});
    //     scope.localIndex = 2;
    //     scope.$digest();
    //     expect(elm.scope().carouselBufferStart).toBe(1);
    //     scope.localIndex = 3;
    //     scope.$digest();
    //     waits(100);
    //     runs(function() {
    //         expect(elm.scope().carouselBufferStart).toBe(1);
    //         scope.localIndex = 0;
    //         scope.$digest();
    //         expect(elm.scope().carouselBufferStart).toBe(0);
    //     });
    // });
  //});
*/

});

