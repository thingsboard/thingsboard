describe('uiAce', function () {
  'use strict';

  var scope, $compile,
    uiConfig;

  beforeEach(function () {

    module('ui.ace');

    inject(function (_$rootScope_, _$compile_, uiAceConfig) {
      scope = _$rootScope_.$new();
      $compile = _$compile_;
      uiConfig = uiAceConfig;
      uiConfig.ace = { showGutter: false };
    });
  });

  afterEach(function () {
    uiConfig = {};
  });

  describe('require', function () {
    var aceRequireFunction;

    beforeEach(function () {
      aceRequireFunction = window.ace.edit;
      window.ace.require = jasmine
        .createSpy('window.ace.require');
    });

    afterEach(function () {
      window.ace.require = aceRequireFunction;
    });

    it('should not call window.ace.require if there is no "require" option', function () {
      $compile('<div ui-ace>')(scope);
      expect(window.ace.require).not.toHaveBeenCalled();
    });

    it('should not call ace/config if a workerPath is not defined', function () {
      $compile('<div ui-ace>')(scope);
      expect(window.ace.require).not.toHaveBeenCalledWith('ace/config');
    });

    it('should call ace/config if a workerPath is defined', function () {
      window.ace.require
        .and.returnValue({
          set: function () {}
        });
      ////
      $compile('<div ui-ace=\'{ workerPath: "/path/to/ace" }\'>')(scope);
      expect(window.ace.require).toHaveBeenCalledWith('ace/config');
    });

    it('should call "set" if workerPath is defined', function () {
      var _config = jasmine.createSpyObj('config', ['set']);
      window.ace.require.and.returnValue(_config);
      ////
      $compile('<div ui-ace=\'{ workerPath: "/path/to/ace" }\'>')(scope);
      expect(_config.set).toHaveBeenCalled();
    });

    it('should call "window.ace.require" for each option in "require"', function () {
      $compile('<div ui-ace=\'{ require: ["ace/ext/language_tools", "ace/ext/static_highlight"]}\'>')(scope);
      expect(window.ace.require).toHaveBeenCalled();
      expect(window.ace.require.calls.count()).toEqual(2);
    });
  });

  describe('options', function () {
    var _ace, aceEditFunction;

    beforeEach(function () {
      aceEditFunction = window.ace.edit;
      window.ace.edit = jasmine
        .createSpy('window.ace.edit')
        .and.callFake(function () {
          _ace = aceEditFunction.apply(this, arguments);
          _ace.setOption = jasmine
            .createSpy('ace.setOption')
            .and.callThrough();
          _ace.renderer.setOption = jasmine
            .createSpy('ace.setOption')
            .and.callThrough();
          return _ace;
        });
    });

    afterEach(function () {
      window.ace.edit = aceEditFunction;
    });

    it('should not trigger ace#setOption.', function () {
      $compile('<div ui-ace>')(scope);
      expect(_ace.setOption.calls.count()).toEqual(0);
    });

    it('should trigger ace#setOption with "advanced" options.', function () {
      $compile('<div ui-ace=\'{ advanced: { enableSnippets: true } }\'>')(scope);
      expect(_ace.setOption.calls.count()).toEqual(1);
      expect(_ace.setOption).toHaveBeenCalledWith('enableSnippets', true);
    });

    it('should trigger renderer#setOption with "rendererOptions" options.', function () {
      $compile('<div ui-ace=\'{ rendererOptions: { maxLines: 42 } }\'>')(scope);
      expect(_ace.renderer.setOption.calls.count()).toEqual(2);
      expect(_ace.renderer.setOption).toHaveBeenCalledWith('showGutter', false);
      expect(_ace.renderer.setOption).toHaveBeenCalledWith('maxLines', 42);
    });
  });

  describe('basic behavior', function () {

    it('should not throw an error when window.ace is defined', function () {
      function compile() {
        $compile('<div ui-ace>')(scope);
      }

      expect(compile).not.toThrow();
    });

    it('should watch the uiAce attribute', function () {
      spyOn(scope, '$watch');
      $compile('<div ui-ace ng-model="foo">')(scope);
      expect(scope.$watch).toHaveBeenCalled();
    });
  });

  describe('instance', function () {
    var _ace, aceEditFunction;

    beforeEach(function () {
      aceEditFunction = window.ace.edit;
      window.ace.edit = jasmine
        .createSpy('window.ace.edit')
        .and.callFake(function () {
          _ace = aceEditFunction.apply(this, arguments);
          _ace.setReadOnly = jasmine.createSpy('ace.setReadOnly')
            .and.callThrough();
          return _ace;
        });
    });

    afterEach(function () {
      window.ace.edit = aceEditFunction;
    });

    it('should call ace.edit', function () {
      $compile('<div ui-ace>')(scope);
      expect(_ace).toBeDefined();
    });

    describe('options', function () {
      describe('passed', function () {
        it('should show the showGutter', function () {
          $compile('<div ui-ace="{showGutter:true}">')(scope);
          expect(_ace.renderer).toBeDefined();
          expect(_ace.renderer.getShowGutter()).toBeTruthy();
        });
      });
      describe('global', function () {
        it('should hide the showGutter', function () {
          $compile('<div ui-ace>')(scope);
          expect(_ace.renderer).toBeDefined();
          expect(_ace.renderer.getShowGutter()).toBeFalsy();
        });
      });
      describe('onLoad', function () {
        it('should call the local onLoad callback', function () {
          scope.aceLoaded = jasmine.createSpy('scope.aceLoaded');
          $compile('<div ui-ace="{onLoad: aceLoaded}">')(scope);
          expect(scope.aceLoaded).toHaveBeenCalled();
          expect(scope.aceLoaded).toHaveBeenCalledWith(_ace);
        });

        it('should call the global onLoad callback', function () {
          uiConfig.ace.onLoad = jasmine.createSpy('uiConfig.ace.onLoad');
          $compile('<div ui-ace>')(scope);
          expect(uiConfig.ace.onLoad).toHaveBeenCalled();
          expect(uiConfig.ace.onLoad).toHaveBeenCalledWith(_ace);
        });

        it('should call both local/global onLoad callbacks', function () {
          scope.aceLoaded = jasmine.createSpy('scope.aceLoaded');
          uiConfig.ace.onLoad = jasmine.createSpy('uiConfig.ace.onLoad');
          $compile('<div ui-ace="{onLoad: aceLoaded}">')(scope);
          expect(uiConfig.ace.onLoad).toHaveBeenCalled();
          expect(uiConfig.ace.onLoad).toHaveBeenCalledWith(_ace);
          expect(scope.aceLoaded).toHaveBeenCalled();
          expect(scope.aceLoaded).toHaveBeenCalledWith(_ace);
        });

        it('should detect same local/global onLoad callback', function () {
          uiConfig.ace.onLoad = jasmine.createSpy('uiConfig.ace.onLoad');
          scope.aceLoaded = uiConfig.ace.onLoad;
          $compile('<div ui-ace="{onLoad: aceLoaded}">')(scope);
          expect(uiConfig.ace.onLoad.calls.count()).toBe(1);
          expect(uiConfig.ace.onLoad).toHaveBeenCalledWith(_ace);
        });
      });
    });

    describe('readOnly', function () {

      it('should read only option true', function () {
        $compile('<div ui-ace readonly>')(scope);
        scope.$apply();
        expect(_ace.setReadOnly).toHaveBeenCalledWith(true);

        $compile('<div ui-ace ng-readonly="true">')(scope);
        scope.$apply();
        expect(_ace.setReadOnly).toHaveBeenCalledWith(true);

        $compile('<div ui-ace ng-readonly="foo">')(scope);
        scope.$apply('foo = true');
        expect(_ace.setReadOnly).toHaveBeenCalledWith(true);

      });

      it('should read only option false', function () {
        $compile('<div ui-ace>')(scope);
        scope.$apply();
        expect(_ace.setReadOnly).not.toHaveBeenCalled();

        $compile('<div ui-ace ng-readonly="false">')(scope);
        scope.$apply();
        expect(_ace.setReadOnly).toHaveBeenCalledWith(false);

        $compile('<div ui-ace ng-readonly="foo">')(scope);
        scope.$apply();
        expect(_ace.setReadOnly).toHaveBeenCalledWith(false);

        scope.$apply('foo = null');
        expect(_ace.setReadOnly).toHaveBeenCalledWith(false);

        scope.$apply('foo = false');
        expect(_ace.setReadOnly).toHaveBeenCalledWith(false);
      });
    });

    describe('when the model changes', function () {
      it('should update the IDE', function () {
        $compile('<div ui-ace ng-model="foo">')(scope);
        scope.$apply('foo = "bar"');
        expect(_ace.getSession().getValue()).toBe(scope.foo);
      });
    });

    describe('when the IDE changes', function () {
      it('should update the model', function () {
        $compile('<div ui-ace ng-model="foo">')(scope);
        scope.$apply('foo = "bar"');

        var value = 'baz';
        _ace.getSession().setValue(value);
        scope.$apply();

        expect(scope.foo).toBe(value);
      });

      it('should update the IDE only if different', function () {
        scope.change = jasmine.createSpy('scope.change');

        $compile('<div ui-ace ng-model="foo" ng-change="change(foo)">')(scope);

        // change shouldn't be called initialy
        expect(scope.change).not.toHaveBeenCalled();

        // change shouldn't be called when the value change is coming from the model.
        scope.$apply('foo = "bar"');
        expect(scope.change).not.toHaveBeenCalled();

        _ace.getSession().setValue('baz');
        scope.$apply();

        // ace removeText event + ace insertText event
        expect(scope.change.calls.count()).toBe(2);
        // ace removeText event
        expect(scope.change).toHaveBeenCalledWith('');
        // ace insertText event
        expect(scope.change).toHaveBeenCalledWith('baz');

        //
        expect(scope.foo).toBe('baz');
      });
    });

    describe('when the model is undefined/null', function () {
      it('should update the IDE with an empty string', function () {
        $compile('<div ui-ace ng-model="foo">')(scope);
        scope.$apply();
        expect(scope.foo).toBeUndefined();
        expect(_ace.getSession().getValue()).toBe('');
        scope.$apply('foo = "bar"');
        expect(scope.foo).toBe('bar');
        expect(_ace.getSession().getValue()).toBe('bar');
        scope.$apply('foo = null');
        expect(scope.foo).toBe(null);
        expect(_ace.getSession().getValue()).toBe('');
      });
    });

    describe('when the callback is not a function', function () {
      it('should throw an error', function () {
        function compileWithObject() {
          scope.changing = {};
          scope.$apply('foo = "bar"');
          $compile('<div ui-ace="{onChange: changing}" ng-model="foo">')(scope);
          _ace.getSession().setValue('baz');
          scope.$apply();
        }

        expect(compileWithObject).toThrow();
      });
    });

    it('should call destroy when the element is removed', function () {
      var element = $compile('<div ui-ace ng-model="foo">')(scope);
      _ace.destroy = jasmine.createSpy('ace.destroy')
        .and.callThrough();
      _ace.session.$stopWorker = jasmine.createSpy('ace.session.$stopWorker')
        .and.callThrough();

      element.remove();
      scope.$apply();

      expect(_ace.session.$stopWorker).toHaveBeenCalled();
      expect(_ace.destroy).toHaveBeenCalled();
    });
  });

  describe('when the model is an object or an array', function () {
    it('should throw an error', function () {
      function compileWithObject() {
        $compile('<div ui-ace ng-model="foo">')(scope);
        scope.foo = {};
        scope.$apply();
      }

      function compileWithArray() {
        $compile('<div ui-ace ng-model="foo">')(scope);
        scope.foo = [];
        scope.$apply();
      }

      expect(compileWithObject).toThrow();
      expect(compileWithArray).toThrow();
    });
  });

});
