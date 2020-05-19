describe('vAccordionController', function () {

  var scope;
  var accordion;
  var isolateScope;
  var controller;

  var generateTemplate = function (options) {
    var dafaults = {
      attributes: '',
      content: ''
    };

    if (options) {
      angular.extend(dafaults, options);
    }

    var template = '<v-accordion ' + dafaults.attributes + '>\n';
        template += dafaults.content + '\n';
        template += '</v-accordion>';

    return template;
  };

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


  beforeEach(module('vAccordion'));

  beforeEach(inject(function (_$rootScope_, _$compile_) {
    scope = _$rootScope_.$new();

    var options = { attributes: 'multiple control="control" onexpand="onExpand(index)" oncollapse="onCollapse(index)"' };
    var template = generateTemplate(options);

    accordion = _$compile_(template)(scope);
    _$rootScope_.$digest();

    isolateScope = accordion.isolateScope();
    controller = accordion.controller('vAccordion');
  }));

  afterEach(function () {
    scope.$destroy();
  });


  it('should add new pane object to `panes` array', function () {
    var samplePane = generatePanes(1)[0];

    expect(isolateScope.panes.length).toBe(0);
    controller.addPane(samplePane);
    expect(isolateScope.panes.length).toBeGreaterThan(0);
  });


  it('should expand pane and call `onExpand` callback', function () {
    var samplePanes = generatePanes(5);
    var paneToExpandIndex = 0;
    var paneToExpand = samplePanes[paneToExpandIndex];

    for (var i = 0; i < samplePanes.length; i++) {
      controller.addPane(samplePanes[i]);
    }

    scope.onExpand = jasmine.createSpy('onExpand');
    scope.$digest();

    expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeFalsy();
    controller.expand(paneToExpand);
    expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeTruthy();

    expect(scope.onExpand).toHaveBeenCalled();
  });


  it('should collapse pane and call `onCollapse` callback', function () {
    var samplePanes = generatePanes(5);
    var paneToExpandIndex = 0;
    var paneToExpand = samplePanes[paneToExpandIndex];
        paneToExpand.isExpanded = true;

    for (var i = 0; i < samplePanes.length; i++) {
      controller.addPane(samplePanes[i]);
    }

    scope.onCollapse = jasmine.createSpy('onCollapse');
    scope.$digest();

    expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeTruthy();
    controller.collapse(paneToExpand);
    expect(isolateScope.panes[paneToExpandIndex].isExpanded).toBeFalsy();

    expect(scope.onCollapse).toHaveBeenCalled();
  });

});
