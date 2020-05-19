# css-animation
---

make css animation easier

[![NPM version][npm-image]][npm-url]

[npm-image]: http://img.shields.io/npm/v/css-animation.svg?style=flat-square
[npm-url]: http://npmjs.org/package/css-animation

## Development

```
npm install
npm start
```

## Example

http://localhost:9001/examples/

online example: http://yiminghe.github.io/css-animation/


## Feature

* support ie8,ie8+,chrome,firefox,safari

## install

[![css-animation](https://nodei.co/npm/css-animation.png)](https://npmjs.org/package/css-animation)

## Usage

```js
var anim = require('css-animation');
anim(el,animationName,function(){});
```

## API

### void anim(el:DOMElement, animationName:String, callback:Function)

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
          <td>el</td>
          <td>DOMElement</td>
          <td></td>
          <td>dom element to be animated</td>
        </tr>
        <tr>
          <td>animationName</td>
          <td>String|Object</td>
          <td></td>
          <td>will add animationName (if string) or animationName.name (if object) as class to el, then setTimeout 0 to add ${animationName}-active (if string) or animationName.active (if object) to el</td>
        </tr>
        <tr>
          <td>callback</td>
          <td>Function</td>
          <td></td>
          <td>triggered when anim caused by animationName is done</td>
        </tr>
    </tbody>
</table>

## Test Case

```
npm test
npm run chrome-test
```

## Coverage

```
npm run coverage
```

open coverage/ dir

## License

css-animation is released under the MIT license.
