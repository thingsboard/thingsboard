# rc-trigger
---

React Trigger Component


[![NPM version][npm-image]][npm-url]
[![build status][travis-image]][travis-url]
[![Test coverage][coveralls-image]][coveralls-url]
[![gemnasium deps][gemnasium-image]][gemnasium-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/rc-trigger.svg?style=flat-square
[npm-url]: http://npmjs.org/package/rc-trigger
[travis-image]: https://img.shields.io/travis/react-component/trigger.svg?style=flat-square
[travis-url]: https://travis-ci.org/react-component/trigger
[coveralls-image]: https://img.shields.io/coveralls/react-component/trigger.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/react-component/trigger?branch=master
[gemnasium-image]: http://img.shields.io/gemnasium/react-component/trigger.svg?style=flat-square
[gemnasium-url]: https://gemnasium.com/react-component/trigger
[node-image]: https://img.shields.io/badge/node.js-%3E=_0.10-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/rc-trigger.svg?style=flat-square
[download-url]: https://npmjs.org/package/rc-trigger


## Browser Support

|![IE](https://raw.github.com/alrra/browser-logos/master/internet-explorer/internet-explorer_48x48.png) | ![Chrome](https://raw.github.com/alrra/browser-logos/master/chrome/chrome_48x48.png) | ![Firefox](https://raw.github.com/alrra/browser-logos/master/firefox/firefox_48x48.png) | ![Opera](https://raw.github.com/alrra/browser-logos/master/opera/opera_48x48.png) | ![Safari](https://raw.github.com/alrra/browser-logos/master/safari/safari_48x48.png)|
| --- | --- | --- | --- | --- |
| IE 8+ ✔ | Chrome 31.0+ ✔ | Firefox 31.0+ ✔ | Opera 30.0+ ✔ | Safari 7.0+ ✔ |


## Development

```
npm install
npm start
```

## Example

http://localhost:8200/examples/

online example: http://react-component.github.io/trigger/examples/

## Feature

* supported on IE 8+, Chrome, Firefox, and Safari

## install

[![rc-trigger](https://nodei.co/npm/rc-trigger.png)](https://npmjs.org/package/rc-trigger)

## Usage

Include the default [styling](https://github.com/react-component/trigger/blob/master/assets/index.less#L4:L11) and then:

```js
import React from 'react';
import ReactDOM from 'react-dom';
import Trigger from 'rc-trigger';

ReactDOM.render((
  <Trigger 
    action={['click']}
    popup={<span>popup</span>}
    popupAlign={{
      points: ['tl', 'bl'],
      offset: [0, 3]
    }}
  >
    <a href='#'>hover</a>
  </Trigger>
), container);
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
          <td>popupClassName</td>
          <td>string</td>
          <td></td>
          <td>additional className added to popup</td>
        </tr>
        <tr>
          <td>destroyPopupOnHide</td>
          <td>boolean</td>
          <td>false</td>
          <td>whether destroy popup when hide</td>
        </tr>
        <tr>
          <td>getPopupClassNameFromAlign</td>
          <td>getPopupClassNameFromAlign(align: Object):String</td>
          <td></td>
          <td>additional className added to popup according to align</td>
        </tr>
        <tr>
          <td>action</td>
          <td>string[]</td>
          <td>['hover']</td>
          <td>which actions cause popup shown. enum of 'hover','click','focus'</td>
        </tr>
        <tr>
          <td>mouseEnterDelay</td>
          <td>number</td>
          <td>0</td>
          <td>delay time to show when mouse enter. unit: s.</td>
        </tr>
        <tr>
          <td>mouseLeaveDelay</td>
          <td>number</td>
          <td>0.1</td>
          <td>delay time to hide when mouse leave. unit: s.</td>
        </tr>
        <tr>
          <td>popupStyle</td>
          <td>Object</td>
          <td></td>
          <td>additional style of popup</td>
        </tr>
        <tr>
          <td>prefixCls</td>
          <td>String</td>
          <td>rc-trigger-popup</td>
          <td>prefix class name</td>
        </tr>
        <tr>
          <td>popupTransitionName</td>
          <td>String</td>
          <td></td>
          <td>https://github.com/react-component/animate</td>
        </tr>
        <tr>
          <td>maskTransitionName</td>
          <td>String</td>
          <td></td>
          <td>https://github.com/react-component/animate</td>
        </tr>
        <tr>
          <td>onPopupVisibleChange</td>
          <td>Function</td>
          <td></td>
          <td>call when popup visible is changed</td>
        </tr>
        <tr>
          <td>mask</td>
          <td>boolean</td>
          <td>false</td>
          <td>whether to support mask</td>
        </tr>
        <tr>
          <td>maskClosable</td>
          <td>boolean</td>
          <td>true</td>
          <td>whether to support click mask to hide</td>
        </tr>
        <tr>
          <td>popupVisible</td>
          <td>boolean</td>
          <td></td>
          <td>whether popup is visible</td>
        </tr>
        <tr>
          <td>zIndex</td>
          <td>number</td>
          <td></td>
          <td>popup's zIndex</td>
        </tr>
        <tr>
          <td>defaultPopupVisible</td>
          <td>boolean</td>
          <td></td>
          <td>whether popup is visible initially</td>
        </tr>
        <tr>
          <td>popupAlign</td>
          <td>Object: alignConfig of [dom-align](https://github.com/yiminghe/dom-align)</td>
          <td></td>
          <td>popup 's align config</td>
        </tr>
        <tr>
          <td>onPopupAlign</td>
          <td>function(popupDomNode, align)</td>
          <td></td>
          <td>callback when popup node is aligned</td>
        </tr>
        <tr>
          <td>popup</td>
          <td>React.Element | function() => React.Element</td>
          <td></td>
          <td>popup content</td>
        </tr>
        <tr>
          <td>getPopupContainer</td>
          <td>getPopupContainer(): HTMLElement</td>
          <td></td>
          <td>function returning html node which will act as popup container</td>
        </tr>
        <tr>
          <td>getDocument</td>
          <td>getDocument(): HTMLElement</td>
          <td></td>
          <td>function returning document node which will be attached click event to close trigger</td>
        </tr>
        <tr>
          <td>popupPlacement</td>
          <td>string</td>
          <td></td>
          <td>use preset popup align config from builtinPlacements, can be merged by popupAlign prop</td>
        </tr>   
        <tr>
          <td>builtinPlacements</td>
          <td>object</td>
          <td></td>
          <td>builtin placement align map. used by placement prop</td>
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

rc-trigger is released under the MIT license.
