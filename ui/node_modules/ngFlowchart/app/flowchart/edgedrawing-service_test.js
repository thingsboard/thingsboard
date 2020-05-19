'use strict';

describe('The edgedrawing service', function() {


  beforeEach(function() {
    module('flowchart');
  });

  beforeEach(inject(function(Edgedrawingservice, flowchartConstants) {
    this.Edgedrawingservice = Edgedrawingservice;
    this.flowchartConstants = flowchartConstants;
    this.startPoint = {x: 10, y: 10};
    this.endPoint = {x: 50, y: 50};

    this.LINE_MATCHER = 'M 10, 10 L 50, 50';
    this.CURVE_MATCHER = /^M 10, 10 C (.*) 50, 50$/; // Move to start point, curve and end at endpoint.

  }));

  it('should implement linestyle', function() {
    var line = this.Edgedrawingservice.getEdgeDAttribute(angular.copy(this.startPoint), angular.copy(this.endPoint), this.flowchartConstants.lineStyle);
    expect(line).toEqual(this.LINE_MATCHER);
  });

  it('should implement curvedstyle', function() {
    var curve = this.Edgedrawingservice.getEdgeDAttribute(angular.copy(this.startPoint), angular.copy(this.endPoint), this.flowchartConstants.curvedStyle);
    expect(curve).toMatch(this.CURVE_MATCHER);
  });
});
