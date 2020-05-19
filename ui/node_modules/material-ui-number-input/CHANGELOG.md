# Change Log

## [v2.0.0](https://github.com/NoHomey/material-ui-number-input/releases/tag/2.0.0)

### Properties

- `value` is now of type 'string'
- `onChange` changed signature to `function (event: React.FormEvent, value: string, complete: boolean) => void`
- `error` is new prop of type `React.PropTypes.oneOf(['none', 'invalidSymbol', 'incompleteNumber', 'singleMinus', 'singleFloatingPoint', 'singleZero', 'min', 'max', 'required'])` and it's the controlled error so `onError` can be called only when error changes. Default value is `'none'`

### Changes

- replacing state double with controlled input
- `value` is now watched for changes
- `onChange` is now called every time when input value must change, third argument provides infomration is value a complete number.

### Bug fixes

- `onKeyDown` is now called when `.` and `-` are pressed and emitted error is `'incompleteNumber'`

### Implementation

- `shouldComponentUpdate` is now not overrided

## [v3.0.0](https://github.com/NoHomey/material-ui-number-input/releases/tag/3.0.0)

### Properties

#### Deprecated

- `showDefaultValue`
- `error`

#### New

- `defaultValue` is of type `number` and is the same as `TextField` and `input` `defaultValue` prop
- `onValid`is function with signature `function(value: number) => void` called when input's value is a valid number
- `useStrategy` is of type `React.PropTypes.oneOf(['ignore', 'warn', 'allow'])` with defualt value `'allow'` and sets used error strategy refer to [Strategy](https://github.com/NoHomey/material-ui-number-input/#strategies) and [Errors](https://github.com/NoHomey/material-ui-number-input/#errors) 

#### Changed

- `onError` signature changed to `function(event: React.FromEvent, value: string) => void`
- `onChange` signature changed to `function(error: 'none' | 'invalidSymbol' | 'incompleteNumber' | 'singleMinus' | 'singleFloatingPoint' | 'singleZero'| 'min' | 'max' | 'required' | 'clean';) => void`

### Errors

- `'clean'` equivalent of `'required'` when `required` prop is `false`

### Implementation

- `error` is moved from `props` to `state`
- re-exposing public method `getInputNode` of `TextField`
- using polyfillied Object.assign ('object-assign')

## [v3.1.0](https://github.com/NoHomey/material-ui-number-input/releases/tag/3.1.0)

### Properties


#### Deprecated

- `useStrategy` has been renamed to `strategy`

### README changes

## [v3.1.2](https://github.com/NoHomey/material-ui-number-input/releases/tag/3.1.2)

### Bug fixes

- fixing when `event.preventDefault()` is called and when `event` is delegated

## [v3.2.0](https://github.com/NoHomey/material-ui-number-input/releases/tag/3.2.0)

### Breaking changes

- When `strategy` is `'ignore'` and `error` is `min` or `max` instead of clearing input field it's value is overwritten with `String(props[error])`

## [v4.0.0](https://github.com/NoHomey/material-ui-number-input/releases/tag/4.0.0)

### Bug fixes

- Fixing all bugs introduced after `v3.0.0` which prevented valid numbers to be entered when `strategy` is `'ignore'` or `'warn'`.
- Fixing a bug after `v3.2.0`. Now `onValid` is emitted when input's value is beeing overwritten when ''min'' or `'max'` errors are cathced and `strategy` is not `'allow'`.
- Fixing a bug after `v3.2.0` where input value was not overwritten when `'min'` or `'max'` errors are cathced and `strategy` is not `'allow'` and input is `uncontrolled`.
- Fixing a bug where input's value is not beeing validated before and after initial `render` depending on that is the input `controlled` or not.

## [v4.0.1](https://github.com/NoHomey/material-ui-number-input/releases/tag/4.0.1)

### Bug fixes

- Fixing a bug where valid numbers are prevented from beeing entered. This bug occures when `(min * 10) < max` (not fixed in `v4.0.0`)
- Fixing a bug where valid numbers are prevented from beeing entered. This bug occures when `Number(checkedValue) === 0` (not fixed in `v4.0.0`)

## [v4.0.2](https://github.com/NoHomey/material-ui-number-input/releases/tag/4.0.2)

### Bug fixes

- Ensure `onBlur` is called if a handler is passed via `props`

### New

- Adding new `public` method `getTextField` with signature `() => TextField` which returns underling `TextField` `ref`

## [v5.0.0](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.0)

### Properties

#### New

- `onRequestValue`is function with signature `function(value: string) => void` called with correct number value when  `strategy` is `'warn'` or `'ignore'` and `value` is provided.

### Implementation

- Droping alot of the logic for correcting value when `strategy` is `'warn'` or `'ignore'` and simplified it by introducing new prop `onRequestValue` which should ensure correct behavior when consumed by third party libraries such as `'react-material-ui-keyboard'` 

## [v5.0.1](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.1)
 
 - Re-exporting `default` from `'./NumberInput'`

## [v5.0.2](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.2)

- Using npm badge for README.md#Install

## [v5.0.3](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.3)

- Fixing spelling bug onReqestValue -> onRequestValue, Opps sry ...

## [v5.0.4](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.4)

- Performance improvment

## [v5.0.5](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.5)

- Performance improvments and memory optimizations

### Bug fixes

- If `strategy` is not `'allow'` and entered number is less than `min` but it is decimal number, input value will be overriden if it's not controlled else `onRequestValue` will be emitted  and `'min'` error will be emitted if `strategy` is `'warn'` 

- If `strategy` is not `'allow'` and `min` is greater than `0` and entered number is `0`, input value will be overriden if it's not controlled else `onRequestValue` will be emitted  and `'min'` error will be emitted if `strategy` is `'warn'` 

## [v5.0.6](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.6)

## TypeScript Users Only

- `propTypes` is now `React.ValidationMap<NumberInputProps>` instead of just `Object` making `React.createElement` callable

## [v5.0.7](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.7)

### Bug fixes

- Fixing a bug which caused input value overriding to be skipped when `strategy` is `'ignore'` instead of when it's `'allow'`

## [v5.0.8](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.8)

- NPM dependencies update


## [v5.0.9](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.9)

- NPM dependencies update
- Moving to TypeScript v2 and replacing typings with @types

## [v5.0.10](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.10)

### Bug fixes

- Fixing when `'singleZero'` is emitted. `'singleZero'` is emited accordingly to [Errors](https://github.com/NoHomey/material-ui-number-input#singlezero) now. In all other cases when `'singleZero'` was emitted now `'invalidSymbol'` is emitted instead.

## [v5.0.11](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.11)

### Bug fixes

- Fixing `'allow'` to properly be masked as `'min'` when error is emitted `onBlur`.
- If there is error `onBlur` it will be emitted no matter dose the strategy is `'warn'` or `'allow'`.

## [v5.0.12](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.12)

### Bug fixes

- Fixing, in addition of `value` the following `props` are checked for changes: `min`, `max`,`required` and `strategy` before take decision to call `onError`, `onValid` and `onRequestValue` accordingly when `componentWillReceiveProps`.

## [v5.0.13](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.13)

### Bug fixes

- Fixing the bug that should have been fixed with `v5.0.12` and the one introduced with `v5.0.12` which caused events to be one state behind actual input.

## [v5.0.14](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.14)

### Bug fixes

- Fixing a bug which caused `onError` to be called based on old `props`.
- Fixing the [bug](https://github.com/NoHomey/material-ui-number-input/blob/master/CHANGELOG.md#bug-fixes-9) that should be fixed with `5.0.12` 

## [v5.0.15](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.15)

### Bug fixes

- Fixing `Cannot resolve module 'material-ui-number-input'` due to `$ npm run npm` command has not been ran before releasing previos version.

## [v5.0.16](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.16)

### Properties

- Displayed by the browser keyboard is now configurable (adding support for the `input` `inputMode` prop)

## [v5.0.17](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.17)

### Bug fixes

- Allowing negative min values, closes #7

## [v5.0.18](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.18)

- Syncing npm and github versions.

## [v5.0.19](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.19)

### Bug fixes

- Fixing empty npm package, closes #9

## [v5.0.20](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.20)

- Dummy version

## [v5.0.21](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.21)

- Shipping #13 (Fixes #12)

## [v5.0.22](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.22)

### Bug fixes

- Fixed:

```
Uncaught TypeError: value.match is not a function
    at Function.NumberInput.validateValue (NumberInput.js:103)
```

(closed #14)

## [v5.0.23](https://github.com/NoHomey/material-ui-number-input/releases/tag/5.0.23)

- Updating package dependencies

- Switching from `React.PropTypes` to `prop-types` package


