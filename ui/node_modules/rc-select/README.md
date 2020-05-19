# rc-select
---

React Select

[![NPM version][npm-image]][npm-url]
[![build status][travis-image]][travis-url]
[![Test coverage][coveralls-image]][coveralls-url]
[![gemnasium deps][gemnasium-image]][gemnasium-url]
[![npm download][download-image]][download-url]

[npm-image]: http://img.shields.io/npm/v/rc-select.svg?style=flat-square
[npm-url]: http://npmjs.org/package/rc-select
[travis-image]: https://img.shields.io/travis/react-component/select.svg?style=flat-square
[travis-url]: https://travis-ci.org/react-component/select
[coveralls-image]: https://img.shields.io/coveralls/react-component/select.svg?style=flat-square
[coveralls-url]: https://coveralls.io/r/react-component/select?branch=master
[gemnasium-image]: http://img.shields.io/gemnasium/react-component/select.svg?style=flat-square
[gemnasium-url]: https://gemnasium.com/react-component/select
[node-image]: https://img.shields.io/badge/node.js-%3E=_0.10-green.svg?style=flat-square
[node-url]: http://nodejs.org/download/
[download-image]: https://img.shields.io/npm/dm/rc-select.svg?style=flat-square
[download-url]: https://npmjs.org/package/rc-select

## Screenshots

<img src="https://tfsimg.alipay.com/images/T1CUBeXa0kXXXXXXXX.png" />

## Feature

* support ie8,ie8+,chrome,firefox,safari

### Keyboard

* Open select (focus input || focus and click)
* KeyDown/KeyUp/Enter to navigate menu

## install

[![rc-select](https://nodei.co/npm/rc-select.png)](https://npmjs.org/package/rc-select)

## Usage

### basic use

```js
import Select, {Option, OptGroup} from 'rc-select';

var c = (
  <Select>
    <Option value="jack">jack</Option>
    <Option value="lucy">lucy</Option>
    <Option value="yiminghe">yiminghe</Option>
  </Select>
);
React.render(c, container);
```

## API

### Select props

| name     | description    | type     | default      |
|----------|----------------|----------|--------------|
|className | additional css class of root dom node | String | '' |
|prefixCls | prefix class | String | '' |
|animation | dropdown animation name. only support slide-up now | String | '' |
|transitionName | dropdown css animation name | String | '' |
|choiceTransitionName | css animation name for selected items at multiple mode | String | '' |
|dropdownMatchSelectWidth | whether dropdown's with is same with select | bool | true |
|dropdownClassName | additional className applied to dropdown | String | - |
|dropdownStyle | additional style applied to dropdown | Object | {} |
|dropdownAlign | additional align applied to dropdown | Object | {} |
|dropdownMenuStyle | additional style applied to dropdown menu | Object | {} |
|notFoundContent | specify content to show when no result matches. | String | 'Not Found' |
|tokenSeparators | separator used to tokenize on tag/multiple mode | string[]? |  |
|placeholder | select placeholder | React Node | |
|showSearch | whether show search input in single mode | bool | true |
|showArrow | whether show arrow in single mode | bool | true |
|allowClear | whether allowClear | bool | false |
|tags | when tagging is enabled the user can select from pre-existing options or create a new tag by picking the first choice, which is what the user has typed into the search box so far. | bool | false |
|maxTagTextLength | max tag text length to show | number | - |
|combobox | enable combobox mode(can not set multiple at the same time) | bool | false |
|multiple | whether multiple select | bool | false |
|disabled | whether disabled select | bool | false |
|filterOption | whether filter options by input value. default filter by option's optionFilterProp prop's value | bool | true/Function(inputValue:string, option:Option) |
|optionFilterProp | which prop value of option will be used for filter if filterOption is true | String | 'value' |
|optionLabelProp | which prop value of option will render as content of select | String | 'value' |
|defaultValue | initial selected option(s) | String/Array<String> | - |
|value | current selected option(s) | String/Array<String>/{key:String, label:React.Node}/Array<{key, label}> | - |
|firstActiveValue | first active value when there is no value | String/Array<String> | - |
|labelInValue| whether to embed label in value, see above value type | Bool | false |
|backfill| whether backfill select option to search input (Only works in single and combobox mode) | Bool | false |
|onChange | called when select an option or input value change(combobox) | function(value) | - |
|onSearch | called when input changed | function | - |
|onBlur | called when blur | function | - |
|onFocus | called when focus | function | - |
|onSelect | called when a option is selected. param is option's value and option instance | Function(value, option:Option) | - |
|onDeselect | called when a option is deselected. param is option's value. only called for multiple or tags | Function(value) | - |
|defaultActiveFirstOption | whether active first option by default | bool | true |
|getPopupContainer | container which popup select menu rendered into | function(trigger:Node):Node | function(){return document.body;} |
|getInputElement| customize input element | function(): Element | - |

### Option props

| name     | description    | type     | default      |
|----------|----------------|----------|--------------|
|className | additional class to option | String | '' |
|disabled | no effect for click or keydown for this item | bool | false |
|key | if react want you to set key, then key is same as value, you can omit value | String | - |
|value | default filter by this attribute. if react want you to set key, then key is same as value, you can omit value | String | - |
|title | if you are not satisfied with auto-generated `title` which is show while hovering on selected value, you can customize it with this property | String | - |


### OptGroup props

| name     | description    | type     | default      |
|----------|----------------|----------|--------------|
|label | group label | String/React.Element | - |
|key | - | String | - |
|value | default filter by this attribute. if react want you to set key, then key is same as value, you can omit value | String | - |


## Development

```
npm install
npm start
```

## Example

http://localhost:8003/examples/

online example: http://react-component.github.io/select/examples/

## Test Case

```
npm test
```

## Coverage

```
npm run coverage
```


## License

rc-select is released under the MIT license.
