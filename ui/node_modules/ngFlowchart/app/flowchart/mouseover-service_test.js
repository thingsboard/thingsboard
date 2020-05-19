'use strict';

describe('The mouse-overservice', function() {
  beforeEach(function() {
    module('flowchart');
  });

  beforeEach(inject(function($rootScope, Mouseoverfactory, flowchartConstants) {
    this.$scope = $rootScope.$new();
    this.$scope.mouseOver = {};

    this.applyFunction = jasmine.createSpy('applyfunction').and.callFake(function(f) {
      return f();
    });
    this.mouseoverservice = Mouseoverfactory(this.$scope.mouseOver, this.applyFunction);

    this.node = {name: 'testnode', id: 1, x: 0, y: 0};
    this.connector = {id: 1, type: flowchartConstants.rightConnectorType};
    this.edge = {source: 1, destination: 2};

    this.event = {};
  }));

  it('should initialize the $scope.mouseOver', function() {
    expect(this.$scope.mouseOver.node).toBeNull();
    expect(this.$scope.mouseOver.connector).toBeNull();
    expect(this.$scope.mouseOver.edge).toBeNull();
  });

  it('.nodeMouseEnter and .nodeMouseLeave should mark the node as hovered and call the applyFunction', function() {
    this.mouseoverservice.nodeMouseOver(this.node)(this.event);
    expect(this.$scope.mouseOver.node).toBe(this.node);
    expect(this.applyFunction.calls.count()).toEqual(1);

    expect(this.$scope.mouseOver.connector).toBeNull();
    expect(this.$scope.mouseOver.edge).toBeNull();

    this.mouseoverservice.nodeMouseOut(this.node)(this.event);
    expect(this.$scope.mouseOver.node).toBeNull();
    expect(this.applyFunction.calls.count()).toEqual(2);

    expect(this.$scope.mouseOver.connector).toBeNull();
    expect(this.$scope.mouseOver.edge).toBeNull();
  });

  it('.connectorMouseEnter and .connectorMouseLeave should mark the connector as hovered and call the applyFunction', function() {
    this.mouseoverservice.connectorMouseEnter(this.connector)(this.event);
    expect(this.$scope.mouseOver.connector).toBe(this.connector);
    expect(this.applyFunction.calls.count()).toEqual(1);

    expect(this.$scope.mouseOver.node).toBeNull();
    expect(this.$scope.mouseOver.edge).toBeNull();

    this.mouseoverservice.connectorMouseLeave(this.connector)(this.event);
    expect(this.$scope.mouseOver.connector).toBeNull();
    expect(this.applyFunction.calls.count()).toEqual(2);

    expect(this.$scope.mouseOver.node).toBeNull();
    expect(this.$scope.mouseOver.edge).toBeNull();
  });

  it('.edgeMouseEnter and .edgeMouseLeave should mark the connector as hovered and not call the applyfunction', function() {
    this.mouseoverservice.edgeMouseEnter(this.event, this.edge);
    expect(this.$scope.mouseOver.edge).toBe(this.edge);

    expect(this.$scope.mouseOver.node).toBeNull();
    expect(this.$scope.mouseOver.connector).toBeNull();

    this.mouseoverservice.edgeMouseLeave(this.event, this.edge);
    expect(this.$scope.mouseOver.edge).toBeNull();
    expect(this.applyFunction).not.toHaveBeenCalled();

    expect(this.$scope.mouseOver.node).toBeNull();
    expect(this.$scope.mouseOver.connector).toBeNull();
  });

});
