let expect = window.expect,
  inject = window.inject,
  module = window.module;

describe('Service: Activity', function () {
  const SOME_USER_ID = 'some-user';

  let activityService,
    httpBackend,
    requestHandler,

    someWatchedRepos,

    GITHUB_ENDPOINT = `https://api.github.com/users/${SOME_USER_ID}/subscriptions`;

  beforeEach(function () {
    someWatchedRepos = [
      {
        id: 0,
        name: 'some-name'
      },
      {
        id: 1,
        name: 'another name'
      }
    ];

    module('app');

    inject(function (_ActivityService_, $httpBackend) {
      activityService = _ActivityService_;
      httpBackend = $httpBackend;
    });

    requestHandler = httpBackend.whenGET(GITHUB_ENDPOINT);
  });

  afterEach(function () {
    httpBackend.verifyNoOutstandingRequest();
  });

  it('should get a list of watched repos', function () {
    requestHandler.respond(someWatchedRepos);

    activityService.findAllWatchedRepos(SOME_USER_ID).then(function (watchedRepos) {
      expect(watchedRepos).to.eql(someWatchedRepos);
    });
    httpBackend.flush();
  });
});
