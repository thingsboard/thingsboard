# angular-jwt

This library will help you work with [JWTs](http://jwt.io/).

## Key Features

* **Decode a JWT** from your AngularJS app
* Check the **expiration date** of the JWT
* Automatically **send the JWT in every request** made to the server
* Manage the user's authentication state with **authManager**

## Installing it

You have several options: Install with either bower or npm and link to the installed file from html using script tag.

```bash
bower install angular-jwt
```

```bash
npm install angular-jwt
```

## jwtHelper

jwtHelper will take care of helping you decode the token and check its expiration date.

### Decoding the Token

```js
angular
  .module('app', ['angular-jwt'])
  .controller('Controller', function Controller(jwtHelper) {
    var expToken = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3NhbXBsZXMuYXV0aDAuY29tLyIsInN1YiI6ImZhY2Vib29rfDEwMTU0Mjg3MDI3NTEwMzAyIiwiYXVkIjoiQlVJSlNXOXg2MHNJSEJ3OEtkOUVtQ2JqOGVESUZ4REMiLCJleHAiOjE0MTIyMzQ3MzAsImlhdCI6MTQxMjE5ODczMH0.7M5sAV50fF1-_h9qVbdSgqAnXVF7mz3I6RjS6JiH0H8';  

    var tokenPayload = jwtHelper.decodeToken(expToken);
  });
```
### Getting the Token Expiration Date

```js
angular
  .module('app', ['angular-jwt'])
  .controller('Controller', function Controller(jwtHelper) {
    var date = jwtHelper.getTokenExpirationDate(expToken);
  });
```

### Checking if the Token is Expired

```js
angular
  .module('app', ['angular-jwt'])
  .controller('Controller', function Controller(jwtHelper) {
    var bool = jwtHelper.isTokenExpired(expToken);
  });
```

### More Examples

You can see some more examples of how this works in [the tests](https://github.com/auth0/angular-jwt/blob/master/test/unit/angularJwt/services/jwtSpec.js)

## jwtInterceptor

JWT interceptor will take care of sending the JWT in every request.

### Basic Usage

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    // Please note we're annotating the function so that the $injector works when the file is minified
    jwtOptionsProvider.config({
      tokenGetter: ['myService', function(myService) {
        myService.doSomething();
        return localStorage.getItem('id_token');
      }]
    });

    $httpProvider.interceptors.push('jwtInterceptor');
  })
  .controller('Controller', function Controller($http) {
    // If localStorage contains the id_token it will be sent in the request
    // Authorization: Bearer [yourToken] will be sent
    $http({
      url: '/hola',
      method: 'GET'
    });
  });
```

### Configuring the Authentication Scheme

By default, angular-jwt uses the `Bearer` scheme when sending JSON Web Tokens as an `Authorization` header. The header that gets attached to `$http` requests looks like this:

```
Authorization: Bearer eyJ0eXAiOiJKV...
```

If you would like to provide your own scheme, you can configure it by setting a value for `authPrefix` in the `jwtOptionsProvider` configuration.

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({
      authPrefix: 'MyPrefix '
      ...
    });

    $httpProvider.interceptors.push('jwtInterceptor');
```

### Not Sending the JWT for Specific Requests

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    // Please note we're annotating the function so that the $injector works when the file is minified
    jwtOptionsProvider.config({
      tokenGetter: ['myService', function(myService) {
        myService.doSomething();
        return localStorage.getItem('id_token');
      }]
    });

    $httpProvider.interceptors.push('jwtInterceptor');
  })
  .controller('Controller', function Controller($http) {
    // This request will NOT send the token as it has skipAuthorization
    $http({
      url: '/hola',
      skipAuthorization: true,
      method: 'GET'
    });
  });
```

### Whitelisting Domains

If you are calling an API that is on a domain other than your application's origin, you will need to whitelist it.

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({

      ...

      whiteListedDomains: ['api.myapp.com', 'localhost']
    });
  });
```

Note that you only need to provide the domain. Protocols (ex: `http://`) and port numbers should be omitted.

You can also specify the domain using a regular expression.

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({

      ...

      whiteListedDomains: [/^api-version-\d+\.myapp\.com$/i, 'localhost']
    });
  });
```

Regular expressions should be as strict as possible to prevent attackers from registering their own malicious domains to bypass the whitelist.

### Not Sending the JWT for Template Requests

The `tokenGetter` method can have a parameter `options` injected by angular-jwt. This parameter is the options object of the current request.

By default the interceptor will send the JWT for all HTTP requests. This includes any `ng-include` directives or
`templateUrls` defined in a `state` in the `stateProvider`. If you want to avoid sending the JWT for these requests you
should adapt your `tokenGetter` method to fit your needs. For example:

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({
      tokenGetter: ['options', function(options) {
        // Skip authentication for any requests ending in .html
        if (options.url.substr(options.url.length - 5) == '.html') {
          return null;
        }

        return localStorage.getItem('id_token');
      }]
    });

    $httpProvider.interceptors.push('jwtInterceptor');
  });
```

### Sending Different Tokens Based on URLs

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({
      tokenGetter: ['options', function(options) {
        if (options.url.indexOf('http://auth0.com') === 0) {
          return localStorage.getItem('auth0.id_token');
        } else {
          return localStorage.getItem('id_token');
        }
      }]
    });
    $httpProvider.interceptors.push('jwtInterceptor');
  })
  .controller('Controller', function Controller($http) {
    // This request will send the auth0.id_token since URL matches
    $http({
      url: 'http://auth0.com/hola',
      skipAuthorization: true,
      method: 'GET'
    });
  });
```

## Managing Authentication state with `authManager`

Almost all applications that implement authentication need some indication of whether the user is authenticated or not and the **authManager** service provides a way to do this. Typical cases include conditionally showing and hiding different parts of the UI, checking whether the user is authenticated when the page is refreshed, and restricting routes to authenticated users.

```html
  <button ng-if="!isAuthenticated">Log In</button>
  <button ng-if="isAuthenticated">Log Out</button>
```
**Note:** `authManager` set `isAuthenticated` on your `$rootScope` object, If you are using component-based architecture,
your component `$scope` is isolated scope, it does not inherits `$rootScope` properties, you need to access `$rootScope` from component's template:

```html
  <button ng-if="!$root.isAuthenticated">Log In</button>
  <button ng-if="$root.isAuthenticated">Log Out</button>
```

### Getting Authentication State on Page Refresh

The authentication state that is set after login will only be good as long as the user doesn't refresh their page. If the page is refreshed, or the browser closed and reopened, the state will be lost. To check whether the user is actually authenticated when the page is refreshed, use the `checkAuthOnRefresh` method in the application's `run` block.

```js
angular
  .module('app')
  .run(function(authManager) {

    authManager.checkAuthOnRefresh();

  });
```

**Note:** If your `tokenGetter` relies on request `options`, be mindful that `checkAuthOnRefresh()` will pass `null` as `options` since the call happens in the run phase of the Angular lifecycle and no requests are fired through the Angular app. If you are using requestion `options`, check that `options` isn't `null` in your `tokenGetter` function:

```js
...

tokenGetter: ['options', function (options) {
  if (options && options.url.substr(options.url.length - 5) == '.html') {
    return null;
  }
  return localStorage.getItem('id_token');
}],

...
```

#### Responding to an Expired Token on Page Refresh

If the user is holding an expired JWT when the page is refreshed, the action that is taken is at your discretion. You may use the `tokenHasExpired` event to listen for expired tokens on page refresh and respond however you like.

```js
// app.run.js

...

$rootScope.$on('tokenHasExpired', function() {
  alert('Your session has expired!');
});

```
### Limiting Access to Routes

Access to various client-side routes can be limited to users who have an unexpired JWT, which is an indication that they are authenticated. Use `requiresLogin: true` on whichever routes you want to protect.

```js
...

.state('ping', {
  url: '/ping',
  controller: 'PingController',
  templateUrl: 'components/ping/ping.html',
  controllerAs: 'vm',
  data: {
    requiresLogin: true
  }
});

...
```

> **Note:** Protecting a route on the client side offers no guarantee that a savvy user won't be able to hack their way to that route. In fact, this could be done simply if the user alters the expiry time in their JWT with a tool like [jwt.io](https://jwt.io). Always ensure that sensitive data is kept off the client side and is protected on the server. 

### Redirecting the User On Unauthorized Requests

When the user's JWT expires and they attempt a call to a secured endpoint, a 401 - Unauthorized response will be returned. In these cases you will likely want to redirect the user back to the page/state used for authentication so they can log in again. This can be done with the `redirectWhenUnauthenticated` method in the application's `run` block.

```js
angular
  .module('app')
  .run(function(authManager) {

    ...

    authManager.redirectWhenUnauthenticated();

  });
```

### Configuring the Login State

The page/state to send the user to when they are redirected because of an unauthorized request can be configured with `jwtOptionsProvider`.

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({
      unauthenticatedRedirectPath: '/login'
    });
  });
```

### Configuring the Unauthenticated Redirector

If you would like to control the behavior of the redirection that happens when users become unauthenticated, you can configure `jwtOptionsProvider` with a custom function.

```js
angular
  .module('app', ['angular-jwt'])
  .config(function Config($httpProvider, jwtOptionsProvider) {
    jwtOptionsProvider.config({
      unauthenticatedRedirector: ['$state', function($state) {
        $state.go('app.login');
      }]
    });
  });
```

### Sending the token as a URL Param

```js
angular.module('app', ['angular-jwt'])
.config(function Config($httpProvider, jwtOptionsProvider) {
  jwtOptionsProvider.config({
    urlParam: 'access_token',
    tokenGetter: ['myService', function(myService) {
      myService.doSomething();
      return localStorage.getItem('id_token');
    }]
  });

  $httpProvider.interceptors.push('jwtInterceptor');
})
.controller('Controller', function Controller($http) {
  // If localStorage contains the id_token it will be sent in the request
  // url will contain access_token=[yourToken]
  $http({
    url: '/hola',
    method: 'GET'
  });
})
```

### More examples

You can see some more examples of how this works in [the tests](https://github.com/auth0/angular-jwt/blob/master/test/unit/angularJwt/services/interceptorSpec.js)

## FAQ

### I have minification problems with angular-jwt in production. What's going on?

When you're using the `tokenGetter` function, it's then called with the injector. `ngAnnotate` doesn't automatically detect that this function receives services as parameters, therefore you must either annotate this method for `ngAnnotate` to know, or use it like follows:

```js
jwtOptionsProvider({
  tokenGetter: ['store', '$http', function(store, $http) {
    ...
  }]
});
```

## Usages

This library is used in [auth0-angular](https://github.com/auth0/auth0-angular) and [angular-lock](https://github.com/auth0/angular-lock).

## Contributing

Just clone the repo, run `npm install`, `bower install` and then `gulp` to work :).

## Issue Reporting

If you have found a bug or if you have a feature request, please report them at this repository issues section. Please do not report security vulnerabilities on the public GitHub issue tracker. The [Responsible Disclosure Program](https://auth0.com/whitehat) details the procedure for disclosing security issues.

## What is Auth0?

Auth0 helps you to:

* Add authentication with [multiple authentication sources](https://docs.auth0.com/identityproviders), either social like **Google, Facebook, Microsoft Account, LinkedIn, GitHub, Twitter, Box, Salesforce, amont others**, or enterprise identity systems like **Windows Azure AD, Google Apps, Active Directory, ADFS or any SAML Identity Provider**.
* Add authentication through more traditional **[username/password databases](https://docs.auth0.com/mysql-connection-tutorial)**.
* Add support for **[linking different user accounts](https://docs.auth0.com/link-accounts)** with the same user.
* Support for generating signed [Json Web Tokens](https://docs.auth0.com/jwt) to call your APIs and **flow the user identity** securely.
* Analytics of how, when and where users are logging in.
* Pull data from other sources and add it to the user profile, through [JavaScript rules](https://docs.auth0.com/rules).

## Create a free account in Auth0

1. Go to [Auth0](https://auth0.com) and click Sign Up.
2. Use Google, GitHub or Microsoft Account to login.

## Author

[Auth0](https://auth0.com)

## License

This project is licensed under the MIT license. See the [LICENSE](LICENSE.txt) file for more info.
