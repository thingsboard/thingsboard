(function() {


// Create all modules and define dependencies to make sure they exist
// and are loaded in the correct order to satisfy dependency injection
// before all nested files are concatenated by Grunt

// Modules
angular.module('angular-jwt',
    [
        'angular-jwt.options',
        'angular-jwt.interceptor',
        'angular-jwt.jwt',
        'angular-jwt.authManager'
    ]);

angular.module('angular-jwt.authManager', [])
  .provider('authManager', function () {

    this.$get = ["$rootScope", "$injector", "$location", "jwtHelper", "jwtInterceptor", "jwtOptions", function ($rootScope, $injector, $location, jwtHelper, jwtInterceptor, jwtOptions) {

      var config = jwtOptions.getConfig();

      function invokeToken(tokenGetter) {
        var token = null;
        if (Array.isArray(tokenGetter)) {
          token = $injector.invoke(tokenGetter, this, {options: null});
        } else {
          token = tokenGetter();
        }
        return token;
      }

      function invokeRedirector(redirector) {
        if (Array.isArray(redirector) || angular.isFunction(redirector)) {
          return $injector.invoke(redirector, config, {});
        } else {
          throw new Error('unauthenticatedRedirector must be a function');
        }
      }

      function isAuthenticated() {
        var token = invokeToken(config.tokenGetter);
        if (token) {
          return !jwtHelper.isTokenExpired(token);
        }
      }

      $rootScope.isAuthenticated = false;

      function authenticate() {
        $rootScope.isAuthenticated = true;
      }

      function unauthenticate() {
        $rootScope.isAuthenticated = false;
      }

      function validateToken() {
        var token = invokeToken(config.tokenGetter);
        if (token) {
          if (!jwtHelper.isTokenExpired(token)) {
            authenticate();
          } else {
            $rootScope.$broadcast('tokenHasExpired', token);
          }
        }
      }

      function checkAuthOnRefresh() {
        if ($injector.has('$transitions')) {
          var $transitions = $injector.get('$transitions');
          $transitions.onStart({}, validateToken);
        } else {
          $rootScope.$on('$locationChangeStart', validateToken);
        }
      }

      function redirectWhenUnauthenticated() {
        $rootScope.$on('unauthenticated', function () {
          invokeRedirector(config.unauthenticatedRedirector);
          unauthenticate();
        });
      }

      function verifyRoute(event, next) {
        if (!next) {
          return false;
        }

        var routeData = (next.$$route) ? next.$$route : next.data;

        if (routeData && routeData.requiresLogin === true && !isAuthenticated()) {
          event.preventDefault();
          invokeRedirector(config.unauthenticatedRedirector);
        }
      }

      function verifyState(transition) {
        var route = transition.to();
        var $state = transition.router.stateService;
          if (route && route.data && route.data.requiresLogin === true && !isAuthenticated()) {
            return $state.target(config.loginPath);
          }
      }

      if ($injector.has('$transitions')) {
        var $transitions = $injector.get('$transitions');
        $transitions.onStart({}, verifyState);
      } else {
        var eventName = ($injector.has('$state')) ? '$stateChangeStart' : '$routeChangeStart';
        $rootScope.$on(eventName, verifyRoute);
      }



      return {
        authenticate: authenticate,
        unauthenticate: unauthenticate,
        getToken: function(){ return invokeToken(config.tokenGetter); },
        redirect: function() { return invokeRedirector(config.unauthenticatedRedirector); },
        checkAuthOnRefresh: checkAuthOnRefresh,
        redirectWhenUnauthenticated: redirectWhenUnauthenticated,
        isAuthenticated: isAuthenticated
      }
    }]
  });

angular.module('angular-jwt.interceptor', [])
  .provider('jwtInterceptor', function() {

    this.urlParam;
    this.authHeader;
    this.authPrefix;
    this.whiteListedDomains;
    this.tokenGetter;

    var config = this;

    this.$get = ["$q", "$injector", "$rootScope", "urlUtils", "jwtOptions", function($q, $injector, $rootScope, urlUtils, jwtOptions) {

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
    }]
  });

 angular.module('angular-jwt.jwt', [])
  .service('jwtHelper', ["$window", function($window) {

    this.urlBase64Decode = function(str) {
      var output = str.replace(/-/g, '+').replace(/_/g, '/');
      switch (output.length % 4) {
        case 0: { break; }
        case 2: { output += '=='; break; }
        case 3: { output += '='; break; }
        default: {
          throw 'Illegal base64url string!';
        }
      }
      return $window.decodeURIComponent(escape($window.atob(output))); //polyfill https://github.com/davidchambers/Base64.js
    };


    this.decodeToken = function(token) {
      var parts = token.split('.');

      if (parts.length !== 3) {
        throw new Error('JWT must have 3 parts');
      }

      var decoded = this.urlBase64Decode(parts[1]);
      if (!decoded) {
        throw new Error('Cannot decode the token');
      }

      return angular.fromJson(decoded);
    };

    this.getTokenExpirationDate = function(token) {
      var decoded = this.decodeToken(token);

      if(typeof decoded.exp === "undefined") {
        return null;
      }

      var d = new Date(0); // The 0 here is the key, which sets the date to the epoch
      d.setUTCSeconds(decoded.exp);

      return d;
    };

    this.isTokenExpired = function(token, offsetSeconds) {
      var d = this.getTokenExpirationDate(token);
      offsetSeconds = offsetSeconds || 0;
      if (d === null) {
        return false;
      }

      // Token expired?
      return !(d.valueOf() > (new Date().valueOf() + (offsetSeconds * 1000)));
    };
  }]);

angular.module('angular-jwt.options', [])
  .provider('jwtOptions', function() {
    var globalConfig = {};
    this.config = function(value) {
      globalConfig = value;
    };
    this.$get = function() {

      var options = {
        urlParam: null,
        authHeader: 'Authorization',
        authPrefix: 'Bearer ',
        whiteListedDomains: [],
        tokenGetter: function() {
          return null;
        },
        loginPath: '/',
        unauthenticatedRedirectPath: '/',
        unauthenticatedRedirector: ['$location', function($location) {
          $location.path(this.unauthenticatedRedirectPath);
        }]
      };

      function JwtOptions() {
        var config = this.config = angular.extend({}, options, globalConfig);
      }

      JwtOptions.prototype.getConfig = function() {
        return this.config;
      };

      return new JwtOptions();
    }
  });

 /**
  * The content from this file was directly lifted from Angular. It is
  * unfortunately not a public API, so the best we can do is copy it.
  *
  * Angular References:
  *   https://github.com/angular/angular.js/issues/3299
  *   https://github.com/angular/angular.js/blob/d077966ff1ac18262f4615ff1a533db24d4432a7/src/ng/urlUtils.js
  */

 angular.module('angular-jwt.interceptor')
  .service('urlUtils', function () {

    // NOTE:  The usage of window and document instead of $window and $document here is
    // deliberate.  This service depends on the specific behavior of anchor nodes created by the
    // browser (resolving and parsing URLs) that is unlikely to be provided by mock objects and
    // cause us to break tests.  In addition, when the browser resolves a URL for XHR, it
    // doesn't know about mocked locations and resolves URLs to the real document - which is
    // exactly the behavior needed here.  There is little value is mocking these out for this
    // service.
    var urlParsingNode = document.createElement("a");
    var originUrl = urlResolve(window.location.href);

    /**
     *
     * Implementation Notes for non-IE browsers
     * ----------------------------------------
     * Assigning a URL to the href property of an anchor DOM node, even one attached to the DOM,
     * results both in the normalizing and parsing of the URL.  Normalizing means that a relative
     * URL will be resolved into an absolute URL in the context of the application document.
     * Parsing means that the anchor node's host, hostname, protocol, port, pathname and related
     * properties are all populated to reflect the normalized URL.  This approach has wide
     * compatibility - Safari 1+, Mozilla 1+, Opera 7+,e etc.  See
     * http://www.aptana.com/reference/html/api/HTMLAnchorElement.html
     *
     * Implementation Notes for IE
     * ---------------------------
     * IE <= 10 normalizes the URL when assigned to the anchor node similar to the other
     * browsers.  However, the parsed components will not be set if the URL assigned did not specify
     * them.  (e.g. if you assign a.href = "foo", then a.protocol, a.host, etc. will be empty.)  We
     * work around that by performing the parsing in a 2nd step by taking a previously normalized
     * URL (e.g. by assigning to a.href) and assigning it a.href again.  This correctly populates the
     * properties such as protocol, hostname, port, etc.
     *
     * References:
     *   http://developer.mozilla.org/en-US/docs/Web/API/HTMLAnchorElement
     *   http://www.aptana.com/reference/html/api/HTMLAnchorElement.html
     *   http://url.spec.whatwg.org/#urlutils
     *   https://github.com/angular/angular.js/pull/2902
     *   http://james.padolsey.com/javascript/parsing-urls-with-the-dom/
     *
     * @kind function
     * @param {string} url The URL to be parsed.
     * @description Normalizes and parses a URL.
     * @returns {object} Returns the normalized URL as a dictionary.
     *
     *   | member name   | Description    |
     *   |---------------|----------------|
     *   | href          | A normalized version of the provided URL if it was not an absolute URL |
     *   | protocol      | The protocol including the trailing colon                              |
     *   | host          | The host and port (if the port is non-default) of the normalizedUrl    |
     *   | search        | The search params, minus the question mark                             |
     *   | hash          | The hash string, minus the hash symbol
     *   | hostname      | The hostname
     *   | port          | The port, without ":"
     *   | pathname      | The pathname, beginning with "/"
     *
     */
    function urlResolve(url) {
      var href = url;

      // Normalize before parse.  Refer Implementation Notes on why this is
      // done in two steps on IE.
      urlParsingNode.setAttribute("href", href);
      href = urlParsingNode.href;
      urlParsingNode.setAttribute('href', href);

      // urlParsingNode provides the UrlUtils interface - http://url.spec.whatwg.org/#urlutils
      return {
        href: urlParsingNode.href,
        protocol: urlParsingNode.protocol ? urlParsingNode.protocol.replace(/:$/, '') : '',
        host: urlParsingNode.host,
        search: urlParsingNode.search ? urlParsingNode.search.replace(/^\?/, '') : '',
        hash: urlParsingNode.hash ? urlParsingNode.hash.replace(/^#/, '') : '',
        hostname: urlParsingNode.hostname,
        port: urlParsingNode.port,
        pathname: (urlParsingNode.pathname.charAt(0) === '/')
          ? urlParsingNode.pathname
          : '/' + urlParsingNode.pathname
      };
    }

    /**
     * Parse a request URL and determine whether this is a same-origin request as the application document.
     *
     * @param {string|object} requestUrl The url of the request as a string that will be resolved
     * or a parsed URL object.
     * @returns {boolean} Whether the request is for the same origin as the application document.
     */
    function urlIsSameOrigin(requestUrl) {
      var parsed = (angular.isString(requestUrl)) ? urlResolve(requestUrl) : requestUrl;
      return (parsed.protocol === originUrl.protocol &&
              parsed.host === originUrl.host);
    }

    return {
      urlResolve: urlResolve,
      isSameOrigin: urlIsSameOrigin
    };

  });

}());