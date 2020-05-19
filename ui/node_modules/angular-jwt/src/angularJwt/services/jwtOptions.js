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
