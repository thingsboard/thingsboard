angular.module('angular-jwt.interceptor', [])
  .provider('jwtInterceptor', function() {

    this.urlParam;
    this.authHeader;
    this.authPrefix;
    this.whiteListedDomains;
    this.tokenGetter;

    var config = this;

    this.$get = function($q, $injector, $rootScope, urlUtils, jwtOptions) {

      var options = angular.extend({}, jwtOptions.getConfig(), config);

      function isSafe (url) {
        if (!urlUtils.isSameOrigin(url) && !options.whiteListedDomains.length) {
          throw new Error('As of v0.1.0, requests to domains other than the application\'s origin must be white listed. Use jwtOptionsProvider.config({ whiteListedDomains: [<domain>] }); to whitelist.')
        }
        var hostname = urlUtils.urlResolve(url).hostname.toLowerCase();
        for (var i = 0; i < options.whiteListedDomains.length; i++) {
          var domain = options.whiteListedDomains[i];
          if (domain instanceof RegExp) {
            if (hostname.match(domain)) {
              return true;
            }
          } else {
            if (hostname === domain.toLowerCase()) {
              return true;
            }
          }
        }

        if (urlUtils.isSameOrigin(url)) {
          return true;
        }

        return false;
      }

      return {
        request: function (request) {
          if (request.skipAuthorization || !isSafe(request.url)) {
            return request;
          }

          if (options.urlParam) {
            request.params = request.params || {};
            // Already has the token in the url itself
            if (request.params[options.urlParam]) {
              return request;
            }
          } else {
            request.headers = request.headers || {};
            // Already has an Authorization header
            if (request.headers[options.authHeader]) {
              return request;
            }
          }

          var tokenPromise = $q.when($injector.invoke(options.tokenGetter, this, {
            options: request
          }));

          return tokenPromise.then(function(token) {
            if (token) {
              if (options.urlParam) {
                request.params[options.urlParam] = token;
              } else {
                request.headers[options.authHeader] = options.authPrefix + token;
              }
            }
            return request;
          });
        },
        responseError: function (response) {
          // handle the case where the user is not authenticated
          if (response !== undefined && response.status === 401) {
            $rootScope.$broadcast('unauthenticated', response);
          }
          return $q.reject(response);
        }
      };
    }
  });
