0.1.11 / 2019-03-09
==================

* Make sure `undefined` responses don't throw an exception

0.1.10 / 2018-06-05

* Fix security vulnerability. [Read more](https://github.com/auth0/angular-jwt/blob/master/SECURITY-NOTICE.md#security-vulnerability-details-for-angular-jwt--0110)

0.1.9 / 2017-01-17
==================

* Add support for RegEx in `whiteListedDomains` array

0.1.8 / 2016-11-10
==================
 
 * Add `isAuthenticated` function to `authManager`

0.1.7 / 2016-10-21
==================

  * Add `invokeRedirector` method to `authManager` to properly handle annotated `unauthenticatedRedirector` functions
  * Expose `getToken` and `redirect` from `authManager`
  * Clean up `gulpfile.js`

0.1.6 / 2016-10-13
==================

  * Add support for `requiresLogin` to be set in route configuration to protect client-side routes
  * Add event `tokenHasExpired` to allow for custom behavior when a token is expired on page refresh

0.1.0 / 2016-08-15
==================

  * Add authManager service
  * Add jwtOptionsProvider to share config between services
  * Add instructions for whitelisting domains

0.0.9 / 2015-06-02
==================

  * Merge pull request #31 from doshprompt/access-token
  * Add config option to allow token to be set as a url param instead of a header
  * Merge pull request #35 from ackerdev/master
  * Fix unix epoch expiration

0.0.8 / 2015-05-18
==================

  * Added new dist
  * Merge pull request #47 from aaronroberson/master
  * Added support for Browserify/Webpack
  * Merge pull request #41 from christopherthielen/master
  * Add a parameter `offsetSeconds` to isTokenExpired function as a "fudge-factor" in the computation.

0.0.7 / 2015-04-07
==================

  * Merge pull request #33 from dunglas/patch-1
  * Fix Auth0 link in README.md
  * Merge pull request #30 from RoberMac/master
  * UTF-8 encoding support
  * UTF-8 encoding support
  * Merge pull request #29 from lopsided/master
  * Document how to avoid sending authentication headers for template requests
  * Merge pull request #28 from ChtiDkois/patch-1
  * Little trick for utf-8 encoding support
  * Merge pull request #25 from ntotten/master
  * Update and rename LICENSE to LICENSE.txt
  * Merge pull request #24 from ntotten/master
  * Cleaned license, readme
  * Merge pull request #22 from nnjpp/patch-1
  * Update bower.json to 0.0.6
  * Updated CDN url
  * Merge pull request #20 from capesean/master
  * Using refresh token & grant type
  * Using the refreshToken in the $http call
  * Merge pull request #17 from zhuangya/npm-main
  * Merge pull request #18 from Graphicnerd/patch-1
  * Update README.md
  * add main for package.json

0.0.4 / 2014-10-06
==================

  * $http config is now sent as `config` to the tokenGetter function.
  * Update README.md

0.0.3 / 2014-10-04
==================

  * Merge pull request #3 from anongit/patch-1
  * Added missing root scope dependency.
  * Merge pull request #2 from itsananderson/examples-fix
  * Update examples to include module dependency
  * Update README.md
  * Update README.md
  * Update README.md
  * Update README.md
  * Fixed dist
  * Merge pull request #1 from gdi2290/patch-1
  * docs(readme): update examples for 1.3+
  * Added installation
  * Added v0.0.2
  * Added version
