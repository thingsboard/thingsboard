'use strict';

describe('interceptor', function() {

  beforeEach(function() {
    module('angular-jwt.interceptor');
    module('angular-jwt.options');
  });

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));


  it('should intercept requests when added to $httpProvider.interceptors and set token', function (done) {
    module( function ($httpProvider, jwtInterceptorProvider, jwtOptionsProvider) {
      jwtInterceptorProvider.tokenGetter = function() {
        return 123;
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend) {
        $http({url: '/hello'}).then(function (data) {
          expect(data.data).to.be.equal('hello');
          done();
        });

        $httpBackend.expectGET('/hello', function (headers) {
          return headers.Authorization === 'Bearer 123';
        }).respond(200, 'hello');
        $httpBackend.flush();
    });

  });

  it('should not add Authr headers to Cross Origin requests unless whitelisted', function (done) {
    module( function ($httpProvider, jwtOptionsProvider, jwtInterceptorProvider) {
      jwtInterceptorProvider.whiteListedDomains = ['whitelisted.Example.com']
      jwtInterceptorProvider.tokenGetter = function() {
        return 123;
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend, $q) {
      $q.all([
        $http({url: 'http://Example.com/hello' }),
        $http({url: 'http://www.example.com/hello' }),
        $http({url: 'http://wwwXexample.com/hello' }),
        $http({url: 'http://whitelisted.example.com.evil.com/hello' }),
        $http({url: 'http://whitelisted.example.com/hello' })
      ]).then(function () {
        done();
      })

      $httpBackend.expectGET('http://Example.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://www.example.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://wwwXexample.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://whitelisted.example.com.evil.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://whitelisted.example.com/hello', function (headers) {
        return headers.Authorization === 'Bearer 123';
      }).respond(200);

      $httpBackend.flush();
    });
  })

  it('should not add Authr headers to Cross Origin requests unless whitelisted with regexp', function (done) {
    module( function ($httpProvider, jwtOptionsProvider, jwtInterceptorProvider) {
      jwtInterceptorProvider.whiteListedDomains = [/^whitelisted(-pr-\d+)?\.Example\.com$/i]
      jwtInterceptorProvider.tokenGetter = function() {
        return 123;
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend, $q) {
      $q.all([
        $http({url: 'http://Example.com/hello' }),
        $http({url: 'http://www.example.com/hello' }),
        $http({url: 'http://whitelisted-pr-123.example.com.evil.com/hello' }),
        $http({url: 'http://extrawhitelisted-pr-123.example.com.evil.com/hello' }),
        $http({url: 'http://whitelisted-pr-123.example.com/hello' })
      ]).then(function () {
        done();
      })

      $httpBackend.expectGET('http://Example.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://www.example.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://whitelisted-pr-123.example.com.evil.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://extrawhitelisted-pr-123.example.com.evil.com/hello', function (headers) {
        return headers.Authorization === undefined;
      }).respond(200);
      $httpBackend.expectGET('http://whitelisted-pr-123.example.com/hello', function (headers) {
        return headers.Authorization === 'Bearer 123';
      }).respond(200);

      $httpBackend.flush();
    });
  })

  it('should work with promises', function (done) {
    module( function ($httpProvider, jwtOptionsProvider, jwtInterceptorProvider) {
      jwtInterceptorProvider.tokenGetter = function($q) {
        return $q.when(345);
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend) {
        $http({url: '/hello'}).then(function (data) {
          expect(data.data).to.be.equal('hello');
          done();
        });

        $httpBackend.expectGET('/hello', function (headers) {
          return headers.Authorization === 'Bearer 345';
        }).respond(200, 'hello');
        $httpBackend.flush();
    });

  });

  it('should not send it if no tokenGetter', function (done) {
    module( function ($httpProvider, jwtInterceptorProvider) {
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend) {
        $http({url: '/hello'}).then(function (data) {
          expect(data.data).to.be.equal('hello');
          done();
        });

        $httpBackend.expectGET('/hello', function (headers) {
          return !headers.Authorization;
        }).respond(200, 'hello');
        $httpBackend.flush();
    });

  });

  it('should add the token to the url params when the configuration option is set', function (done) {
    module( function ($httpProvider, jwtOptionsProvider, jwtInterceptorProvider) {
      jwtInterceptorProvider.urlParam = 'access_token';
      jwtInterceptorProvider.tokenGetter = function() {
        return 123;
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend) {
        $http({url: '/hello'}).then(function (data) {
          expect(data.data).to.be.equal('hello');
          done();
        });

        $httpBackend.expectGET('/hello?access_token=123', function (headers) {
          return headers.Authorization === undefined;
        }).respond(200, 'hello');
        $httpBackend.flush();
    });

  });

  it('should handle an undefined responseError', function (done) {
    module( function($httpProvider, jwtOptionsProvider, jwtInterceptorProvider) {
      jwtInterceptorProvider.tokenGetter = function() {
        return 123;
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function (jwtInterceptor, $httpBackend) {
      jwtInterceptor.responseError(undefined).then(function (data) {

      }).catch(function (data) {
        expect(data).to.be.equal(undefined);
        done();
      });

      $httpBackend.flush();
    });
  });

  it('should broadcast unauthenticated on 401 response', function (done) {
    module( function ($httpProvider, jwtInterceptorProvider, jwtOptionsProvider, $rootScopeProvider) {
      jwtInterceptorProvider.tokenGetter = function() {
        return 123;
      }
      $httpProvider.interceptors.push('jwtInterceptor');
    });

    inject(function ($http, $httpBackend, $rootScope) {
      sinon.spy($rootScope, "$broadcast");

      $http({url: '/hello'}).then(function (data) {

      }).catch(function (data) {
        expect(data.status).to.be.equal(401);
        expect($rootScope.$broadcast.calledWith("unauthenticated")).to.be.true
        done();
      });
      $httpBackend.expectGET('/hello', function (headers) {
        return headers.Authorization === 'Bearer 123';
      }).respond(401, {}, {}, 'Error');
      
      $httpBackend.flush();
    });

  });
});
