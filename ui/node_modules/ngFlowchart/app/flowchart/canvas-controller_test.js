'use strict';

describe('test canvas-controller', function() {
  var $controller;
  var $rootScope;

  beforeEach(module('flowchart'));

  beforeEach(inject(function(_$controller_, _$rootScope_) {
    $rootScope = _$rootScope_;
    this.$scope = $rootScope.$new();
    this.Mouseoverfactory = jasmine.createSpy('mouseoverfactory').and.returnValue(jasmine.createSpyObj('mouseoverservice', ['nodeMouseOver', 'nodeMouseOut', 'connectorMouseEnter',
      'connectorMouseLeave', 'edgeMouseEnter', 'edgeMouseLeave']));
    this.Nodedraggingfactory = jasmine.createSpy('Nodedraggingfactory').and.returnValue(jasmine.createSpyObj('nodedragging', ['drop', 'dragstart', 'dragend', 'dragover']));
    this.Modelfactory = jasmine.createSpy('Modelfactory').and.returnValue(jasmine.createSpyObj('modelservice', ['deselectAll']));
    this.Edgedraggingfactory = jasmine.createSpy('Edgedraggingfactory').and.returnValue(jasmine.createSpyObj('edgeDraggingservice', ['dragstart', 'drop', 'dragover', 'dragoverConnector', 'dragend', 'dragoverMagnet']));
    this.edgeDrawingService = jasmine.createSpy('edgeDrawingService');

    $controller = _$controller_;
    this.controller = $controller('canvasController', {
      $scope: this.$scope,
      Mouseoverfactory: this.Mouseoverfactory,
      Nodedraggingfactory: this.Nodedraggingfactory,
      Modelfactory: this.Modelfactory,
      edgeDraggingFactory: this.edgeDraggingFactory,
      edgeDrawingService: this.edgeDrawingService
    });
  }));

  it('should define all the scope variables', function() {
    expect(this.$scope.modelservice).toBeDefined();
    expect(this.$scope.nodeDragging).toBeDefined();
    expect(this.$scope.edgeDragging).toBeDefined();
    expect(this.$scope.mouseOver).toBeDefined();
    expect(this.$scope.canvasClick).toEqual(jasmine.any(Function));
    expect(this.$scope.edgeMouseEnter).toEqual(jasmine.any(Function));
    expect(this.$scope.edgeMouseLeave).toEqual(jasmine.any(Function));
    expect(this.$scope.drop).toEqual(jasmine.any(Function));
    expect(this.$scope.dragover).toEqual(jasmine.any(Function));
    expect(this.$scope.edgeClick).toEqual(jasmine.any(Function));
    expect(this.$scope.edgeDoubleClick).toEqual(jasmine.any(Function));
    expect(this.$scope.edgeMouseOver).toEqual(jasmine.any(Function));
    expect(this.$scope.getEdgeDAttribute).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.nodeDragstart).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.nodeDragend).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.edgeDragstart).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.edgeDragend).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.edgeDrop).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.edgeDragoverConnector).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.edgeDragoverMagnet).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.nodeClicked).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.nodeMouseOver).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.nodeMouseOut).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.connectorMouseEnter).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.connectorMouseLeave).toEqual(jasmine.any(Function));
    expect(this.$scope.callbacks.nodeClicked()).toEqual(jasmine.any(Function)); // Should be of type function(node) {return function(event){};}
  });

  it('should set $scope.userCallbacks if not given and control if they are all functionsexcept the nodeCallbacks', function() {
    var that = this;
    expect(this.$scope.userCallbacks).toBeDefined();

    var userCallbacks = {edgeDoubleClick: function() {}, nodeCallbacks: {test: 'test'}};
    this.$scope.userCallbacks = angular.copy(userCallbacks);
    this.controller = $controller('canvasController', {
      $scope: this.$scope,
      Mouseoverfactory: this.Mouseoverfactory,
      Nodedraggingfactory: this.Nodedraggingfactory,
      Modelfactory: this.Modelfactory,
      edgeDraggingFactory: this.edgeDraggingFactory,
      edgeDrawingService: this.edgeDrawingService
    });
    expect(this.$scope.userCallbacks).toEqual(userCallbacks);
    expect(this.$scope.userNodeCallbacks).toEqual(userCallbacks.nodeCallbacks);

    this.$scope.userCallbacks.isValidEdge = {};
    expect(function() { $controller('canvasController', {
      $scope: that.$scope,
      Mouseoverfactory: that.Mouseoverfactory,
      Nodedraggingfactory: that.Nodedraggingfactory,
      Modelfactory: that.Modelfactory,
      edgeDraggingFactory: that.edgeDraggingFactory,
      edgeDrawingService: that.edgeDrawingService
    });}).toThrowError('All callbacks should be functions.')

  });

  it('should give the edgeAddedCallback to the modelservice', function() {
    expect(this.Modelfactory).toHaveBeenCalledWith(undefined, undefined, angular.noop, angular.noop, angular.noop);

    var edgeAddedCallback = jasmine.createSpy('edgeAddedCallback');
    var edgeRemovedCallback = jasmine.createSpy('edgeRemovedCallback')
    var nodeRemovedCallback = jasmine.createSpy('nodeRemovedCallback')
    var userCallbacks = {edgeAdded: edgeAddedCallback, edgeRemoved: edgeRemovedCallback, nodeRemoved: nodeRemovedCallback};
    this.$scope.userCallbacks = angular.copy(userCallbacks);
    this.controller = $controller('canvasController', {
      $scope: this.$scope,
      Mouseoverfactory: this.Mouseoverfactory,
      Nodedraggingfactory: this.Nodedraggingfactory,
      Modelfactory: this.Modelfactory,
      edgeDraggingFactory: this.edgeDraggingFactory,
      edgeDrawingService: this.edgeDrawingService
    });
    expect(this.Modelfactory.calls.argsFor(1)).toEqual([undefined, undefined, edgeAddedCallback, nodeRemovedCallback, edgeRemovedCallback]);


  });
});
