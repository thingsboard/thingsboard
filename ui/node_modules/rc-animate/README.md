# rc-animate
---

animate react element easily

[![NPM version][npm-image]][npm-url]
[![build status][travis-image]][travis-url]
[![Test coverage][coveralls-image]][coveralls-url]
[![gemnasium deps][gemnasium-image]][gemnasium-url]
[![node version][node-image]][node-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/rc-animate.svg?style=flat-square
[npm-url]: http://npmjs.org/package/rc-animate
[travis-image]: https://img.shields.io/travis/react-component/animate.svg?style=flat-square
[travis-url]: https://travis-ci.org/react-component/animate
[coveralls-image]: https://img.shields.io/coveralls/react-component/animate.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/react-component/animate?branch=master
[gemnasium-image]: http://img.shields.io/gemnasium/react-component/animate.svg?style=flat-square
[gemnasium-url]: https://gemnasium.com/react-component/animate
[node-image]: https://img.shields.io/badge/node.js-%3E=_0.10-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/rc-animate.svg?style=flat-square
[download-url]: https://npmjs.org/package/rc-animate

## Feature

* support ie8,ie8+,chrome,firefox,safari

## install

[![rc-animate](https://nodei.co/npm/rc-animate.png)](https://npmjs.org/package/rc-animate)

## Usage

```jsx
import Animate from 'rc-animate';

ReactDOM.render(
  <Animate animation={{ ... }}>
    <p key="1">1</p>
    <p key="2">2</p>
  </Animate>
, mountNode);
```

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
          <td>component</td>
          <td>React.Element/String</td>
          <td>'span'</td>
          <td>wrap dom node or component for children. set to '' if you do not wrap for only one child</td>
        </tr>
        <tr>
          <td>componentProps</td>
          <td>Object</td>
          <td>{}</td>
          <td>extra props that will be passed to component</td>
        </tr>
        <tr>
          <td>showProp</td>
          <td>String</td>
          <td></td>
          <td>using prop for show and hide. [demo](http://react-component.github.io/animate/examples/hide-todo.html) </td>
        </tr>
        <tr>
          <td>exclusive</td>
          <td>Boolean</td>
          <td></td>
          <td>whether allow only one set of animations(enter and leave) at the same time. </td>
        </tr>
        <tr>
          <td>transitionName</td>
          <td>String|Object</td>
          <td></td>
          <td>specify corresponding css, see ReactCSSTransitionGroup</td>
        </tr>
       <tr>
         <td>transitionAppear</td>
         <td>Boolean</td>
         <td>false</td>
         <td>whether support transition appear anim</td>
       </tr>
        <tr>
          <td>transitionEnter</td>
          <td>Boolean</td>
          <td>true</td>
          <td>whether support transition enter anim</td>
        </tr>
       <tr>
         <td>transitionLeave</td>
         <td>Boolean</td>
         <td>true</td>
         <td>whether support transition leave anim</td>
       </tr>
       <tr>
         <td>onEnd</td>
         <td>function(key:String, exists:Boolean)</td>
         <td>true</td>
         <td>animation end callback</td>
       </tr>
        <tr>
          <td>animation</td>
          <td>Object</td>
          <td>{}</td>
          <td>
            to animate with js. see animation format below.
          </td>
        </tr>
    </tbody>
</table>

### animation format

with appear, enter and leave as keys. for example:

```js
  {
    appear: function(node, done){
      node.style.display='none';
      $(node).slideUp(done);
      return {
        stop:function(){
          // jq will call done on finish
          $(node).stop(true);
        }
      };
    },
    enter: function(){
      this.appear.apply(this,arguments);
    },
    leave: function(node, done){
      node.style.display='';
      $(node).slideDown(done);
      return {
        stop:function(){
          // jq will call done on finish
          $(node).stop(true);
        }
      };              
    }
  }
```

## Development

```
npm install
npm start
```

## Example

http://localhost:8200/examples/index.md

online example: http://react-component.github.io/animate/examples/

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

rc-animate is released under the MIT license.
