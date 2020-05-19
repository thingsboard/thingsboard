# rc-util

Common Utils For React Component.

[![NPM version][npm-image]][npm-url]
[![David dm][david-dm-image]][david-dm-url]
[![node version][node-image]][node-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/rc-util.svg?style=flat-square
[npm-url]: http://npmjs.org/package/rc-util
[travis-image]: https://img.shields.io/travis/react-component/util.svg?style=flat-square
[travis-url]: https://travis-ci.org/react-component/util
[coveralls-image]: https://img.shields.io/coveralls/react-component/util.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/react-component/util?branch=master
[david-dm-image]: https://img.shields.io/david/react-component/util.svg
[david-dm-url]: https://david-dm.org/react-component/util
[node-image]: https://img.shields.io/badge/node.js-%3E=_0.10-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/rc-util.svg?style=flat-square
[download-url]: https://npmjs.org/package/rc-util

## Install

[![rc-util](https://nodei.co/npm/rc-util.png)](https://npmjs.org/package/rc-util)

## API

### createChainedFunction

> (...functions): Function

Create a function which will call all the functions with it's arguments from left to right.

```jsx
import createChainedFunction from 'rc-util/lib/createChainedFunction';
```

### deprecated

> (prop: string, instead: string, component: string): void

Log an error message to warn developers that `prop` is deprecated.

```jsx
import deprecated from 'rc-util/lib/deprecated';
```

### getContainerRenderMixin

> (config: Object): Object

To generate a mixin which will render specific component into specific container automatically.

```jsx
import getContainerRenderMixin from 'rc-util/lib/getContainerRenderMixin';
```

Fields in `config` and their meanings.

| Field | Type | Description | Default |
|-------|------|-------------|---------|
| autoMount | boolean | Whether to render component into container automatically | true |
| autoDestroy | boolean | Whether to remove container automatically while the component is unmounted | true |
| isVisible | (instance): boolean | A function to get current visibility of the component | - |
| isForceRender | (instance): boolean | A function to determine whether to render popup even it's not visible | - |
| getComponent | (instance, extra): ReactNode | A function to get the component which will be rendered into container | - |
| getContainer | (instance): HTMLElement | A function to get the container | |

### Portal

Render children to the specific container;

```jsx
import Portal from 'rc-util/lib/Portal';
```

Props:

| Prop | Type | Description | Default |
|-------|------|-------------|---------|
| children | ReactChildren | Content render to the container | - |
| getContainer | (): HTMLElement  | A function  to get the container | - |


### getScrollBarSize

> (fresh?: boolean): number

Get the width of scrollbar.

```jsx
import getScrollBarSize from 'rc-util/lib/getScrollBarSize';
```

### guid

> (): string

To generate a global unique id across current application.

```jsx
import guid from 'rc-util/lib/guid';
```

### pickAttrs

> (props: Object): Object

Pick valid HTML attributes and events from props.

```jsx
import pickAttrs from 'rc-util/lib/pickAttrs';
```

### warn

> (msg: string): void

A shallow wrapper of `console.warn`.

```jsx
import warn from 'rc-util/lib/warn';
```

### warning

> (valid: boolean, msg: string): void

A shallow wrapper of [warning](https://github.com/BerkeleyTrue/warning), but only warning once for the same message.

```jsx
import warning, { noteOnce } from 'rc-util/lib/warning';

warning(false, '[antd Component] test hello world');

// Low level note
noteOnce(false, '[antd Component] test hello world');
```

### Children

A collection of functions to operate React elements' children.

#### Children/mapSelf

> (children): children

Return a shallow copy of children.

```jsx
import mapSelf from 'rc-util/lib/Children/mapSelf';
```

#### Children/toArray

> (children: ReactNode[]): ReactNode[]

Convert children into an array.

```jsx
import toArray from 'rc-util/lib/Children/toArray';
```

### Dom

A collection of functions to operate DOM elements.

#### Dom/addEventlistener

> (target: ReactNode, eventType: string, listener: Function): { remove: Function }

A shallow wrapper of [add-dom-event-listener](https://github.com/yiminghe/add-dom-event-listener).

```jsx
import addEventlistener from 'rc-util/lib/Dom/addEventlistener';
```

#### Dom/canUseDom

> (): boolean

Check if DOM is available.

```jsx
import canUseDom from 'rc-util/lib/Dom/canUseDom';
```

#### Dom/class

A collection of functions to operate DOM nodes' class name.

* `hasClass(node: HTMLElement, className: string): boolean`
* `addClass(node: HTMLElement, className: string): void`
* `removeClass(node: HTMLElement, className: string): void`

```jsx
import cssClass from 'rc-util/lib/Dom/class;
```

#### Dom/contains

> (root: HTMLElement, node: HTMLElement): boolean

Check if node is equal to root or in the subtree of root.

```jsx
import contains from 'rc-util/lib/Dom/contains';
```

#### Dom/css

A collection of functions to get or set css styles.

* `get(node: HTMLElement, name?: string): any`
* `set(node: HTMLElement, name?: string, value: any) | set(node, object)`
* `getOuterWidth(el: HTMLElement): number`
* `getOuterHeight(el: HTMLElement): number`
* `getDocSize(): { width: number, height: number }`
* `getClientSize(): { width: number, height: number }`
* `getScroll(): { scrollLeft: number, scrollTop: number }`
* `getOffset(node: HTMLElement): { left: number, top: number }`

```jsx
import css from 'rc-util/lib/Dom/css';
```

#### Dom/focus

A collection of functions to operate focus status of DOM node.

* `saveLastFocusNode(): void`
* `clearLastFocusNode(): void`
* `backLastFocusNode(): void`
* `getFocusNodeList(node: HTMLElement): HTMLElement[]` get a list of focusable nodes from the subtree of node.
* `limitTabRange(node: HTMLElement, e: Event): void`

```jsx
import focus from 'rc-util/lib/Dom/focus';
```

#### Dom/support

> { animation: boolean | Object, transition: boolean | Object }

A flag to tell whether current environment supports `animationend` or `transitionend`.

```jsx
import support from 'rc-util/lib/Dom/support';
```

### KeyCode

> Enum

Enum of KeyCode, please check the [definition](https://github.com/react-component/util/blob/master/src/KeyCode.js) of it.

```jsx
import KeyCode from 'rc-util/lib/KeyCode';
```

#### KeyCode.isTextModifyingKeyEvent

> (e: Event): boolean

Whether text and modified key is entered at the same time.

#### KeyCode.isCharacterKey

> (keyCode: KeyCode): boolean

Whether character is entered.

### switchScrollingEffect

> (close: boolean) => void

improve shake when page scroll bar hidden

`switchScrollingEffect` change body style, and add a class `ant-scrolling-effect` when called, so if you page look abnormal, please check this

```js
import switchScrollingEffect from "./src/switchScrollingEffect";

switchScrollingEffect();
```

## License

rc-util is released under the MIT license.
