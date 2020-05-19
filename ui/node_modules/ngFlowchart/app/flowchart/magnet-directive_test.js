describe('The magnet-directive', function() {

  var flowchartConstants;

  beforeEach(function() {
    module('flowchart');
  });

  beforeEach(inject(function(_$compile_, _$rootScope_, _flowchartConstants_) {
    var $compile = _$compile_;
    var $rootScope = _$rootScope_;
    flowchartConstants = _flowchartConstants_;

    this.scope = $rootScope.$new();
    this.scope.fcCallbacks = jasmine.createSpyObj('callbacks', ['edgeDragoverMagnet', 'edgeDrop', 'edgeDragend']);

    this.innerEdgeDragoverMagnet = jasmine.createSpy('innerEdgeDragoverMagnet');
    this.scope.fcCallbacks.edgeDragoverMagnet.and.returnValue(this.innerEdgeDragoverMagnet);

    this.innerEdgeDrop = jasmine.createSpy('innerEdgeDrop');
    this.scope.fcCallbacks.edgeDrop.and.returnValue(this.innerEdgeDrop);

    this.magnet = $compile('<fc-magnet></fc-magnet>')(this.scope);

  }));

  it('should register a magnet dragover handler', function() {
    expect(this.innerEdgeDragoverMagnet).not.toHaveBeenCalled();
    this.magnet.triggerHandler('dragover');
    expect(this.innerEdgeDragoverMagnet).toHaveBeenCalled();
  });

  it('should register a magnet drop handler', function() {
    expect(this.innerEdgeDrop).not.toHaveBeenCalled();
    this.magnet.triggerHandler('drop');
    expect(this.innerEdgeDrop).toHaveBeenCalled();
  });

  it('should register a magnet dragend handler', function() {
    expect(this.scope.fcCallbacks.edgeDragend).not.toHaveBeenCalled();
    this.magnet.triggerHandler('dragend');
    expect(this.scope.fcCallbacks.edgeDragend).toHaveBeenCalled();
  });

  it('should add the fc-magnet class', function() {
    expect(this.magnet.hasClass(flowchartConstants.magnetClass)).toBe(true);
  })
});
