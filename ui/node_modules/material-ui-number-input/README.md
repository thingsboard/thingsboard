# material-ui-number-input

The better TextField for number inputs.

# Install

[![NPM](https://nodei.co/npm/material-ui-number-input.png?downloads=true&stars=true)](https://nodei.co/npm/material-ui-number-input/)

# Changelog

**Check [Change log](CHANGELOG.md) for changes.**

# Properties

| Name                    | Type       | Default   | TextField | Description                                             |
| ----------------------- | ---------- | --------- | --------- | ------------------------------------------------------- |
| children                | *node*     |           | ✓         |                                                         |
| className               | *string*   |           | ✓         | The css class name of the root element. |
| disabled                | *bool*     | *false*   | ✓         | Disables the input field if set to true.|
| floatingLabelFixed      | *bool*     | *false*   | ✓         | If true, the floating label will float even when there is no value. |
| id                      | *string*   |           | ✓         | The id prop for the input field. |
| name                    | *string*   |           | ✓         | Name applied to the input. |
| fullWidth               | *bool*     | *false*   | ✓         | If true, the field receives the property width 100%. |
| underlineShow           | *bool*     | *true*   | ✓         | If true, shows the underline for the input field. |
| defaultValue            | *number*   |           | ✓         | The number to use for the default value. Must be in range [min, max] if any is setted. |
| strategy                | *'ignore' \| 'warn' \| 'allow'* | *'allow'* | ❌       | Strategy to use when user presses key and when value prop change it's value. |
| min                     | *number*   |           | ❌         | The number used as minimum value limit. Must be less than max. |
| max                     | *number*   |           | ❌         | The number used as maximum value limit. Must be greater than min. |
| required                 | *bool*     | *false*   | ❌         | If true and if input is left empty than instead of 'clean', 'required' error will be emited throughout onError handler if useStrategy is not 'ignore'. |
| value                   | *string*   |           | ✓        | The value of the input field. |
| onChange                | *function* |           | ✓        | Callback function that is fired when input filed must change it's value. **Signature:** `function(event: React.FormEvent<HTMLInputElement>, value: string) => void`. |
| onError                 | *function* |         | ❌         | Callback function that is fired when input error status changes and strategy is not 'ignore'.  **Signature:** `function(error: 'none' | 'invalidSymbol' | 'incompleteNumber' | 'singleMinus' | 'singleFloatingPoint' | 'singleZero' | 'min' | 'max' | 'required' | 'clean') => void`. |
| onValid                 | *function*|            | ❌         | Callback function that is fired when input's value is a valid number.  **Signature:** `function(value: number) => void` |
| onRequestValue\*        | *function* |           | ❌         | Callback function that is fired when strategy is 'warn' or 'ignore', input is controlled and an invalid number value is passed. It provides valid number value which needs to be setted. **Signature:** `function(value: string) => void` |
| errorText               | *node*     |           | ✓         | The error content to display. |
| errorStyle              | *object*   |           | ✓         | The style object to use to override error styles. |
| floatingLabelFocusStyle | *object*   |           | ✓         | The style object to use to override floating label styles when focused. |
| floatingLabelStyle      | *object*   |           | ✓         | The style object to use to override floating label styles. |
| floatingLabelText       | *node*     |           | ✓         | The content to use for the floating label element. |
| hintStyle               | *object*   |           | ✓         | Override the inline-styles of the TextField's hint text element. |
| hintText                | *node*     |           | ✓         | The hint content to display. |
| inputStyle              | *object*   |           | ✓         | Override the inline-styles of the TextField's input element. When multiLine is false: define the style of the input element. When multiLine is true: define the style of the container of the textarea. |
| style                   | *object*   |           | ✓         | Override the inline-styles of the root element. |
| underlineDisabledStyle  | *object*   |           | ✓         | Override the inline-styles of the TextField's underline element when disabled. |
| underlineFocusStyle     | *object*   |           | ✓         | Override the inline-styles of the TextField's underline element when focussed. |
| underlineStyle          | *object*   |           | ✓         | Override the inline-styles of the TextField's underline element. |

\* onRequestValue is required when strategy is 'warn' or 'ignore' and input is controlled in order to ensure correct strategy behaviour.

# Strategies

| strategy | onError fired | onRequestValue fired |
| -------- | ------------- | ------------------- |
| 'allow'  |       ✓       |                     |
| 'warn'   |       ✓       |          ✓\*        |
| 'ignore' |               |          ✓\*        |

\* Fired when input is controlled (`value` is provided). If input is not controlled it's value will be automaticlly corrected when it get's invalid number value.

# Errors

## 'none'

Fired when input's value is valid (there is no error).

## 'required'

Fired when `required` prop is `true` and user leaves empty the input or it gets cleared.

## 'clean'

Fired when `required` prop is `false` and user leaves empty the input or it gets cleared.

## 'invalidSymbol'

Fired when user enters none special key which is different than `-`,`.`,`[0-9]`.

## 'incompleteNumber'

Fired wehn user enters `-` as first char in the input or when user enters the first `.`.

## 'singleMinus'

Fired when user enters `-` not as a first char.

## 'singleFloatingPoint'

Fired when user enters `.` and there is already one entered.

## 'singleZero'

Fired when user has entered `0` as first char and enters a digit key.

## 'min'

Fired when user enters number less than `min` prop value.

## 'max'

Fired when user enters number greater than `max` prop value.

# public methods

`NumberInput` re-exposes public method `getInputNode(): HTMLInputElement` from `TextField`.

`TextField` methods: `blur`, `focus`, `select` and `getValue` are not exposed as they and `getInputNode` will be removed in material-ui 0.16 and replaced with public member `input` which is public and now but `getInputNode` is prefered until 0.16 is released. If you want to use any of those methods call them on input retunrned from `getInputNode` with the excpetion of `getValue` instead use `value` property.

# Example

```js
import * as React from 'react';
import NumberInput from 'material-ui-number-input';

class Demo extends React.Component {
  constructor(props) {
  super(props);
  
  this.onKeyDown = (event) => {
    console.log(`onKeyDown ${event.key}`);
  };
  
  this.onChange = (event, value) => {
    const e = event;
    console.log(`onChange ${e.target.value}, ${value}`);
  };
  
  this.onError = (error) => {
    let errorText;
    console.log(error);
    switch (error) {
      case 'required':
        errorText = 'This field is required';
        break;
      case 'invalidSymbol':
        errorText = 'You are tring to enter none number symbol';
        break;
      case 'incompleteNumber':
        errorText = 'Number is incomplete';
        break;
      case 'singleMinus':
        errorText = 'Minus can be use only for negativity';
        break;
      case 'singleFloatingPoint':
        errorText = 'There is already a floating point';
        break;
      case 'singleZero':
        errorText = 'Floating point is expected';
        break;
      case 'min':
        errorText = 'You are tring to enter number less than -10';
        break;
      case 'max':
          errorText = 'You are tring to enter number greater than 12';
          break;
      }
      this.setState({ errorText: errorText });
    };

    this.onValid = (value) => {
      console.debug(`${value} is a valid number`);
    };

    this.onRequestValue = (value) => {
      console.log(`request ${JSON.stringify(value)}`);
      this.setState({ value: value })
    }
  }
    
  render() {
    const { state, onChange, onError, onKeyDown, onValid, onRequestValue } = this; 
    return (
      <NumberInput
        id="num"
        value={state.value}
        required
        defaultValue={9}
        min={-10}
        max={12}
        strategy="warn"
        errorText={state.errorText}
        onValid={onValid}
        onChange={onChange}
        onError={onError}
        onRequestValue={onRequestValue}
        onKeyDown={onKeyDown} />
    );
  }
}
```

# Written in Typescript and Typescript Ready! ([check example](example/index.tsx))

# Supports propTypes for regular JavaScript users

# Testing

## Tests will be added soon

# Contributing

1. Fork the repository
2. `npm install`
3. `npm run typings`
4. Make changes
5. `npm start`
6. open `http://localhost:3000`
7. Make a Pull Request

