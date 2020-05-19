# material-steppers

Angular Steppers directive for Angular Material

Based on Material Steppers: https://www.google.com/design/spec/components/steppers.html#steppers-types-of-steppers

## Demo

https://eberlitz.github.io/material-steppers/demo

## Usage

###  using bower

```shell
bower install material-steppers --save
```

### or using npm

```shell
npm install material-steppers --save
```

**note**: works with angular 1.4.9

### Add to your module

```javascript
angular.module('app', ['ngMaterial', 'mdSteppers']);
```

### Write your html

```html      
<md-stepper id="stepper-demo" 
        md-mobile-step-text="$ctrl.isMobileStepText" 
        md-vertical="$ctrl.isVertical" 
        md-linear="$ctrl.isLinear"
        md-alternative="$ctrl.isAlternative">
        <md-step md-label="Select a campaign">
            <md-step-body>
                <p>Step content</p>
            </md-step-body>
            <md-step-actions>
                <md-button class="md-primary md-raised" ng-click="$ctrl.selectCampaign();">Continue</md-button>
                <md-button class="md-primary" ng-click="$ctrl.cancel();">Cancel</md-button>
            </md-step-actions>
        </md-step>
        <md-step md-label="Create an group">
            <md-step-body>
                <p>Step content</p>
            </md-step-body>
            <md-step-actions>
                <md-button class="md-primary md-raised" ng-click="$ctrl.nextStep();">Continue</md-button>
                <md-button class="md-primary" ng-click="$ctrl.previousStep();">Back</md-button>
            </md-step-actions>
        </md-step>
        <!-- Other steps if needed ... -->
    </md-stepper>
```

# $mdStepper Service

Used to control a stepper by it's id. Example:

```js
ver steppers = $mdStepper('stepper-demo');
steppers.next();
```

Detailed service operations bellow:

| Method | Description | Returns |
| --- | --- | --- |
| `next()` | Complete the current step and move one to the next. Using this method on editable steps (in linear stepper) it will search by the next step without "completed" state to move. When invoked it dispatch the event onstepcomplete to the step element. | boolean - True if move and false if not move (e.g. On the last step) | 
| `back()` | Move to the previous step without change the state of current step. Using this method in linear stepper it will check if previous step is editable to move. | boolean - True if move and false if not move (e.g. On the first step) |
| `skip()` | Move to the next step without change the state of current step. This method works only in optional steps. | boolean - True if move and false if not move (e.g. On non-optional step) |
| `goto(stepNumber: number)` | Move "active" to specified step id parameter. The id used as reference is the integer number shown on the label of each step (e.g. 2). | boolean - True if move and false if not move (e.g. On id not found) |
| `error(message: string)` | Defines the current step state to "error" and shows the message parameter on title message element.When invoked it dispatch the event onsteperror to the step element. | {string} message The error message |
| `clearError()` | Defines the current step state to "normal" and removes the message parameter on title message element. | void |
| `showFeedback(message?: string)` | Shows a feedback message and a loading indicador. | void |
| `clearFeedback()` | Removes the feedback. |  void |

# TODO

- [x] Horizontal steppers
- [x] Vertical steppers
- [x] Linear steppers
- [x] Non-linear steppers
- [x] Alternative labels
- [x] Optional steps
- [ ] Editable steps
- [x] Stepper feedback
- Mobile steppers
    - [x] Mobile step text
    - [ ] Mobile step dots
    - [ ] Mobile step progress bar
- [x] Correct apply styles (css) of the material design
- [x] Embed SVG Icon assets
- [ ] Create a better demo page with all options.

## Remarks

- Based on:
 - [MDL Stepper](https://github.com/ahlechandre/mdl-stepper)
 - [MD Steppers](https://github.com/ipiz/md-steppers)
 - [Angular Material Steppers](https://github.com/marcosmoura/angular-material-steppers)

- Thanks to all ;)


## License

The MIT License (MIT)

Copyright (c) 2016 Eduardo Eidelwein Berlitz

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
