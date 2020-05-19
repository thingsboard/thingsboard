'use strict';

describe('test the nodedragging service', function() {

  var CANVAS_LENGTH = 1000;
  var $rootScope;
  var $document;
  var nodedragging;
  var modelservice;


  beforeEach(function() {
    module(function($provide) {
      $provide.service('modelservice', function() {
        this.nodes = {};
        this.nodes.select = jasmine.createSpy('modelservice.nodes.select');
        this.deselectAll = jasmine.createSpy('modelservice.deselectAll');
        var canvasHtmlElement = jasmine.createSpyObj('canvasHtmlElement', ['test']);
        canvasHtmlElement.offsetHeight = CANVAS_LENGTH;
        canvasHtmlElement.offsetWidth = CANVAS_LENGTH;
        this.getCanvasHtmlElement = jasmine.createSpy('modelservice.getCanvasHtmlElement').and.returnValue(canvasHtmlElement);
      });
    });
    module('flowchart');
  });

  beforeEach(inject(function(_$document_, _modelservice_, _$rootScope_, Nodedraggingfactory) {
    $rootScope = _$rootScope_;
    modelservice = _modelservice_;
    $document = _$document_;

    this.$scope = $rootScope.$new();

    this.node = {
      id: 1,
      x: 0,
      y: 0,
      name: 'testnode'
    };

    this.automaticResize = false;
    this.dragAnimation = 'repaint';

    this.fakeEvent = {
      preventDefault: jasmine.createSpy('preventDefault'),
      target: angular.element('<div style="top: 100px; left: 100px; display: initial;"></div>')[0],
      dataTransfer: {
        setData: jasmine.createSpy('setData'),
        setDragImage: jasmine.createSpy('setDragImage')
      },
      clientX: 0,
      clientY: 0
    };

    this.$scope.draggingNode = {};
    nodedragging = Nodedraggingfactory(modelservice, this.$scope.draggingNode, this.$scope.$apply.bind(this.$scope), this.automaticResize, this.dragAnimation);
  }));

  it('dragstart should select the node witch is dragged and deselect all others', function() {
    var innerDragStart = nodedragging.dragstart(this.node);
    innerDragStart(this.fakeEvent);

    expect(modelservice.deselectAll).toHaveBeenCalled();
    expect(modelservice.nodes.select).toHaveBeenCalledWith(this.node);
  });

  it('dragstart should set $scope.nodeDragging.draggedNode', function() {
    this.$scope.draggingNode.draggedNode = null;

    var innerDragStart = nodedragging.dragstart(this.node);
    innerDragStart(this.fakeEvent);

    expect(this.$scope.draggingNode.draggedNode).toBe(this.node);
  });

  it('dragstart should call setData to support firefox', function() {

    var innerDragStart = nodedragging.dragstart(this.node);
    innerDragStart(this.fakeEvent);

    expect(this.fakeEvent.dataTransfer.setData).toHaveBeenCalled();
  });

  it('should drop the defaultNode under the mousepointer and prevent default', function() {
    var clientX = 100;
    var clientY = 100;
    this.fakeEvent.clientX = clientX;
    this.fakeEvent.clientY = clientY;

    nodedragging.dragstart(this.node)(this.fakeEvent);
    expect(nodedragging.drop(this.fakeEvent)).toBe(false);
    expect(this.fakeEvent.preventDefault).toHaveBeenCalled();

    expect(this.node.x).toEqual(clientX);
    expect(this.node.y).toEqual(clientY);
  });

  it('dragover should preventdefault, update node coordinates and prevent dragging outside of the canvas', function() {
    var clientX = 100;
    var clientY = 100;
    this.fakeEvent.clientX = clientX;
    this.fakeEvent.clientY = clientY;

    nodedragging.dragstart(this.node)(this.fakeEvent);
    expect(nodedragging.dragover(this.fakeEvent)).toBe(false);
    expect(this.fakeEvent.preventDefault).toHaveBeenCalled();

    expect(this.node.x).toEqual(clientX);
    expect(this.node.y).toEqual(clientY);

    clientX = -2;
    clientY = -2;
    this.fakeEvent.clientX = clientX;
    this.fakeEvent.clientY = clientY;
    expect(nodedragging.dragover(this.fakeEvent)).toBe(false);
    expect(this.node.x).toEqual(0);
    expect(this.node.y).toEqual(0);

    clientX = CANVAS_LENGTH + 1;
    clientY = CANVAS_LENGTH + 1;
    this.fakeEvent.clientX = clientX;
    this.fakeEvent.clientY = clientY;
    expect(nodedragging.dragover(this.fakeEvent)).toBe(false);
    expect(this.node.x).toEqual(CANVAS_LENGTH);
    expect(this.node.y).toEqual(CANVAS_LENGTH);
  });

  it('dragover should prevent dragging outside of the canvas', function() {

  });

  it('should reset all variables when dragging ends.', function() {
    nodedragging.dragstart(this.node)(this.fakeEvent);
    nodedragging.dragend(this.fakeEvent);

    expect(this.$scope.draggingNode.draggedNode).toBe(null);
  });

  it('should do nothing if no node is dragged', function() {
    this.$scope.draggingNode.draggedNode = null;

    expect(nodedragging.dragend(this.fakeEvent)).not.toBe(false);
    expect(this.fakeEvent.preventDefault).not.toHaveBeenCalled();

    expect(nodedragging.drop(this.fakeEvent)).not.toBe(false);
    expect(this.fakeEvent.preventDefault).not.toHaveBeenCalled();

    expect(nodedragging.dragover(this.fakeEvent)).not.toBe(false);
    expect(this.fakeEvent.preventDefault).not.toHaveBeenCalled();

    expect(this.$scope.draggingNode.draggedNode).toBe(null);
  });

  it('should fix the internet explorer setDragImage bug', function() {
    this.fakeEvent.dataTransfer.setDragImage = null;
    nodedragging.dragstart(this.node)(this.fakeEvent);

    expect(this.fakeEvent.target.style.display).toEqual('none');
  });
});
