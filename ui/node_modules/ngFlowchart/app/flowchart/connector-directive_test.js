'use strict';

describe('test for connector directive', function() {
  var $compile;
  var $rootScope;
  var modelService;
  var flowchartConstants;

  var connector = {
    type: 'leftConnector',
    id: 1
  };


  beforeEach(function() {
    module(function($provide) {
      $provide.service('modelService', function() {
        this.connectors = {};
        this.connectors.setHtmlElement = jasmine.createSpy('setHtmlElement');
      })
    });
    module('flowchart');
  });

  beforeEach(inject(function(_$compile_, _$rootScope_, _modelService_, _flowchartConstants_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    modelService = _modelService_;
    flowchartConstants = _flowchartConstants_;

    $rootScope.connector = connector;
    $rootScope.mouseOverConnector = null;
    $rootScope.fcCallbacks = jasmine.createSpyObj('callbacks', ['edgeDragend', 'edgeDragoverConnector', 'connectorMouseEnter', 'connectorMouseLeave']);
    $rootScope.fcCallbacks.connectorMouseEnter.and.returnValue(function(event) {
    });
    $rootScope.fcCallbacks.connectorMouseLeave.and.returnValue(function(event) {
    });

    this.innerDragStart = jasmine.createSpy('innerDragStart');
    $rootScope.fcCallbacks.edgeDragstart = jasmine.createSpy('edgeDragstart').and.returnValue(this.innerDragStart);

    this.innerDrop = jasmine.createSpy('innerDrop');
    $rootScope.fcCallbacks.edgeDrop = jasmine.createSpy('edgeDrop').and.returnValue(this.innerDrop);

    this.innerMouseEnter = jasmine.createSpy('innerMouseEnter');
    $rootScope.fcCallbacks.connectorMouseEnter.and.returnValue(this.innerMouseEnter);

    this.innerMouseLeave = jasmine.createSpy('innerMouseLeave');
    $rootScope.fcCallbacks.connectorMouseLeave.and.returnValue(this.innerMouseLeave);

    $rootScope.modelservice = modelService;
  }));

  function getCompiledConnector() {
    var connector = $compile('<div fc-connector></div>')($rootScope);
    $rootScope.$digest();
    return connector;
  }

  it('should be draggable', function() {
    var connector = getCompiledConnector();
    expect(connector.attr('draggable')).toBe('true');
  });

  it('should have a hovered class if hovered', function() {
    $rootScope.mouseOverConnector = connector;
    var htmlConnector = getCompiledConnector();
    expect(htmlConnector.hasClass(flowchartConstants.hoverClass)).toBe(true);

    $rootScope.mouseOverConnector = null;
    $rootScope.$apply();
    expect(htmlConnector.hasClass(flowchartConstants.hoverClass)).toBe(false);
  });

  it('should store the connector html elements', function() {
    getCompiledConnector();
    expect(modelService.connectors.setHtmlElement.calls.count()).toBe(1);
    expect(modelService.connectors.setHtmlElement).toHaveBeenCalledWith(connector.id, jasmine.any(Object));
  });

  it('should register the dragstart, dragend, drop, mouseenter, mouseleave and dragover event', function() {
    var htmlConnector = getCompiledConnector();

    expect(this.innerDragStart).not.toHaveBeenCalled();
    htmlConnector.triggerHandler('dragstart');
    expect(this.innerDragStart).toHaveBeenCalled();

    expect(this.innerDrop).not.toHaveBeenCalled();
    htmlConnector.triggerHandler('drop');
    expect(this.innerDrop).toHaveBeenCalled();

    expect($rootScope.fcCallbacks.edgeDragend).not.toHaveBeenCalled();
    htmlConnector.triggerHandler('dragend');
    expect($rootScope.fcCallbacks.edgeDragend).toHaveBeenCalled();

    expect($rootScope.fcCallbacks.edgeDragoverConnector).not.toHaveBeenCalled();
    htmlConnector.triggerHandler('dragover');
    expect($rootScope.fcCallbacks.edgeDragoverConnector).toHaveBeenCalled();

    expect(this.innerMouseEnter).not.toHaveBeenCalled();
    htmlConnector.triggerHandler('mouseenter');
    expect(this.innerMouseEnter).toHaveBeenCalled();

    expect(this.innerMouseLeave).not.toHaveBeenCalled();
    htmlConnector.triggerHandler('mouseleave');
    expect(this.innerMouseLeave).toHaveBeenCalled();
  });
});
