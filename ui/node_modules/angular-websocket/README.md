<p align="center">
  <a href="https://angularclass.com" target="_blank">
    <img src="https://cloud.githubusercontent.com/assets/1016365/10355203/f50e880c-6d1d-11e5-8f59-d0d8c0870739.png" alt="Angular Websocket" width="500" height="320"/>
  </a>
</p>


# Angular Websocket [![Join Slack](https://img.shields.io/badge/slack-join-brightgreen.svg)](https://angularclass.com/slack-join) [![Join the chat at https://gitter.im/AngularClass/angular-websocket](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/AngularClass/angular-websocket?utm_campaign=pr-badge&utm_content=badge&utm_medium=badge&utm_source=badge) [![gdi2290/angular-websocket API Documentation](https://www.omniref.com/github/gdi2290/angular-websocket.png)](https://www.omniref.com/github/gdi2290/angular-websocket)

[![Travis](https://img.shields.io/travis/gdi2290/angular-websocket.svg?style=flat)](https://travis-ci.org/gdi2290/angular-websocket)
[![Bower](https://img.shields.io/bower/v/angular-websocket.svg?style=flat)](https://github.com/gdi2290/angular-websocket)
[![npm](https://img.shields.io/npm/v/angular-websocket.svg?style=flat)](https://www.npmjs.com/package/angular-websocket)
[![Dependency Status](https://david-dm.org/gdi2290/angular-websocket.svg)](https://david-dm.org/gdi2290/angular-websocket)
[![devDependency Status](https://david-dm.org/gdi2290/angular-websocket/dev-status.svg)](https://david-dm.org/gdi2290/angular-websocket#info=devDependencies)
[![NPM](https://nodei.co/npm/angular-websocket.svg?downloads=true&downloadRank=true&stars=true)](https://nodei.co/npm/angular-websocket/)

### Status: Looking for feedback about new API changes

An AngularJS 1.x WebSocket service for connecting client applications to servers.

## How do I add this to my project?

You can download angular-websocket by:

* (prefered) Using bower and running `bower install angular-websocket --save`
* Using npm and running `npm install angular-websocket --save`
* Downloading it manually by clicking [here to download development unminified version](https://raw.github.com/gdi2290/angular-websocket/master/angular-websocket.js)
* CDN for development `https://rawgit.com/gdi2290/angular-websocket/v1.0.9/angular-websocket.js`
* CDN for production `https://cdn.rawgit.com/gdi2290/angular-websocket/v1.0.9/angular-websocket.min.js`

## Usage

```html
  <script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.3.15/angular.min.js"></script>
  <script src="bower_components/angular-websocket/angular-websocket.js"></script>
  <section ng-controller="SomeController">
    <ul ng-repeat="data in MyData.collection track by $index" >
      <li> {{ data }} </li>
    </ul>
  </section>
  <script>
    angular.module('YOUR_APP', [
      'ngWebSocket' // you may also use 'angular-websocket' if you prefer
    ])
    //                          WebSocket works as well
    .factory('MyData', function($websocket) {
      // Open a WebSocket connection
      var dataStream = $websocket('ws://website.com/data');

      var collection = [];

      dataStream.onMessage(function(message) {
        collection.push(JSON.parse(message.data));
      });

      var methods = {
        collection: collection,
        get: function() {
          dataStream.send(JSON.stringify({ action: 'get' }));
        }
      };

      return methods;
    })
    .controller('SomeController', function ($scope, MyData) {
      $scope.MyData = MyData;
    });
  </script>
```

## API

### Factory: `$websocket` (in module `ngWebSocket`)

returns instance of $Websocket

### Methods

name        | arguments                                              | description
------------|--------------------------------------------------------|------------
$websocket <br>_constructor_ | url:String                              | Creates and opens a [WebSocket](http://mdn.io/API/WebSocket) instance. <br>`var ws = $websocket('ws://foo');`
send        | data:String,Object returns                             | Adds data to a queue, and attempts to send if socket is ready. Accepts string or object, and will stringify objects before sending to socket.
onMessage   | callback:Function <br>options{filter:String,RegExp, autoApply:Boolean=true} | Register a callback to be fired on every message received from the websocket, or optionally just when the message's `data` property matches the filter provided in the options object. Each message handled will safely call `$rootScope.$digest()` unless `autoApply` is set to `false in the options. Callback gets called with a [MessageEvent](https://developer.mozilla.org/en-US/docs/Web/API/MessageEvent?redirectlocale=en-US&redirectslug=WebSockets%2FWebSockets_reference%2FMessageEvent) object.
onOpen      | callback:Function                                      | Function to be executed each time a socket connection is opened for this instance.
onClose     | callback:Function                                      | Function to be executed each time a socket connection is closed for this instance.
onError     | callback:Function                                      | Function to be executed each time a socket connection has an Error for this instance.
close       | force:Boolean:_optional_                               | Close the underlying socket, as long as no data is still being sent from the client. Optionally force close, even if data is still being sent, by passing `true` as the `force` parameter. To check if data is being sent, read the value of `socket.bufferedAmount`.

### Properties
name               | type             | description
-------------------|------------------|------------
socket             | window.WebSocket | [WebSocket](http://mdn.io/API/WebSocket) instance.
sendQueue          | Array<function>  | Queue of `send` calls to be made on socket when socket is able to receive data. List is populated by calls to the `send` method, but this array can be spliced if data needs to be manually removed before it's been sent to a socket. Data is removed from the array after it's been sent to the socket.
onOpenCallbacks    | Array<function>  | List of callbacks to be executed when the socket is opened, initially or on re-connection after broken connection. Callbacks should be added to this list through the `onOpen` method.
onMessageCallbacks | Array<function>  | List of callbacks to be executed when a message is received from the socket. Callbacks should be added via the `onMessage` method.
onErrorCallbacks   | Array<function>  | List of callbacks to be executed when an error is received from the socket. Callbacks should be added via the `onError` method.
onCloseCallbacks   | Array<function>  | List of callbacks to be executed when the socket is closed. Callbacks should be added via the `onClose` method.
readyState         | Number:readonly  | Returns either the readyState value from the underlying WebSocket instance, or a proprietary value representing the internal state of the lib, e.g. if the lib is in a state of re-connecting.
initialTimeout     | Number           | The initial timeout, should be set at the outer limits of expected response time for the service. For example, if your service responds in 1ms on average but in 10ms for 99% of requests, then set to 10ms.
maxTimeout         | Number           | Should be as low as possible to keep your customers happy, but high enough that the system can definitely handle requests from all clients at that sustained rate.

### CancelablePromise

This type is returned from the `send()` instance method of $websocket, inherits from [$q.defer().promise](https://ng-click.com/$q).

### Methods

name        | arguments                                              | description
------------|--------------------------------------------------------|------------
cancel      | | Alias to `deferred.reject()`, allows preventing an unsent message from being sent to socket for any arbitrary reason.
then        | resolve:Function, reject:Function | Resolves when message has been passed to socket, presuming the socket has a `readyState` of 1. Rejects if the socket is hopelessly disconnected now or in the future (i.e. the library is no longer attempting to reconnect). All messages are immediately rejected when the library has determined that re-establishing a connection is unlikely.


### Service: `$websocketBackend` (in module `ngWebSocketMock`)

Similar to [`httpBackend`](https://ng-click.com/$httpBackend) mock in
AngularJS's `ngMock` module. You can use `ngWebSocketMock` to mock a websocket
server in order to test your applications:

```javascript
    var $websocketBackend;

    beforeEach(angular.mock.module('ngWebSocket', 'ngWebSocketMock');

    beforeEach(inject(function (_$websocketBackend_) {
      $websocketBackend = _$websocketBackend_;

      $websocketBackend.mock();
      $websocketBackend.expectConnect('ws://localhost:8080/api');
      $websocketBackend.expectSend({data: JSON.stringify({test: true})});
    }));
```

### Methods

name                           | arguments  | description
-------------------------------|------------|-----------------------------------
flush                          |            | Executes all pending requests
expectConnect                  | url:String | Specify the url of an expected WebSocket connection
expectClose                    |            | Expect "close" to be called on the WebSocket
expectSend                     | msg:String | Expectation of send to be called, with required message
verifyNoOutstandingExpectation |            | Makes sure all expectations have been satisfied, should be called in afterEach
verifyNoOutstandingRequest     |            | Makes sure no requests are pending, should be called in afterEach

## Frequently asked questions

 * *Q.*: What if the browser doesn't support WebSockets?
 * *A.*: This module will not help; it does not have a fallback story for browsers that do not support WebSockets. Please check your browser target support [here](http://caniuse.com/#feat=websockets) and to include fallback support.

## Development

```shell
$ npm install
$ bower install
```

## Changelog
[Changelog](https://github.com/gdi2290/angular-websocket/blob/master/CHANGELOG.md)

### Unit Tests
`$ npm test` Run karma in Chrome, Firefox, and Safari

### Manual Tests

In the project root directory open `index.html` in the example folder or browserify example

### Distribute
`$ npm run dist` Builds files with uglifyjs

### Support, Questions, or Feedback
> Contact us anytime for anything about this repo or Angular 2

* [Slack: AngularClass](https://angularclass.com/slack-join)
* [Gitter: angularclass/angular-websocket](https://gitter.im/AngularClass/angular-websocket)
* [Twitter: @AngularClass](https://twitter.com/AngularClass)


## TODO
 * Allow JSON if object is sent
 * Allow more control over $digest cycle per WebSocket instance
 * Add Angular interceptors
 * Add .on(event)
 * Include more examples of patterns for realtime Angular apps
 * Allow for optional configuration object in $websocket constructor
 * Add W3C Websocket support
 * Add socket.io support
 * Add SockJS support
 * Add Faye support
 * Add PubNub support
___

enjoy — **AngularClass** 

<br><br>

[![AngularClass](https://cloud.githubusercontent.com/assets/1016365/9863770/cb0620fc-5af7-11e5-89df-d4b0b2cdfc43.png  "Angular Class")](https://angularclass.com)
##[AngularClass](https://angularclass.com)
> Learn AngularJS, Angular 2, and Modern Web Development form the best.
> Looking for corporate Angular training, want to host us, or Angular consulting? patrick@angularclass.com


## License
[MIT](https://github.com/angularclass/angular-websocket/blob/master/LICENSE)
