## 2.0.0 (2016-5-21)

Features:
  - Repackage library
  - Package source files no longer duplicated

Bugfixes:
  - autoApply is undefined error
  - arraybuffer data type

BREAKING CHANGES:
  - package source files are not duplicated
    - if using files from `/`, use `/dist/` instead
      - change `/angular-websocket-mock.js` to `/dist/angular-websocket-mock.js`
      - change `/angular-websocket.js` to `/dist/angular-websocket.js`
      - change `/angular-websocket-min.js` to `/dist/angular-websocket-min.js`
      - change `/angular-websocket-min.js.map` to `/dist/angular-websocket-min.js.map`

## 1.1.0 (2016-4-4)

## 1.0.14 (2015-9-15)

## 1.0.13 (2015-6-2)
 Build:
  - UpdatedevDependencies

## 1.0.12 (2015-6-2)

Features:
  - Update to angular 1.3.15
  - Added an alwaysReconnect option
  - Can mock a websocket server for testing purpose.
  - Use Angular's `bind` polyfill

Bugfixes:
  - Pass options to the $websocket constructor
  - Fix MockWebsocket readyState and flush


## 1.0.9 (2015-2-22)

Features:
  - Export module for better browserify support

Bugfixes:
  - Missing `ws` dependency for browserify

## 1.0.8 (2015-1-29)

Features:
  - Allow .asyncApply() if useApplyAsync is true in config
  - Pass event to .notifyOpenCallbacks() callbacks
  - Tests for .bindToScope and .safeDigest

Bugfixes:
  - Bind ._connect() correctly for .reconnect()
  - Correct rootScopeFailover of $rootScope with bindToScope

## 1.0.7 (2015-1-20)

Features:
  - Named functions for debugging
  - Add .bindToScope() and expose .safeDigest() with scope/rootScopeFailover option

Bugfixes:
  - Remove default protocol

Deprecated:
  - Change $WebSocketBackend.createWebSocketBackend -> $WebSocketBackend.create

## 1.0.6 (2015-1-7)

Features:
  - Include onErrorCallbacks docs
  - Include onCloseCallbacks docs

Bugfixes:
  - Typo in _connect()

## 1.0.5 (2014-12-29)

Features:
  - NPM ignore

## 1.0.4 (2014-12-29)

Features:
  - Update bower ignore
  - Add more dot files
  - Add keywords to bower.json and package.json

## 1.0.3 (2014-12-29)

Bugfixes:
  - Module name typo in Readme

## 1.0.2 (2014-12-29)

Bugfixes:
  - have built files in root directory

Features:
  - Copy dist to root in build


## 1.0.1 (2014-12-29)

Bugfixes:
  - Outdated Dependencies

## 1.0.0 (2014-12-29)

Bugfixes:
  - Tests

Features:
  - Mocks
  - Tests with Travis CI
  - Module backend
  - Improved API
  - Docs
  - Better working examples
  - Reconnect
  - Minified with sourcemap
  - Legacy support 1.2.x with IE8
  - Upgrade tests from Jasmine 1.3 to 2.0
  - Update build
  - Include Karma for tests
  - Use ngSocket as base

## 0.0.5
  - Initial release
