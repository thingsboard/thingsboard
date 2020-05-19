# rc-align
---

React Align Component. Wrapper around https://github.com/yiminghe/dom-align.

[![NPM version][npm-image]][npm-url]
[![build status][travis-image]][travis-url]
[![Test coverage][coveralls-image]][coveralls-url]
[![gemnasium deps][gemnasium-image]][gemnasium-url]
[![node version][node-image]][node-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/rc-align.svg?style=flat-square
[npm-url]: http://npmjs.org/package/rc-align
[travis-image]: https://img.shields.io/travis/react-component/align.svg?style=flat-square
[travis-url]: https://travis-ci.org/react-component/align
[coveralls-image]: https://img.shields.io/coveralls/react-component/align.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/react-component/align?branch=master
[gemnasium-image]: http://img.shields.io/gemnasium/react-component/align.svg?style=flat-square
[gemnasium-url]: https://gemnasium.com/react-component/align
[node-image]: https://img.shields.io/badge/node.js-%3E=_0.10-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/rc-align.svg?style=flat-square
[download-url]: https://npmjs.org/package/rc-align


## Development

```
npm install
npm start
```

## Example

http://localhost:8100/examples/

online example: http://react-component.github.io/align/examples/


## Feature

* support ie8,ie8+,chrome,firefox,safari

### Keyboard



## install

[![rc-align](https://nodei.co/npm/rc-align.png)](https://npmjs.org/package/rc-align)

## Usage

```js
var Align = require('rc-align');
var ReactDOM = require('react-dom');
ReactDOM.render(<Align align={{}} target={function(){}}><div></div></Align>, container);
```

will align child with target when mounted or align is changed

## API

### props

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
          <td>align</td>
          <td>Object</td>
          <td></td>
          <td>same with alignConfig from https://github.com/yiminghe/dom-align</td>
        </tr>
        <tr>
          <td>onAlign</td>
          <td>function(source:HTMLElement, align:Object)</td>
          <td></td>
          <td>called when align</td>
        </tr>
        <tr>
          <td>target</td>
          <td>
              function():HTMLElement || 
              { pageX: number, pageY: number } ||
              { clientX: number, clientY: number }
          </td>
          <td>function(){return window;}</td>
          <td>
            a function which returned value or point is used for target from https://github.com/yiminghe/dom-align
        </td>
        </tr>
        <tr>
          <td>monitorWindowResize</td>
          <td>Boolean</td>
          <td>false</td>
          <td>whether realign when window is resized</td>
        </tr>
    </tbody>
</table>


## License

rc-align is released under the MIT license.
