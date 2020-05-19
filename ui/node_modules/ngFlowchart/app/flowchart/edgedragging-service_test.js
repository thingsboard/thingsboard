'use strict';

describe('edgedragging-service_test', function() {

  beforeEach(function() {
    module('flowchart', function($provide) {
      $provide.service('Edgedrawingservice', function() {
        this.test = 'test';
      });
      $provide.service('Modelvalidation', function() {
        this.validateEdges = jasmine.createSpy('validateEdges');
        this.ModelvalidationError = function(){};
        this.ModelvalidationError.prototype = Object.create(Error.prototype);
        this.ModelvalidationError.prototype.constructor = this.ModelvalidationError;
      });
    });
    module('flowchart');
  });

  function createEvent(name, clientX, clientY) {
    var event = jasmine.createSpyObj(name, ['stopPropagation', 'preventDefault']);
    event.target = angular.element('<div></div>')[0];
    event.dataTransfer = jasmine.createSpyObj('datatransfer', ['setDragImage', 'setData']);
    event.clientX = clientX;
    event.clientY = clientY;
    return event;
  }

  beforeEach(inject(function(Edgedraggingfactory, flowchartConstants, Modelvalidation, Edgedrawingservice) {
    this.Modelvalidation = Modelvalidation;
    this.canvasCoords = {top: 200, left: 200};
    this.canvasElement = jasmine.createSpyObj('canvasElement', ['getBoundingClientRect']);
    this.canvasElement.getBoundingClientRect.and.returnValue(angular.copy(this.canvasCoords));

    this.dragAnimation = flowchartConstants.dragAnimationRepaint;
    this.edgeStyle = flowchartConstants.lineStyle;

    this.connector = {id: 1, type: flowchartConstants.rightConnectorType};
    this.destinationConnector = {id: 2, type: flowchartConstants.rightConnectorType};
    this.connectorCoords = {x: 100, y: 100};

    this.modelservice = jasmine.createSpyObj('modelservice', ['getCanvasHtmlElement']);
    this.modelservice.getCanvasHtmlElement.and.returnValue(this.canvasElement);
    this.modelservice.connectors = jasmine.createSpyObj('modelservice.connectors', ['getCenteredCoord']);
    this.modelservice.connectors.getCenteredCoord.and.returnValue(angular.copy(this.connectorCoords));
    this.modelservice.edges = {};
    this.modelservice.edges._addEdge = jasmine.createSpy('_addEdge');

    this.edgeDragging = {};
    this.userIsValidEdgeCallback = jasmine.createSpy('isValidEdge').and.returnValue(true);
    this.applyFunction = jasmine.createSpy('apply').and.callFake(function(f) {
      return f()
    });
    this.edgedraggingService = Edgedraggingfactory(this.modelservice, {nodes: [], edges: []}, this.edgeDragging, this.userIsValidEdgeCallback, this.applyFunction, this.dragAnimation, this.edgeStyle);

    this.startEvent = createEvent('startEvent', this.canvasCoords.left + this.connectorCoords.x, this.canvasCoords.top + this.connectorCoords.y);
    this.dragDistance = 20;
    this.overEvent = createEvent('overEvent', this.canvasCoords.left + this.connectorCoords.x + this.dragDistance, this.canvasCoords.top + this.connectorCoords.y + this.dragDistance);
    this.endEvent = createEvent('endEvent', this.canvasCoords.left + this.connectorCoords.x + this.dragDistance, this.canvasCoords.top + this.connectorCoords.y + this.dragDistance);
    this.dropEvent = createEvent('dropEvent', this.canvasCoords.left + this.connectorCoords.x + this.dragDistance, this.canvasCoords.top + this.connectorCoords.y + this.dragDistance);
  }));

  it('should initialize the edgeDragging', function() {
    expect(this.edgeDragging.isDragging).toBe(false);
    expect(this.edgeDragging.dragPoint1).toBeNull();
    expect(this.edgeDragging.dragPoint2).toBeNull();
  });

  it('dragstart should initialize the scope, set an invisible dragimage, call setData and call stopPropagation.', function() {
    this.edgedraggingService.dragstart(angular.copy(this.connector))(this.startEvent);
    expect(this.edgeDragging.isDragging).toBe(true);
    expect(this.edgeDragging.dragPoint1).toEqual(this.connectorCoords);
    expect(this.edgeDragging.dragPoint2).toEqual(this.connectorCoords);
    //expect(this.startEvent.dataTransfer.setDragImage).toHaveBeenCalled();
    expect(this.startEvent.dataTransfer.setData).toHaveBeenCalled();
    expect(this.startEvent.stopPropagation).toHaveBeenCalled();
  });

  it('dragover should update the scope', function() {
    this.edgedraggingService.dragstart(angular.copy(this.connector))(this.startEvent);
    this.edgedraggingService.dragover(this.overEvent);
    expect(this.edgeDragging.isDragging).toBe(true);
    expect(this.edgeDragging.dragPoint1).toEqual(this.connectorCoords);
    expect(this.edgeDragging.dragPoint2).toEqual({
      x: this.connectorCoords.x + this.dragDistance,
      y: this.connectorCoords.y + this.dragDistance
    });
  });

  it('dragover connector should call preventDefault and stopPropagation', function() {
    this.edgedraggingService.dragstart(angular.copy(this.connector))(this.startEvent);

    this.userIsValidEdgeCallback.and.returnValue(false);
    this.edgedraggingService.dragoverConnector(this.connector)(this.overEvent);
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.edgeDragging.dragPoint2).toEqual({x: this.connectorCoords.x + this.dragDistance, y: this.connectorCoords.y + this.dragDistance});

    this.userIsValidEdgeCallback.and.returnValue(true);
    expect(this.edgedraggingService.dragoverConnector(this.connector)(this.overEvent)).toBe(false);
    expect(this.overEvent.stopPropagation).toHaveBeenCalled();
    expect(this.overEvent.preventDefault).toHaveBeenCalled();
    expect(this.edgeDragging.dragPoint2).toEqual({x: this.connectorCoords.x + this.dragDistance, y: this.connectorCoords.y + this.dragDistance});
  });

  it('dragover magnet should call preventDefault and stopPropagation, it should perform user validation.', function() {
    this.edgedraggingService.dragstart(angular.copy(this.connector))(this.startEvent);

    this.userIsValidEdgeCallback.and.returnValue(false);
    this.edgedraggingService.dragoverMagnet(this.connector)(this.overEvent);
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.edgeDragging.dragPoint2).toEqual({x: this.connectorCoords.x + this.dragDistance, y: this.connectorCoords.y + this.dragDistance});

    this.userIsValidEdgeCallback.and.returnValue(true);
    expect(this.edgedraggingService.dragoverMagnet(this.destinationConnector)(this.overEvent)).toBe(false);
    expect(this.applyFunction).toHaveBeenCalled();
    expect(this.overEvent.stopPropagation).toHaveBeenCalled();
    expect(this.overEvent.preventDefault).toHaveBeenCalled();
    expect(this.edgeDragging.dragPoint2).toEqual(this.connectorCoords);
  });

  it('dragend should reset the scope and call stopPropagation', function() {
    this.edgedraggingService.dragstart(angular.copy(this.connector))(this.startEvent);
    this.edgedraggingService.dragend(this.endEvent);

    expect(this.edgeDragging.isDragging).toBe(false);
    expect(this.edgeDragging.dragPoint1).toBeNull();
    expect(this.edgeDragging.dragPoint2).toBeNull();
    expect(this.endEvent.stopPropagation).toHaveBeenCalled();
  });

  it('drop should add a new edge, if sourceconnector and targetconnector differ. Also preventDefault and stopPropagation must be called', function() {
    this.edgedraggingService.dragstart(angular.copy(this.connector))(this.startEvent);

    this.userIsValidEdgeCallback.and.returnValue(false);
    this.edgedraggingService.drop(this.destinationConnector)(this.dropEvent);
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.modelservice.edges._addEdge).not.toHaveBeenCalled();

    this.userIsValidEdgeCallback.and.returnValue(true);
    expect(this.edgedraggingService.drop(this.destinationConnector)(this.dropEvent)).toBe(false);

    expect(this.modelservice.edges._addEdge).toHaveBeenCalledWith(this.connector, this.destinationConnector);
    expect(this.dropEvent.stopPropagation).toHaveBeenCalled();
    expect(this.dropEvent.preventDefault).toHaveBeenCalled();
  });

  it('should fix the internet explorer setDragImage bug', function() {
    this.startEvent.dataTransfer.setDragImage = null;
    this.edgedraggingService.dragstart(this.connector)(this.startEvent);

    expect(this.startEvent.target.style.display).toEqual('none');

    this.edgedraggingService.dragover(this.overEvent);
    expect(this.startEvent.target.style.display).toEqual('');
  });

  it('dragover and drop should perform modelvalidation', function() {
    var that = this;

    this.startEvent.dataTransfer.setDragImage = null;
    this.edgedraggingService.dragstart(this.connector)(this.startEvent);

    this.Modelvalidation.validateEdges.and.throwError(new this.Modelvalidation.ModelvalidationError());
    expect(this.edgedraggingService.drop(this.destinationConnector)(this.dropEvent)).toBe(true);
    expect(this.dropEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.dropEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.modelservice.edges._addEdge).not.toHaveBeenCalled();

    expect(this.edgedraggingService.dragoverConnector(this.destinationConnector)(this.overEvent)).toBe(true);
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();

    expect(this.edgedraggingService.dragoverMagnet(this.destinationConnector)(this.overEvent)).toBe(true);
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.edgeDragging.dragPoint2).toEqual({x: this.connectorCoords.x + this.dragDistance, y: this.connectorCoords.y + this.dragDistance});


    this.Modelvalidation.validateEdges.and.throwError(new Error('Test'));
    expect(function() {that.edgedraggingService.drop(that.destinationConnector)(that.dropEvent);}).toThrowError('Test');
    expect(this.dropEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.dropEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.modelservice.edges._addEdge).not.toHaveBeenCalled();

    expect(function() {that.edgedraggingService.dragoverConnector(that.destinationConnector)(that.overEvent);}).toThrowError('Test');
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();

    expect(function() {that.edgedraggingService.dragoverMagnet(that.destinationConnector)(that.overEvent);}).toThrowError('Test');
    expect(this.overEvent.preventDefault).not.toHaveBeenCalled();
    expect(this.overEvent.stopPropagation).not.toHaveBeenCalled();
    expect(this.edgeDragging.dragPoint2).toEqual({x: this.connectorCoords.x + this.dragDistance, y: this.connectorCoords.y + this.dragDistance});
  });

});
