# dom-scroll-into-view
---

scroll node in contain to make node visible

[![NPM version][npm-image]][npm-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/dom-scroll-into-view.svg?style=flat-square
[npm-url]: http://npmjs.org/package/dom-scroll-into-view
[travis-image]: https://img.shields.io/travis/react-component/dom-scroll-into-view.svg?style=flat-square
[travis-url]: https://travis-ci.org/react-component/dom-scroll-into-view
[coveralls-image]: https://img.shields.io/coveralls/react-component/dom-scroll-into-view.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/react-component/dom-scroll-into-view?branch=master
[gemnasium-image]: http://img.shields.io/gemnasium/react-component/dom-scroll-into-view.svg?style=flat-square
[gemnasium-url]: https://gemnasium.com/react-component/dom-scroll-into-view
[node-image]: https://img.shields.io/badge/node.js-%3E=_0.10-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/dom-scroll-into-view.svg?style=flat-square
[download-url]: https://npmjs.org/package/dom-scroll-into-view


## install

[![dom-scroll-into-view](https://nodei.co/npm/dom-scroll-into-view.png)](https://npmjs.org/package/dom-scroll-into-view)

## Usage

```js
var scrollIntoView = require('dom-scroll-into-view');
scrollIntoView(source,container,config);
```
## Development

```
npm install
npm start
```

## Example

http://localhost:8000/examples/

online example: http://yiminghe.github.io/dom-scroll-into-view/

## function parameter

<table class="table table-bordered table-striped">
    <thead>
    <tr>
        <th style="width: 100px;">name</th>
        <th style="width: 50px;">type</th>
        <th style="width: 50px;">default</th>
        <th>description</th>
    </tr>
    </thead>
    <tbody>
        <tr>
          <td>source</td>
          <td>HTMLElement</td>
          <td></td>
          <td>node wanted to show</td>
        </tr>
        <tr>
          <td>container</td>
          <td>HTMLElement</td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td>config.alignWithLeft</td>
          <td>Boolean</td>
          <td></td>
          <td>whether align with left edge</td>
        </tr>
        <tr>
          <td>config.alignWithTop</td>
          <td>Boolean</td>
          <td></td>
          <td>whether align with top edge</td>
        </tr>
        <tr>
          <td>config.offsetTop</td>
          <td>Number</td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td>config.offsetLeft</td>
          <td>Number</td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td>config.offsetBottom</td>
          <td>Number</td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td>config.offsetRight</td>
          <td>Number</td>
          <td></td>
          <td></td>
        </tr>
        <tr>
          <td>config.allowHorizontalScroll</td>
          <td>Boolean</td>
          <td></td>
          <td>whether allow horizontal scroll container</td>
        </tr>
        <tr>
          <td>config.onlyScrollIfNeeded</td>
          <td>Boolean</td>
          <td></td>
          <td>whether scroll container when source is visible</td>
        </tr>
    </tbody>
</table>

## License

dom-scroll-into-view is released under the MIT license.
