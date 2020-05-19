describe('events', function () {
  var $compile;
  var $rootScope;
  var element;
  var elementScope;

  beforeEach(module('flow'));

  beforeEach(inject(function(_$compile_, _$rootScope_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    $rootScope.fileProgress = jasmine.createSpy('fileProgress');
    element = $compile('<div flow-init flow-file-progress="fileProgress($file)">' +
      '<div flow-file-progress="fileProgress($file)"></div>' +
    '</div>')($rootScope);
    $rootScope.$digest();
    elementScope = element.scope();
  }));

  describe('$broadcast events', function () {
    var ngFileProgress;
    var ngRootFileProgress;
    beforeEach(inject(function(){
      ngFileProgress = jasmine.createSpy('fileProgress');
      ngRootFileProgress = jasmine.createSpy('ngRootFileProgress');
      elementScope.$on('flow::fileProgress', ngFileProgress);
      $rootScope.$on('flow::fileProgress', ngRootFileProgress);
      elementScope.$flow.fire('fileProgress', 'file');
    }));
    it('should catch event on element scope', function () {
      expect(ngFileProgress).toHaveBeenCalled();
      expect(ngFileProgress.calls.count()).toBe(1);
      var args = ngFileProgress.calls.mostRecent().args;
      expect(args[1]).toBe(elementScope.$flow);
      expect(args[2]).toBe('file');
    });
    it('should not catch event on parent scope', function () {
      expect(ngRootFileProgress).not.toHaveBeenCalled();
    });
  });

  describe('uploadStart event should be aliased', function () {
    var uploadStart;
    beforeEach(inject(function(){
      $rootScope.uploadStart = jasmine.createSpy('uploadStart');
      element = $compile('<div flow-init flow-upload-started="uploadStart($file)">' +
        '</div>')($rootScope);
      $rootScope.$digest();
      elementScope = element.scope();
    }));
    it('should catch broadcast event', function () {
      uploadStart = jasmine.createSpy('uploadStart');
      elementScope.$on('flow::uploadStart', uploadStart);
      elementScope.$flow.fire('uploadStart');
      expect(uploadStart.calls.count()).toBe(1);
    });
    it('should execute scope function', function () {
      elementScope.$flow.fire('uploadStart');
      expect($rootScope.uploadStart.calls.count()).toBe(1);
    });
  });

  it('should call event', function () {
    elementScope.$flow.fire('fileProgress', 'file');
    expect($rootScope.fileProgress).toHaveBeenCalledWith('file');
    expect($rootScope.fileProgress.calls.count()).toBe(2);
  });

  describe('nested flow directives', function () {
    var flowScope1;
    var flowScope2;
    beforeEach(inject(function() {
      $rootScope.fileProgress1 = jasmine.createSpy('fileProgress1');
      $rootScope.fileProgress2 = jasmine.createSpy('fileProgress2');
      element = $compile('<div flow-init flow-file-progress="fileProgress1($file)">' +
        '<div flow-init flow-file-progress="fileProgress2($file)"></div>' +
        '</div>')($rootScope);
      $rootScope.$digest();
      flowScope1 = element.scope();
      flowScope2 = flowScope1.$$childHead;
    }));
    it('should not call event of child directive', function () {
      flowScope1.$flow.fire('fileProgress', 'file1');
      expect($rootScope.fileProgress1).toHaveBeenCalledWith('file1');
      expect($rootScope.fileProgress2).not.toHaveBeenCalled();
    });
    it('should not call event of parent directive', function () {
      flowScope2.$flow.fire('fileProgress', 'file2');
      expect($rootScope.fileProgress2).toHaveBeenCalledWith('file2');
      expect($rootScope.fileProgress1).not.toHaveBeenCalled();
    });
  });
});
