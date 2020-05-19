### 0.7.5

Minor bugfixes

### 0.7.4

Minor bugfixes

### 0.7.3

Several bugfixes

#### Features

* Added `mdp-disabled` 

#### Breaking changes

`min` and `max` attributes are refactored to `mdp-min-date` and `mdp-max-date` respectively and are setup as two-way binding

#### Features

* Floating labels and `mdp-no-float` attribute
* Directives are restricted to (A)ttribute and (E)lement
* `mdp-open-on-click` attribute for open date picker by clicking on the input

### 0.7.1

Bugfixes and improvements

### 0.7.0

Bugfixes and improvements (obviously.. :))

#### Breaking Changes

* `mdpTimePicker` and `mdpDatePicker` are now restricted to and Element (see demo for details)
* Pickers will not open clicking the input, but using the button

#### Features

* Button for open dialog
* Set first day of the week by changing Moment.js localeData
* Customize date format on date picker
* Dialog will not close others (needs angular material >= 1.1.0rc-1)

### 0.6.1

Minor bugfixes and improvements

### 0.6.0

#### Breaking Changes

* Removed Material Design Icons dependency
* `$mdpDatePicker` and `$mdpTimePicker` now accept the date/time and an object containing the options as arguments. _(see demo for more details)_
 
#### Features

* `mdpDatePicker` directive now accept min/max date and a filter function

```html
<input type="date" min="2000-01-01" max="2050-01-01" mdp-date-filter="myFilterFunction" />
```
```javascript
function myFilterFunction(date) {
    ...
}
```

* Output format in `mdpTimePicker` and `mdpDatePicker` directives thorugh `mdp-format` attribute _(see moment.js documentation for details)_
```html
<input type="text" min="2000-01-01" max="2050-01-01" mdp-format="DD/MM/YYYY" />
```
* Swipe capabilities on date picker
* Automatic switch between hours and minutes view in the time picker

### 0.5.2

#### Bugfixes

* Corrected packaje.json file

#### Features

* Added mdpTimePicker directive for input[type=time]

### 0.5.1

Minor bugfixes and improvements

### 0.5.0

#### Breaking Changes

* Service `$mdDatePicker` is changed to `$mdpDatePicker` 
* Directive `mdDatePicker` is changed to `mdpDatePicker` 

#### Features

* Time picker
* Dynamic year selector on date picker
* Animations
* Minor improvements 

### 0.3.2

#### Bugfixes

* Renamed classes names to avoid collisions with official date picker
* Changed angular repository in bower config

#### Features

* Added config provider for $mdDatePicker. Now is possible to set the labels of the dialog buttons with $mdDatePickerProvider.setOKButtonLabel(...) and $mdDatePickerProvider.setCancelButtonLabel(...)
* Minor improvements

### 0.3.0

#### Features

* Bower support

### 0.2.0

#### Breaking Changes

Repository name is changed to `mdPickers`. The goal is to have a both date and time pickers in the same module.

### 0.1.0

#### Features

* Date picker dialog
