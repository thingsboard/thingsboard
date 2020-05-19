# React-Ace

[![Backers on Open Collective](https://opencollective.com/react-ace/backers/badge.svg)](#backers) [![Sponsors on Open Collective](https://opencollective.com/react-ace/sponsors/badge.svg)](#sponsors) [![Greenkeeper badge](https://badges.greenkeeper.io/securingsincity/react-ace.svg)](https://greenkeeper.io/)

[![npm version](https://badge.fury.io/js/react-ace.svg)](http://badge.fury.io/js/react-ace)
[![Build Status](https://travis-ci.org/securingsincity/react-ace.svg)](https://travis-ci.org/securingsincity/react-ace)
[![CDNJS](https://img.shields.io/cdnjs/v/react-ace.svg)](https://cdnjs.com/libraries/react-ace)
[![Coverage Status](https://coveralls.io/repos/github/securingsincity/react-ace/badge.svg?branch=master)](https://coveralls.io/github/securingsincity/react-ace?branch=master)

A react component for Ace / Brace

[DEMO](http://securingsincity.github.io/react-ace/)

## Install

`npm install react-ace`

## Usage

```javascript
import React from 'react';
import { render } from 'react-dom';
import brace from 'brace';
import AceEditor from 'react-ace';

import 'brace/mode/java';
import 'brace/theme/github';

function onChange(newValue) {
  console.log('change',newValue);
}

// Render editor
render(
  <AceEditor
    mode="java"
    theme="github"
    onChange={onChange}
    name="UNIQUE_ID_OF_DIV"
    editorProps={{$blockScrolling: true}}
  />,
  document.getElementById('example')
);
```

## Examples

* Checkout `example` directory for a working example using webpack.
* [create-react-app](https://github.com/securingsincity/react-ace-create-react-app-example)
* [preact](https://github.com/securingsincity/react-ace-preact-example)
* [webpack](https://github.com/securingsincity/react-ace-webpack-example)


## Available Props

|Prop|Default|Description|
|-----|------|----------|
|name| 'brace-editor'| Unique Id to be used for the editor|
|mode| ''| Language for parsing and code highlighting|
|theme| ''| theme to use|
|height| '500px'| CSS value for height|
|width| '500px'| CSS value for width|
|className| | custom className|
|fontSize| 12| pixel value for font-size|
|showGutter| true| boolean|
|showPrintMargin| true| boolean|
|highlightActiveLine| true| boolean|
|focus| false| boolean|
|cursorStart| 1| number|
|wrapEnabled| false| Wrapping lines|
|readOnly| false| boolean|
|minLines| | Minimum number of lines to be displayed|
|maxLines| | Maximum number of lines to be displayed|
|enableBasicAutocompletion| false| Enable basic autocompletion|
|enableLiveAutocompletion| false| Enable live autocompletion|
|tabSize| 4| tabSize number|
|value | ''| String value you want to populate in the code highlighter|
|defaultValue | ''| Default value of the editor|
|onLoad| | Function onLoad|
|onBeforeLoad| | function that trigger before editor setup|
|onChange| | function that occurs on document change it has 2 arguments the value and the event. see the example above|
|onCopy| | function that trigger by editor `copy` event, and pass text as argument|
|onPaste| | function that trigger by editor `paste` event, and pass text as argument|
|onSelectionChange| | function that trigger by editor `selectionChange` event, and passes a [Selection](https://ace.c9.io/#nav=api&api=selection) as it's first argument and the event as the second|
|onFocus| | function that trigger by editor `focus` event|
|onBlur| | function that trigger by editor `blur` event|
|onScroll| | function that trigger by editor `scroll` event|
|editorProps| | Object of properties to apply directly to the Ace editor instance|
|setOptions| | Object of [options](https://github.com/ajaxorg/ace/wiki/Configuring-Ace) to apply directly to the Ace editor instance|
|keyboardHandler| | String corresponding to the keybinding mode to set (such as vim)|
|commands| | Array of new commands to add to the editor
|annotations| | Array of annotations to show in the editor i.e. `[{ row: 0, column: 2, type: 'error', text: 'Some error.'}]`, displayed in the gutter|
|markers| | Array of [markers](https://ace.c9.io/api/edit_session.html#EditSession.addMarker) to show in the editor, i.e. `[{ startRow: 0, startCol: 2, endRow: 1, endCol: 20, className: 'error-marker', type: 'background' }]`|
|style| | Object with camelCased properties |

## Modes, Themes, and Keyboard Handlers

All modes, themes, and keyboard handlers should be required through ```brace``` directly.  Browserify will grab these modes / themes / keyboard handlers through ```brace``` and will be available at run time.  See the example above.  This prevents bloating the compiled javascript with extra modes and themes for your application.

### Example Modes

* javascript
* java
* python
* xml
* ruby
* sass
* markdown
* mysql
* json
* html
* handlebars
* golang
* csharp
* coffee
* css

### Example Themes

* monokai
* github
* tomorrow
* kuroir
* twilight
* xcode
* textmate
* solarized dark
* solarized light
* terminal

### Example Keyboard Handlers

* vim
* emacs


## Backers

Support us with a monthly donation and help us continue our activities. [[Become a backer](https://opencollective.com/react-ace#backer)]

<a href="https://opencollective.com/react-ace/backer/0/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/0/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/1/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/1/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/2/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/2/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/3/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/3/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/4/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/4/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/5/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/5/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/6/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/6/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/7/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/7/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/8/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/8/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/9/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/9/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/10/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/10/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/11/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/11/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/12/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/12/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/13/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/13/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/14/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/14/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/15/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/15/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/16/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/16/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/17/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/17/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/18/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/18/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/19/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/19/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/20/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/20/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/21/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/21/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/22/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/22/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/23/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/23/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/24/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/24/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/25/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/25/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/26/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/26/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/27/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/27/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/28/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/28/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/backer/29/website" target="_blank"><img src="https://opencollective.com/react-ace/backer/29/avatar.svg"></a>


## Sponsors

Become a sponsor and get your logo on our README on Github with a link to your site. [[Become a sponsor](https://opencollective.com/react-ace#sponsor)]

<a href="https://opencollective.com/react-ace/sponsor/0/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/1/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/2/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/3/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/4/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/5/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/6/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/7/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/8/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/react-ace/sponsor/9/website" target="_blank"><img src="https://opencollective.com/react-ace/sponsor/9/avatar.svg"></a>


