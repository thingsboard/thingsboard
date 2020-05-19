describe('vPaneController', function () {

  var scope;
  var pane;
  var isolateScope;
  var controller;

  var generatePanes = function (length) {
    var samplePanes = [];

    for (var i = 0; i < length; i++) {
      var samplePane = {
        header: 'Pane header #' + i,
        content: 'Pane content #' + i
      };

      samplePanes.push(samplePane);
    }

    return samplePanes;
  };

  var generateTemplate = function (options) {
    var dafaults = {
      content: '',
      accordionAttributes: '',
      paneAttributes: ''
    };

    if (options) {
      angular.extend(dafaults, options);
    }

    var template = '<v-accordion ' + dafaults.accordionAttributes + '>\n';
        template += '<v-pane ' + dafaults.paneAttributes + '>\n';
        template += '<v-pane-header></v-pane-header>\n';
        template += '<v-pane-content>' + dafaults.content + '</v-pane-content>\n';
        template += '</v-pane>\n';
        template += '</v-accordion>';

    return template;
  };


  beforeEach(module('vAccordion'));

  beforeEach(inject(function (_$rootScope_, _$compile_) {
    scope = _$rootScope_.$new();

    var options = { accordionAttributes: 'multiple' };
    var template = generateTemplate(options);

    var accordion = _$compile_(template)(scope);
    pane = accordion.find('v-pane');
    _$rootScope_.$digest();

    isolateScope = pane.isolateScope();
    controller = pane.controller('vPane');
  }));

  afterEach(function () {
    scope.$destroy();
  });


  it('should change isExpanded scope value using `expand()`, `collapse()` and `toggle()` methods', function () {
    expect(controller.isExpanded()).toBe(false);
    controller.expand();
    expect(controller.isExpanded()).toBe(true);
    controller.collapse();
    expect(controller.isExpanded()).toBe(false);
    controller.toggle();
    expect(controller.isExpanded()).toBe(true);
  });


  it('should change isFocused scope value using `focusPane()` and `blurPane()` methods', function () {
    expect(isolateScope.isFocused).toBeFalsy();
    controller.focusPane();
    expect(isolateScope.isFocused).toBe(true);
    controller.blurPane();
    expect(isolateScope.isFocused).toBe(false);
  });

});
