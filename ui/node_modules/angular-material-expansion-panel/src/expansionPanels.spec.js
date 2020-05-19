describe('material.components.expansionPanels', function () {
  var panel;
  var group;
  var scope;
  var content;
  var $mdUtil;
  var $timeout;
  var $animate;


  // disable all css transitions so test wont fail
  var disableAnimationStyles = ''+
    '-webkit-transition: none !important;'+
    '-moz-transition: none !important;'+
    '-ms-transition: none !important;'+
    '-o-transition: none !important;'+
    'transition: none !important;';
  window.onload = function () {
    var animationStyles = document.createElement('style');
    animationStyles.type = 'text/css';
    animationStyles.innerHTML = '* {' + disableAnimationStyles + '}';
    document.head.appendChild(animationStyles);
  };



  beforeEach(module('ngAnimateMock'));
  beforeEach(module('material.components.expansionPanels'));
  beforeEach(inject(function(_$mdUtil_, _$timeout_, _$animate_) {
    $mdUtil = _$mdUtil_;
    // getComputedStyle does not work in phantomjs. mock out method
    $mdUtil.hasComputedStyle = function () { return false; };

    $timeout = _$timeout_;
    $animate = _$animate_;
  }));

  // destroy all created scopes and elements
  afterEach(function () {
    if (scope == undefined) { return; }
    scope.$destroy();
    panel.remove();
    panel = undefined;
    scope = undefined;

    if (group) {
      group.scope().$destroy();
      group.remove();
      group = undefined;
    }

    if (content) {
      content.remove();
      content = undefined;
    }
  });






  // --- Expansion Panel Service ---

  describe('$mdExpansionPanel Service', function () {
    it('should find instance by id with sync method', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      var instance = $mdExpansionPanel('expansionPanelId');
      expect(instance).toBeDefined();
    }));

    it('should find instance by id with async method', inject(function($mdExpansionPanel) {
      var instance;

      $mdExpansionPanel().waitFor('expansionPanelId').then(function(inst) {
        instance = inst;
      });
      expect(instance).toBeUndefined();

      var element = getDirective({componentId: 'expansionPanelId'});
      $timeout.flush();

      expect(instance).toBeDefined();
    }));


    it('should expand panel', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').expand();
      flushAnimations();

      expect(element.hasClass('md-open')).toBe(true);
    }));


    it('should collapse panel', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').expand();
      $mdExpansionPanel('expansionPanelId').collapse();
      flushAnimations();

      expect(element.hasClass('md-close')).toBe(true);
    }));

    it('should remove panel', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').remove();
      expect(element.scope()).toBeUndefined();
    }));


    it('should call onRemove callabck', inject(function($mdExpansionPanel) {
      var obj = {
        callback: function () {}
      };
      spyOn(obj, 'callback');
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').onRemove(obj.callback);
      $mdExpansionPanel('expansionPanelId').remove();

      expect(obj.callback).toHaveBeenCalled();
    }));


    it('should add a click catcher', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').addClickCatcher();
      expect(element.parent().find('md-backdrop')).toBeDefined();
    }));


    it('should call clickback', inject(function($mdExpansionPanel) {
      var obj = {
        callback: function () {}
      };
      spyOn(obj, 'callback');
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').addClickCatcher(obj.callback);
      element.parent().find('md-backdrop').triggerHandler('click');

      expect(obj.callback).toHaveBeenCalled();
    }));


    it('should remove click catcher', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').addClickCatcher();
      $mdExpansionPanel('expansionPanelId').removeClickCatcher();
      expect(element.parent().find('md-backdrop').length).toEqual(0);
    }));


    it('should return false for isOpen', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      expect($mdExpansionPanel('expansionPanelId').isOpen()).toBe(false);
    }));

    it('should return true for isOpen', inject(function($mdExpansionPanel) {
      var element = getDirective({componentId: 'expansionPanelId'});
      $mdExpansionPanel('expansionPanelId').expand();
      flushAnimations();

      expect($mdExpansionPanel('expansionPanelId').isOpen()).toBe(true);
    }));
  });






  // --- Expansion Panel Group Service ---

  describe('$mdExpansionPanelGroup Service', function () {
    it('should find instance by id using sync method', inject(function($mdExpansionPanelGroup) {
      var element = getGroupDirective();
      var instance = $mdExpansionPanelGroup('expansionPanelGroupId');
      expect(instance).toBeDefined();
    }));


    it('should find instance by id usign async method', inject(function($mdExpansionPanelGroup) {
      var instance;

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
      });
      expect(instance).toBeUndefined();

      var element = getGroupDirective();
      $timeout.flush();

      expect(instance).toBeDefined();
    }));


    it('should register panel and add it', inject(function($mdExpansionPanelGroup) {
      var instance;
      var panelInstance;

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.register('panelName', {
          template: getTemplate(),
          controller: function () {}
        });

        inst.add('panelName').then(function (_panelInstance) {
          panelInstance = _panelInstance;
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(panelInstance).toBeDefined();
    }));


    it('should remove added panel', inject(function($mdExpansionPanelGroup) {
      var instance;

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.add({
          template: getTemplate({componentId: 'expansionPanelId'}),
          controller: function () {}
        }).then(function () {
          inst.remove('expansionPanelId');
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(element[0].querySelector('[md-component-id="expansionPanelId"]')).toBe(null);
    }));


    it('should remove all panels', inject(function($mdExpansionPanelGroup) {
      var instance;

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.add({
          template: getTemplate({componentId: 'expansionPanelOne'}),
          controller: function () {}
        });

        inst.add({
          template: getTemplate({componentId: 'expansionPanelTwo'}),
          controller: function () {}
        }).then(function () {
          inst.removeAll();
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(element[0].querySelector('[md-component-id="expansionPanelOne"]')).toBe(null);
      expect(element[0].querySelector('[md-component-id="expansionPanelTwo"]')).toBe(null);
    }));


    it('should get all panel instances', inject(function($mdExpansionPanelGroup) {
      var instance;
      var all = [];

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.add({
          template: getTemplate({componentId: 'expansionPanelOne'}),
          controller: function () {}
        });

        inst.add({
          template: getTemplate({componentId: 'expansionPanelTwo'}),
          controller: function () {}
        }).then(function () {
          all = inst.getAll();
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(all.length).toBe(2);
    }));


    it('should get opened panel instances', inject(function($mdExpansionPanelGroup) {
      var instance;
      var open = [];

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.add({
          template: getTemplate({componentId: 'expansionPanelOne'}),
          controller: function () {}
        });

        inst.add({
          template: getTemplate({componentId: 'expansionPanelTwo'}),
          controller: function () {}
        }).then(function (panelInst) {

          panelInst.expand().then(function () {
            open = inst.getOpen();
          })
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(open.length).toBe(1);
    }));


    it('should collapse all panels', inject(function($mdExpansionPanelGroup) {
      var instance;
      var open = [];

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.add({
          template: getTemplate({componentId: 'expansionPanelOne'}),
          controller: function () {}
        });

        inst.add({
          template: getTemplate({componentId: 'expansionPanelTwo'}),
          controller: function () {}
        }).then(function (panelInst) {

          panelInst.expand().then(function () {
            instance.collapseAll();
            open = inst.getOpen();
          })
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(open.length).toBe(0);
    }));



    it('should call onChange callback', inject(function($mdExpansionPanelGroup) {
      var obj = {
        callback: function () {}
      };
      spyOn(obj, 'callback');

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        inst.onChange(obj.callback);
        inst.add({
          template: getTemplate({componentId: 'expansionPanelId'}),
          controller: function () {}
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(obj.callback).toHaveBeenCalled();
    }));

    it('should not call onChange callback', inject(function($mdExpansionPanelGroup) {
      var obj = {
        callback: function () {}
      };
      spyOn(obj, 'callback');

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        var change = inst.onChange(obj.callback);
        change();
        inst.add({
          template: getTemplate({componentId: 'expansionPanelId'}),
          controller: function () {}
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(obj.callback).not.toHaveBeenCalled();
    }));


    it('should return count', inject(function($mdExpansionPanelGroup) {
      var instance;

      $mdExpansionPanelGroup().waitFor('expansionPanelGroupId').then(function(inst) {
        instance = inst;
        inst.add({
          template: getTemplate({componentId: 'expansionPanelId'}),
          controller: function () {}
        });
      });

      var element = getGroupDirective();
      $timeout.flush();

      expect(instance.count()).toBe(1);
    }));

  });






  // ---- Expansion Panel Directive ---

  describe('md-expansion-panel directive', function () {
    it('should have `tabindex` attribute', function () {
      var element = getDirective();
      expect(element.attr('tabindex')).not.toBe(undefined);
    });

    it('should set `tabindex` to `-1` if `disabled`', function () {
      var element = getDirective({disabled: true});
      expect(element.attr('tabindex')).toEqual('-1');
    });


    it('should set `tabindex` to `-1` if `ng-disabled` is true', function () {
      var element = getDirective({ngDisabled: 'isDisabled'});
      scope.isDisabled = true;
      scope.$digest();
      expect(element.attr('tabindex')).toEqual('-1');
    });

    it('should set `tabindex` to `0` if `ng-disabled` is false', function () {
      var element = getDirective({ngDisabled: 'isDisabled'});
      scope.isDisabled = false;
      scope.$digest();
      expect(element.attr('tabindex')).toEqual('0');
    });

    it('should set `tabindex` to `0` if `ng-disabled` is not set', function () {
      var element = getDirective({ngDisabled: 'isDisabled'});
      scope.isDisabled = undefined;
      scope.$digest();
      expect(element.attr('tabindex')).toEqual('0');
    });


    it('should thow errors on invalid markup', inject(function($compile, $rootScope) {
      function buildBadPanelOne() {
        $compile('<md-expansion-panel></md-expansion-panel>')($rootScope);
      }

      function buildBadPanelTwo() {
        $compile('<md-expansion-panel><md-expansion-panel-collapsed></md-expansion-panel-collapsed></md-expansion-panel>')($rootScope);
      }

      expect(buildBadPanelOne).toThrow();
      expect(buildBadPanelTwo).toThrow();
    }));


    it('should add `md-open` class on expand', function () {
      var element = getDirective();
      expandPanel();
      expect(element.hasClass('md-open')).toBe(true);
    });

    it('should add `md-close` class on collapse', function () {
      var element = getDirective();
      expandPanel();
      collapsePanel();
      expect(element.hasClass('md-close')).toBe(true);
    });


    it('should remove panel', inject(function($mdExpansionPanel) {
      var element = getDirective();
      element.scope().$panel.remove();
      expect(element.scope()).toBeUndefined();
    }));

    it('should return false for isOpen', inject(function($mdExpansionPanel) {
      var element = getDirective();
      expect(element.scope().$panel.isOpen()).toBe(false);
    }));

    it('should return true for isOpen', inject(function($mdExpansionPanel) {
      var element = getDirective();
      element.scope().$panel.expand();
      flushAnimations();
      expect(element.scope().$panel.isOpen()).toBe(true);
    }));


    describe('Focused', function () {
      it('should be the focused element', function () {
        var element = getDirective();
        focusPanel();
        expect(document.activeElement).toBe(element[0]);
      });

      it('should Expand on `enter` keydown', function () {
        var element = getDirective();
        focusPanel();
        pressKey(13);
        expect(element.hasClass('md-open')).toBe(true);
      });


      it('should Collapse on `escape` keydown', function () {
        var element = getDirective();
        expandPanel();
        focusPanel();
        pressKey(27);

        expect(element.hasClass('md-close')).toBe(true);
      });
    });






    // --- expanded Directive ---

    describe('md-expansion-panel-expanded directive', function () {
      describe('Expanded', function () {
        it('should have `md-show` class', function () {
          var element = getDirective();
          expandPanel();
          expect(element.find('md-expansion-panel-expanded').hasClass('md-show')).toBe(true);
        });

        describe('Animating', function () {
          it('should have `md-overflow` class', function () {
            var element = getDirective();
            expandPanel();
            expect(element.find('md-expansion-panel-expanded').hasClass('md-overflow')).toBe(true);
          });
        });

        describe('After Animating', function () {
          it('should not have `md-overflow` class', function () {
            var element = getDirective();
            expandPanel();
            flushAnimations();
            expect(element.find('md-expansion-panel-expanded').hasClass('md-overflow')).toBe(false);
          });

          it('should not have `max-height` style', function () {
            var element = getDirective();
            expandPanel();
            flushAnimations();
            expect(element.find('md-expansion-panel-expanded').css('max-height')).toBe('none');
          });
        });
      });


      describe('Expanded with height set', function () {
        it('should have `max-height` style', function () {
          var element = getDirective({height: 300});
          expandPanel();
          flushAnimations();
          expect(element.find('md-expansion-panel-expanded').css('max-height')).toBe('300px');
        });

        it('should have `md-scroll-y` class', function () {
          var element = getDirective({height: 300});
          expandPanel();
          flushAnimations();
          expect(element.find('md-expansion-panel-expanded').hasClass('md-scroll-y')).toBe(true);
        });
      });


      describe('Collapse', function () {
        it('should have `md-hide` class', function () {
          var element = getDirective();
          expandPanel();
          collapsePanel();
          expect(element.find('md-expansion-panel-expanded').hasClass('md-hide')).toBe(true);
        });

        it('should have `md-overflow` class', function () {
          var element = getDirective();
          expandPanel();
          collapsePanel();
          expect(element.find('md-expansion-panel-expanded').hasClass('md-overflow')).toBe(true);
        });


        describe('After Animating', function () {
          it('should not have `md-hide` class', function () {
            var element = getDirective();
            expandPanel();
            flushAnimations();
            collapsePanel();
            flushAnimations();
            expect(element.find('md-expansion-panel-expanded').hasClass('md-hide')).toBe(false);
          });
        });
      });
    });






    // --- collapsed Directive ----

    describe('md-expansion-panel-collapsed directive', function () {
      describe('Expanded', function () {
        it('should have `md-hide` class', function () {
          var element = getDirective();
          expandPanel();
          expect(element.find('md-expansion-panel-collapsed').hasClass('md-hide')).toBe(true);
        });

        it('should not have `md-show` class', function () {
          var element = getDirective();
          expandPanel();
          expect(element.find('md-expansion-panel-collapsed').hasClass('md-show')).toBe(false);
        });

        it('should have `width` style', function () {
          var element = getDirective();
          expandPanel();
          expect(element.find('md-expansion-panel-collapsed').css('width')).toBeDefined();
        });


        describe('After Animating', function () {
          it('should have `md-absolute` class', function () {
            var element = getDirective();
            expandPanel();
            flushAnimations();
            expect(element.find('md-expansion-panel-collapsed').hasClass('md-absolute')).toBe(true);
          });

          it('should not have `md-hide` style', function () {
            var element = getDirective();
            expandPanel();
            flushAnimations();
            expect(element.find('md-expansion-panel-collapsed').hasClass('md-hide')).toBe(false);
          });

          it('should add `max-hight` to `md-expansion-panel`', function () {
            var element = getDirective();
            expandPanel();
            flushAnimations();
            expect(element.css('min-height')).toBeDefined();
          });
        });



        describe('Collapsed', function () {
          it('should have `md-show` class', function () {
            var element = getDirective();
            expandPanel();
            collapsePanel();
            expect(element.find('md-expansion-panel-collapsed').hasClass('md-show')).toBe(true);
          });

          it('should have `width` style', function () {
            var element = getDirective();
            expandPanel();
            expect(element.find('md-expansion-panel-collapsed').css('width')).toBeDefined();
          });


          describe('After Animating', function () {
            it('should not have `md-absolute` class', function () {
              var element = getDirective();
              expandPanel();
              flushAnimations();
              collapsePanel();
              flushAnimations();
              expect(element.find('md-expansion-panel-collapsed').hasClass('md-absolute')).toBe(false);
            });

            it('should not have `width` style', function () {
              var element = getDirective();
              expandPanel();
              flushAnimations();
              collapsePanel();
              flushAnimations();
              expect(element.find('md-expansion-panel-collapsed').css('width')).toBe('');
            });

            it('should remove `max-hight` from `md-expansion-panel`', function () {
              var element = getDirective();
              expandPanel();
              flushAnimations();
              collapsePanel();
              flushAnimations();
              expect(element.css('min-height')).toBe('');
            });
          });
        });
      });
    });







    // --- Header Directive ----

    describe('md-expansion-panel-header directive', function () {
      describe('On Scroll', function () {
        it('should have `md-stick` class', inject(function($$rAF) {
          var element = getDirective({content: true});
          expandPanel();
          flushAnimations();

          content[0].scrollTop = 80;
          content.triggerHandler({
            type: 'scroll',
            target: {scrollTop: 80}
          });
          $$rAF.flush();

          expect(element.find('md-expansion-panel-header').hasClass('md-stick')).toBe(true);
        }));
      });


      describe('On Scroll Top', function () {
        it('should not have `md-stick` class', inject(function($$rAF) {
          var element = getDirective({content: true});
          expandPanel();
          flushAnimations();

          content[0].scrollTop = 0;
          content.triggerHandler({
            type: 'scroll',
            target: {scrollTop: 0}
          });
          $$rAF.flush();

          expect(element.find('md-expansion-panel-header').hasClass('md-stick')).toBe(false);
        }));
      });



      describe('No Sticky', function () {
        it('should not have `md-stick` class', inject(function($$rAF) {
          var element = getDirective({content: true, headerNoStick: true});
          expandPanel();
          flushAnimations();

          content[0].scrollTop = 80;
          content.triggerHandler({
            type: 'scroll',
            target: {scrollTop: 80}
          });
          $$rAF.flush();

          expect(element.find('md-expansion-panel-header').hasClass('md-stick')).toBe(false);
        }));
      });
    });






    // --- Foooter Directive ----

    describe('md-expansion-panel-footer directive', function () {
      describe('On Scroll', function () {
        it('should have `md-stick` class', inject(function($$rAF) {
          var element = getDirective({content: true});
          expandPanel();
          flushAnimations();

          content[0].scrollTop = 80;
          content.triggerHandler({
            type: 'scroll',
            target: {scrollTop: 80}
          });
          $$rAF.flush();

          expect(element.find('md-expansion-panel-footer').hasClass('md-stick')).toBe(true);
        }));
      });


      describe('On Scroll Bottom', function () {
        it('should not have `md-stick` class', inject(function($$rAF) {
          var element = getDirective({content: true});
          expandPanel();
          flushAnimations();

          content[0].scrollTop = 1000;
          content.triggerHandler({
            type: 'scroll',
            target: {scrollTop: 1000}
          });
          $$rAF.flush();

          expect(element.find('md-expansion-panel-footer').hasClass('md-stick')).toBe(false);
        }));
      });



      describe('No Sticky', function () {
        it('should not have `md-stick` class', inject(function($$rAF) {
          var element = getDirective({content: true, footerNoStick: true});
          expandPanel();
          flushAnimations();

          content[0].scrollTop = 80;
          content.triggerHandler({
            type: 'scroll',
            target: {scrollTop: 80}
          });
          $$rAF.flush();

          expect(element.find('md-expansion-panel-footer').hasClass('md-stick')).toBe(false);
        }));
      });
    });


  });






  // --- Helpers -------------------


  function getDirective(options) {
    options = options || {};

    var template = getTemplate(options);

    inject(function($compile, $rootScope) {
      scope = $rootScope.$new();
      panel = $compile(template)(scope);
    });

    document.body.appendChild(panel[0]);

    if (options.content) {
      content = panel;
      panel = content.find('md-expansion-panel');
    }

    panel.scope().$digest();
    return panel;
  }

  function getTemplate(options) {
    options = options || {};

    return $mdUtil.supplant('{2}' +
    '<md-expansion-panel {0}{6}{7}>'+
      '><md-expansion-panel-collapsed>'+
        '<div class="md-title">Title</div>'+
        '<div class="md-summary">Summary</div>'+
      '</md-expansion-panel-collapsed>'+
      '<md-expansion-panel-expanded{1}>'+
        '<md-expansion-panel-header{4}>'+
          '<div class="md-title">Expanded Title</div>'+
          '<div class="md-summary">Expanded Summary</div>'+
        '</md-expansion-panel-header>'+
        '<md-expansion-panel-content>'+
          '<h4>Content</h4>'+
          '<p>Put content in here</p>'+
        '</md-expansion-panel-content>'+
        '<md-expansion-panel-footer{5}>'+
          '<div flex></div>'+
          '<md-button class="md-warn">Collapse</md-button>'+
        '</md-expansion-panel-footer>'+
      '</md-expansion-panel-expanded>'+
    '</md-expansion-panel>{3}',
    [
      options.disabled ? 'disabled' : '',
      options.height ? ' height="'+options.height+'" ' : '',
      options.content ? '<md-content style="height: 160px;">' : '',
      options.content ? '</md-content>' : '',
      options.headerNoStick ? ' md-no-sticky ' : '',
      options.footerNoStick ? ' md-no-sticky ' : '',
      options.componentId ? ' md-component-id="'+options.componentId+'" ' : '',
      options.ngDisabled ? ' ng-disabled="'+options.ngDisabled+'" ' : ''
    ]);
  }



  function getGroupDirective(options) {
    options = options || {};

    var template = $mdUtil.supplant('' +
    '<md-expansion-panel-group {0} {1} md-component-id="expansionPanelGroupId">'+
    '</md-expansion-panel-group>',
    [
      options.multiple ? ' multiple ' : '',
      options.autoExpand ? ' auto-exapnd ' : ''
    ]);

    inject(function($compile, $rootScope) {
      group = $compile(template)($rootScope.$new());
    });

    document.body.appendChild(group[0]);

    group.scope().$digest();
    return group;
  }


  function expandPanel() {
    panel.find('md-expansion-panel-collapsed').triggerHandler('click');
    panel.scope().$digest();
  }

  function collapsePanel() {
    panel.scope().$panel.collapse();
    panel.scope().$digest();
  }

  function focusPanel() {
    panel.focus();
    panel.scope().$digest();
  }

  function pressKey(keycode) {
    panel.triggerHandler({
      type: 'keydown',
      keyCode: keycode
    });
    panel.scope().$digest();
  }


  function flushAnimations() {
    $animate.flush();
    $timeout.flush();
  }

});
