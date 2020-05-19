let sinon = window.sinon,
  expect = window.expect,
  inject = window.inject,
  module = window.module;

describe('Controller: ActivityController', function () {
  let activityController,

    findAllDeferred,

    $controller,
    activityService,
    $q,
    $rootScope,

    someUser,
    expectedActivity,
    sandbox;

  function respondFromServiceWith(response) {
    findAllDeferred.resolve(response);
    $rootScope.$apply();
  }

  function injectDependencies(_$controller_, _ActivityService_, _$q_, _$rootScope_) {
    $controller = _$controller_;
    activityService = _ActivityService_;
    $q = _$q_;
    $rootScope = _$rootScope_;
  }

  beforeEach(function () {
    expectedActivity = [{}, {}];
    someUser = 'some user';

    sandbox = sinon.sandbox.create();

    module('app');

    inject(injectDependencies);

    findAllDeferred = $q.defer();
    sandbox.stub(activityService, 'findAllWatchedRepos')
      .withArgs(someUser)
      .returns(findAllDeferred.promise);

    activityController = $controller('ActivityCtrl', {
      ActivityService: activityService,
      targetUser: someUser
    });
  });

  afterEach(function () {
    sandbox.restore();
  });

  it('should get the list of commits', function () {
    expect(activityController.watchedRepos).to.eql([]);

    respondFromServiceWith(expectedActivity);
    expect(activityController.watchedRepos).to.equal(expectedActivity);
  });
});
