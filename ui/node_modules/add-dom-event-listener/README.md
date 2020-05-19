# add-dom-event-listener
add dom event listener. normalize ie and others. port from https://github.com/modulex/event-dom

[![NPM version][npm-image]][npm-url]
[![build status][travis-image]][travis-url]
[![Test coverage][coveralls-image]][coveralls-url]
[![gemnasium deps][gemnasium-image]][gemnasium-url]
[![node version][node-image]][node-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/add-dom-event-listener.svg?style=flat-square
[npm-url]: http://npmjs.org/package/add-dom-event-listener
[travis-image]: https://img.shields.io/travis/yiminghe/add-dom-event-listener.svg?style=flat-square
[travis-url]: https://travis-ci.org/yiminghe/add-dom-event-listener
[coveralls-image]: https://img.shields.io/coveralls/yiminghe/add-dom-event-listener.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/yiminghe/add-dom-event-listener?branch=master
[gemnasium-image]: http://img.shields.io/gemnasium/yiminghe/add-dom-event-listener.svg?style=flat-square
[gemnasium-url]: https://gemnasium.com/yiminghe/add-dom-event-listener
[node-image]: https://img.shields.io/badge/node.js-%3E=_4.0.0-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/add-dom-event-listener.svg?style=flat-square
[download-url]: https://npmjs.org/package/add-dom-event-listener

## examples

```js
var addEventListener = require('add-dom-event-listener');
var handler = addEventListener(document.body, 'click', function(e){
  console.log(e.target); // works for ie
  
  console.log(e.nativeEvent); // native dom event
});
handler.remove(); // detach event listener
```

## history

### 1.1.0

- allow event options