'use strict';

describe('the canvas-directive, node drawing is not tested at all to separate from node- and connector-directive', function() {
  var EDGE_SOURCE = {x: 0, y: 0};
  var EDGE_DESTINATION = {x: 100, y: 100};
  var EDGE_SOURCE_TANGENT = {x: 10, y: 10};
  var EDGE_DESTINATION_TANGENT = {x: 20, y: 20};

  var $compile;
  var $rootScope;
  var controllerScope;
  var flowchartConstants;
  var modelservice;

  beforeEach(function() {
    module('flowchart', function($provide, $controllerProvider) {
      $provide.service('modelservice', function() {
        this.setCanvasHtmlElement = jasmine.createSpy('modelservice.setCanvasHtmlElement');
        this.setSvgHtmlElement = jasmine.createSpy('modelservice.setSvgHtmlElement');

        this.edges = jasmine.createSpyObj('modelservice edges', ['sourceCoord', 'destCoord', 'isSelected']);
        this.edges.sourceCoord.and.returnValue(EDGE_SOURCE);
        this.edges.destCoord.and.returnValue(EDGE_DESTINATION);
        this.edges.isSelected.and.returnValue(false);
      });
      $controllerProvider.register('canvasController', function($scope, modelservice) {
        controllerScope = $scope;

        $scope.modelservice = modelservice;

        $scope.mouseOver = {};
        $scope.mouseOver.edge = null;

        $scope.edgeDragging = {};

        $scope.edgeClick = jasmine.createSpy('edgeClick listener');
        $scope.edgeMouseEnter = jasmine.createSpy('edgeMouseEnter listener');
        $scope.edgeMouseLeave = jasmine.createSpy('edgeMouseLeave listener');
        $scope.edgeDoubleClick= jasmine.createSpy('edgeDoubleClick listener');
        $scope.edgeMouseOver = jasmine.createSpy('edgeMouseOver listener');

        $scope.getEdgeDAttribute = jasmine.createSpy('getEdgeDAttribute').and.returnValue('No calculation.');
        $scope.getSourceTangentX = jasmine.createSpy().and.returnValue(EDGE_SOURCE_TANGENT.x);
        $scope.getSourceTangentY = jasmine.createSpy().and.returnValue(EDGE_SOURCE_TANGENT.y);
        $scope.getDestTangentX = jasmine.createSpy().and.returnValue(EDGE_DESTINATION_TANGENT.x);
        $scope.getDestTangentY = jasmine.createSpy().and.returnValue(EDGE_DESTINATION_TANGENT.y);

        $scope.dragover = jasmine.createSpy('dragover listener');
        $scope.drop = jasmine.createSpy('drop listener');
        $scope.canvasMouseMove = jasmine.createSpy('canvasMouseMove listener');
        $scope.canvasClick = jasmine.createSpy('canvasClick listener');

      });
    });

    module('flowchart');
  });

  function compileCanvas(scope, style) {
    var canvas = $compile('<fc-canvas model="model" selected-objects="selectedObjects" edge-style="' + style + '"></flowchart-canvas>')(scope);
    scope.$digest();
    return canvas;
  }

  beforeEach(inject(function(_$compile_, _$rootScope_, _flowchartConstants_, _modelservice_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    flowchartConstants = _flowchartConstants_;
    modelservice = _modelservice_;

    this.outerScope = $rootScope.$new();
    this.outerScope.selectedObjects = [];
    this.outerScope.model = {nodes: [], edges: []};

    this.canvas = compileCanvas(this.outerScope, flowchartConstants.lineStyle);
  }));

  it('should add the flowchart-canvas class', function() {
    expect(this.canvas.hasClass(flowchartConstants.canvasClass)).toBe(true);
  });

  it('should validate the edgestyle', function() {
    compileCanvas(this.outerScope, flowchartConstants.lineStyle);
    compileCanvas(this.outerScope, flowchartConstants.curvedStyle);

    this.canvas = $compile('<flowchart-canvas model="model" selected-objects="selectedObjects" edge-style="' + 'unsupportedstyle' + '"></flowchart-canvas>')(this.outerScope);
    expect(this.outerScope.$digest).toThrow();
  });

  it('should add a dragoverlistener', function() {
    expect(controllerScope.dragover).not.toHaveBeenCalled();
    this.canvas.triggerHandler('dragover');
    expect(controllerScope.dragover).toHaveBeenCalled();
  });

  it('should add a droplistener', function() {
    expect(controllerScope.drop).not.toHaveBeenCalled();
    this.canvas.triggerHandler('drop');
    expect(controllerScope.drop).toHaveBeenCalled();
  });

  it('should add a mouseclick listener', function() {
    expect(controllerScope.canvasClick).not.toHaveBeenCalled();
    this.canvas.triggerHandler('click');
    expect(controllerScope.canvasClick).toHaveBeenCalled();
  });

  it('should set the html element to the modelservice', function() {
    expect(modelservice.setCanvasHtmlElement).toHaveBeenCalledWith(this.canvas[0]);
  });

  describe('test for edgedrawing', function() {
    it('should draw edge which are dragged', function() {
      controllerScope.edgeDragging.isDragging = true;
      controllerScope.edgeDragging.dragPoint1 = {x: 0, y: 0};
      controllerScope.edgeDragging.dragPoint2 = {x: 100, y: 100};
      controllerScope.edgeDragging.dragTangent1 = {x: 10, y: 10};
      controllerScope.edgeDragging.dragTangent2 = {x: 20, y: 20};

      controllerScope.$apply();

      var draggedEdge = this.canvas.find('path');
      expect(draggedEdge.length).toEqual(1);
      expect(draggedEdge.hasClass(flowchartConstants.edgeClass)).toBe(true);
      expect(draggedEdge.hasClass(flowchartConstants.draggingClass)).toBe(true);

      // No dragged edge if no dragging.
      controllerScope.edgeDragging.isDragging = false;
      controllerScope.$apply();
      expect(this.canvas.find('path').length).toEqual(0);
    });

    it('should draw the edges from the model', function() {
      expect(this.canvas.find('path').length).toEqual(0);

      controllerScope.model = {nodes: [], edges: [{'source': 1, 'destination': 2}]};
      controllerScope.$apply();
      var edge = this.canvas.find('path');
      expect(edge.length).toEqual(1);


      controllerScope.model = {nodes: [], edges: [{'source': 1, 'destination': 2}, {'source': 1, 'destination': 2}]};
      controllerScope.$apply();
      edge = this.canvas.find('path');
      expect(edge.length).toEqual(2);
    });

    it('should set selected classes for the edges', function() {
      controllerScope.model = {nodes: [], edges: [{'source': 1, 'destination': 2}]};
      controllerScope.$apply();
      expect(this.canvas.find('path').length).toEqual(1); // Edges were drawn?
      expect(this.canvas.find('path').hasClass(flowchartConstants.selectedClass)).toBe(false);

      controllerScope.modelservice.edges.isSelected.and.returnValue(true);
      controllerScope.$apply();
      expect(this.canvas.find('path').hasClass(flowchartConstants.selectedClass)).toBe(true);
    });

    it('should set hover classes for the edges', function() {
      controllerScope.model = {nodes: [], edges: [{'source': 1, 'destination': 2}, {'source': 1, 'destination': 2}]};
      controllerScope.mouseOver.edge = controllerScope.model.edges[0];
      controllerScope.$apply();
      expect(this.canvas.find('path').length).toEqual(2); // Edges were drawn?
      expect(this.canvas.find('path').hasClass(flowchartConstants.hoverClass)).toBe(true);

      controllerScope.mouseOver.edge = controllerScope.model.edges[1]; // .hasClass uses the first found edge.
      controllerScope.$apply();
      expect(this.canvas.find('path').hasClass(flowchartConstants.hoverClass)).toBe(false);
    });

    it('should register a click, mouseenter, mouseleave listener for the edges', function() {
      controllerScope.model = {nodes: [], edges: [{'source': 1, 'destination': 2}, {'source': 1, 'destination': 2}]};
      controllerScope.$apply();

      expect(controllerScope.edgeClick).not.toHaveBeenCalled();
      this.canvas.find('path').triggerHandler('click');
      expect(controllerScope.edgeClick).toHaveBeenCalled();

      expect(controllerScope.edgeMouseEnter).not.toHaveBeenCalled();
      this.canvas.find('path').triggerHandler('mouseenter');
      expect(controllerScope.edgeMouseEnter).toHaveBeenCalled();

      expect(controllerScope.edgeMouseLeave).not.toHaveBeenCalled();
      this.canvas.find('path').triggerHandler('mouseleave');
      expect(controllerScope.edgeMouseLeave).toHaveBeenCalled();
    });

    it('should add a doubleclick and a hoverlistener', function() {
      controllerScope.model = {nodes: [], edges: [{'source': 1, 'destination': 2}, {'source': 1, 'destination': 2}]};
      controllerScope.$apply();

      expect(controllerScope.edgeDoubleClick).not.toHaveBeenCalled();
      this.canvas.find('path').triggerHandler('dblclick');
      expect(controllerScope.edgeDoubleClick).toHaveBeenCalled();

      expect(controllerScope.edgeMouseOver).not.toHaveBeenCalled();
      this.canvas.find('path').triggerHandler('mouseover');
      expect(controllerScope.edgeMouseOver).toHaveBeenCalled();
    });
  });

});
