![license](https://img.shields.io/npm/l/ngclipboard.svg)
![travis](https://travis-ci.org/sachinchoolur/ngclipboard.svg?branch=master)
![bower](https://img.shields.io/bower/v/ngclipboard.svg)
![npm](https://img.shields.io/npm/v/ngclipboard.svg)

# ngclipboard
##### An angularjs directive to copy text to clipboard without using flash

> Angularjs directive for [clipboard.js](http://zenorocha.github.io/clipboard.js/) by [@zenorocha](https://twitter.com/zenorocha)

## Install

You can get it on npm.

```
npm install ngclipboard --save
```

Or bower, too.

```
bower install ngclipboard --save
```

If you're not into package management, just [download a ZIP](https://github.com/sachinchoolur/ngclipboard/archive/master.zip) file.

## Setup

First, include angularjs and clipboard.js into your document. 

```html
<script src="//ajax.googleapis.com/ajax/libs/angularjs/1.3.14/angular.min.js"></script>
<script src="https://cdn.rawgit.com/zenorocha/clipboard.js/master/dist/clipboard.min.js"></script>
```

Then Include ngclipboard.js. 

```html
<script src="dist/ngclipboard.min.js"></script>
```
Add `ngclipboard` dependency to your module
```javascript
var myApp = angular.module('app', ['ngclipboard']);
```

Finally, add `ngclipboard` directive to the wanted html element.

```javascript
<button class="btn" ngclipboard data-clipboard-text="Just because you can doesn't mean you should — clipboard.js">
    Copy to clipboard
</button>
```

# Usage

We're living a _declarative renaissance_, that's why we decided to take advantage of [HTML5 data attributes](https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Using_data_attributes) for better usability.

### Copy text from another element

A pretty common use case is to copy content from another element. You can do that by adding a `data-clipboard-target` attribute in your trigger element.

The value you include on this attribute needs to match another's element selector.

<a href="https://zenorocha.github.io/clipboard.js/#example-target"><img width="473" alt="example-2" src="https://cloud.githubusercontent.com/assets/398893/9983467/a4946aaa-5fb1-11e5-9780-f09fcd7ca6c8.png"></a>

```html
<!-- Target -->
<input id="foo" value="https://github.com/sachinchoolur/ngclipboard.git">

<!-- Trigger -->
<button class="btn" ngclipboard data-clipboard-target="#foo">
    <img src="assets/clippy.svg" alt="Copy to clipboard">
</button>
```

### Cut text from another element

Additionally, you can define a `data-clipboard-action` attribute to specify if you want to either `copy` or `cut` content.

If you omit this attribute, `copy` will be used by default.

<a href="https://zenorocha.github.io/clipboard.js/#example-action"><img width="473" alt="example-3" src="https://cloud.githubusercontent.com/assets/398893/10000358/7df57b9c-6050-11e5-9cd1-fbc51d2fd0a7.png"></a>

```html
<!-- Target -->
<textarea id="bar">Mussum ipsum cacilds...</textarea>

<!-- Trigger -->
<button class="btn" ngclipboard data-clipboard-action="cut" data-clipboard-target="#bar">
    Cut to clipboard
</button>
```

As you may expect, the `cut` action only works on `<input>` or `<textarea>` elements.

### Copy text from attribute

Truth is, you don't even need another element to copy its content from. You can just include a `data-clipboard-text` attribute in your trigger element.

<a href="https://zenorocha.github.io/clipboard.js/#example-text"><img width="147" alt="example-1" src="https://cloud.githubusercontent.com/assets/398893/10000347/6e16cf8c-6050-11e5-9883-1c5681f9ec45.png"></a>

```html
<!-- Trigger -->
<button class="btn" ngclipboard data-clipboard-text="Just because you can doesn't mean you should — clipboard.js">
    Copy to clipboard
</button>
```

## Events

There are cases where you'd like to show some user feedback or capture what has been selected after a copy/cut operation.

That's why we fire custom events such as `success` and `error` for you to listen and implement your custom logic.

ngclipboard provides you two attributes called `ngclipboard-success` and `ngclipboard-error` to listen the clipboard events and implement your custom logic.

```html
<button class="btn" ngclipboard ngclipboard-success="onSuccess(e);" ngclipboard-error="onError(e);" data-clipboard-text="Just because you can doesn't mean you should — clipboard.js">
    Copy to clipboard
</button>
```

```js
// You can still access the clipboard.js event
$scope.onSuccess = function(e) {
    console.info('Action:', e.action);
    console.info('Text:', e.text);
    console.info('Trigger:', e.trigger);

    e.clearSelection();
};

$scope.onError = function(e) {
    console.error('Action:', e.action);
    console.error('Trigger:', e.trigger);
}

```

For a live demonstration, open this [site](https://sachinchoolur.github.io/ngclipboard/) and just your console :)


## Browser Support

This library relies on both [Selection](https://developer.mozilla.org/en-US/docs/Web/API/Selection) and [execCommand](https://developer.mozilla.org/en-US/docs/Web/API/Document/execCommand) APIs. The second one is supported in the following browsers.

| <img src="https://zenorocha.github.io/clipboard.js/assets/images/chrome.png" width="48px" height="48px" alt="Chrome logo"> | <img src="https://zenorocha.github.io/clipboard.js/assets/images/firefox.png" width="48px" height="48px" alt="Firefox logo"> | <img src="https://zenorocha.github.io/clipboard.js/assets/images/ie.png" width="48px" height="48px" alt="Internet Explorer logo"> | <img src="https://zenorocha.github.io/clipboard.js/assets/images/opera.png" width="48px" height="48px" alt="Opera logo"> | <img src="https://zenorocha.github.io/clipboard.js/assets/images/safari.png" width="48px" height="48px" alt="Safari logo"> |
|:---:|:---:|:---:|:---:|:---:|
| 42+ ✔ | 41+ ✔ | 9+ ✔ | 29+ ✔ | 10+ ✔ |

When an occurence occurs where the [execCommand](https://developer.mozilla.org/en-US/docs/Web/API/Document/execCommand) copy/cut operations are not supported, it gracefully degrades because [Selection](https://developer.mozilla.org/en-US/docs/Web/API/Selection) is widely supported.

That means you can show a tooltip saying `Copied!` when `success` event is called and `Press Ctrl+C to copy` when `error` event is called because the text is already selected.

For a live demonstration, open this [site](https://sachinchoolur.github.io/ngclipboard/) with a non-supporting browser.

## License

MIT License
