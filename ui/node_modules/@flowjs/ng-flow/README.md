# ng-flow [![Build Status](https://travis-ci.org/flowjs/ng-flow.svg)](https://travis-ci.org/flowjs/ng-flow) [![Test Coverage](https://codeclimate.com/github/flowjs/ng-flow/badges/coverage.svg)](https://codeclimate.com/github/flowjs/ng-flow/coverage)

[![Saucelabs Test Status](https://saucelabs.com/browser-matrix/ng-flow.svg)](https://saucelabs.com/u/ng-flow)

ng-flow is a [Flow.js](https://github.com/flowjs/flow.js) extensions for angular.js framework, no 3rd party JS dependencies required!

Demo: http://flowjs.github.io/ng-flow/

How can I install it?
============
1) Get the library:

**Direct Download**
Download a latest build from https://github.com/flowjs/ng-flow/releases
it contains development and minified production files in `dist/` directory,
they are also concatenated with core flow.js library.

**Using Bower**
        
        bower install ng-flow#~2
                
**Git Clone**
        
        git clone https://github.com/flowjs/ng-flow
        
**Using Yeoman**

        bower install "ng-flow#~2" --save
        grunt bower-install
                
2) Add the module to your app as a dependency:

        angular.module('app', ['flow'])
        
3) Include the files in your project
```html
<!-- concatenated flow.js + ng-flow libraries -->
<script src="ng-flow/dist/ng-flow-standalone.min.js"></script>
<!-- or include the files separately -->
<script src="flow.js/dist/flow.min.js"></script>
<script src="ng-flow/dist/ng-flow.min.js"></script>
```

How can I use it?
============

First of all wrap places there you are going to use Flow.js
````html
<div flow-init>
    ... other flow directives goes here ...
</div>
````

This directive is going to add $flow variable to current scope.
Also directive can be nested, because `$flow` variable is going to be overridden.
`$flow` is instance of [Flow](https://github.com/flowjs/flow.js#flow).


Secondly you need to assign some upload buttons:
````html
<input type="file" flow-btn />
<input type="file" flow-btn flow-directory />
  Input OR Other element as upload button
<span flow-btn>Upload File</span>
````

First button is for normal uploads and second is for directory uploads.
Note: avoid using `<a>` and `<button>` tags as file upload buttons, use `<span>` instead.


Now you need to display uploaded files, all you need to do is to loop files array.
Files array is attached to flow object named `$flow`.
````html
<tr ng-repeat="file in $flow.files">
    <td>{{$index+1}}</td>
    <td>{{file.name}}</td>
</tr>
````

file is instance of [FlowFile](https://github.com/flowjs/flow.js#flowfile).


### Quick setup
````html
<div flow-init="{target: '/upload'}"
     flow-files-submitted="$flow.upload()"
     flow-file-success="$file.msg = $message">

  <input type="file" flow-btn/>
  Input OR Other element as upload button
  <span class="btn" flow-btn>Upload File</span>

  <table>
    <tr ng-repeat="file in $flow.files">
        <td>{{$index+1}}</td>
        <td>{{file.name}}</td>
        <td>{{file.msg}}</td>
    </tr>
  </table>
</div>
```

Need more examples?
============
Clone this repository and go to "ng-flow/samples/basic/index.html".
Single image upload "ng-flow/samples/image/index.html".


How can I drop files?
============

Use `flow-drop` directive:
````html
<div class="alert" flow-drop>
    Drag And Drop your file here
</div>
````
Note: in most cases `flow-drop` must be used together with `flow-prevent-drop` directive on `body`
element, because it prevents file from being loaded in the browser.

### Prevent dropping files on a document
Use `flow-prevent-drop` directive on `body` element:
````html
<body flow-prevent-drop>

</body>
````

### How to add some styles while dropping a file?
Use `flow-drag-enter` directive:
````html
<div flow-drag-enter="style={border:'4px solid green'}" flow-drag-leave="style={}"
     ng-style="style">
</div>
````
Note: `flow-drag-leave` attribute can't be used alone, it is a part of `flow-drag-enter` directive.

### How to dynamically disable drop area?
````html
<div class="alert" flow-drop flow-drop-enabled="config.enabled">
    Drag And Drop your file here
</div>
````
See example at `samples/dataurl/`.


How can I preview uploaded image?
============

Use flow-img directive:
````html
<img flow-img="$flow.files[0]" />
````

Image will be automatically updated once file is added. No need to start upload.


How can I set options for flow.js?
============

Use config:
````javascript
var app = angular.module('app', ['flow'])
.config(['flowFactoryProvider', function (flowFactoryProvider) {
    flowFactoryProvider.defaults = {
        target: '/upload',
        permanentErrors:[404, 500, 501]
    };
    // You can also set default events:
    flowFactoryProvider.on('catchAll', function (event) {
      ...
    });
    // Can be used with different implementations of Flow.js
    // flowFactoryProvider.factory = fustyFlowFactory;
}]);
````

also can be configured on "flow-init" directive:
````html
<div flow-init="{target:'/uploader'}">

</div>
````


How can I catch events?
============

Events are listed inside `flow-init` directive:
````html
<div flow-init
        flow-file-success="someHandlerMethod( $file, $message, $flow )"
        flow-file-progress="someHandlerMethod( $file, $flow )"
        flow-file-added="someHandlerMethod( $file, $event, $flow )"
        flow-files-added="someHandlerMethod( $files, $event, $flow )"
        flow-files-submitted="someHandlerMethod( $files, $event, $flow )"
        flow-file-retry="someHandlerMethod( $file, $flow )"
        flow-file-error="someHandlerMethod( $file, $message, $flow )"
        flow-error="someHandlerMethod( $file, $message, $flow )"
        flow-complete=" ... "
        flow-upload-started=" ... "
        flow-progress=" ... "
      >
      <div flow-file-progress=" ... events can be also assigned inside flow-init ... "></div>

</div>
````

### How can I catch an event in a controller?
If controller is on the same scope as `flow-init` directive or in a child scope,
then we can catch events with `$on`. Events are prefixed with `flow::`.
````javascript
$scope.$on('flow::fileAdded', function (event, $flow, flowFile) {
  event.preventDefault();//prevent file from uploading
});
````
second argument is always a `flow` instance and then follows event specific arguments.

How can I assign flow to a parent scope?
============

Use `flow-name` attribute and set it to any variable in the scope.
````html
<div flow-init flow-name="obj.flow">
    ... Flow is set to obj.flow  ...
    I have uploaded files: #{{obj.flow.files.length}}
</div>
````
````javascript
$scope.obj = {}; // variable "obj" must be initialized on the scope
````

How can I initialize flow with an existing flow object ?
============

Use `flow-object` attribute and set it with the existing flow object on scope.
````html
<div flow-init flow-object="existingFlowObject">
    ... Flow is initialized with existingFlowObject, no new Flow object  is created ...
    There are already {{ existingFLowObject.files.length }} files uploaded,
    which is equal to {{ $flow.files.length }}.
</div>
````

How can I support older browsers?
============
Go to https://github.com/flowjs/fusty-flow.js
and add to your config:
````javascript
var app = angular.module('app', ['flow'])
.config(['flowFactoryProvider', function (flowFactoryProvider) {
    flowFactoryProvider.factory = fustyFlowFactory;
}]);
````

Contribution
============
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes must be tested by one or more specs.

* With the exceptions listed below, we follow the rules contained in [Google's JavaScript Style Guide](http://google-styleguide.googlecode.com/svn/trunk/javascriptguide.xml):

  * Wrap all code at 100 characters.

  * Instead of complex inheritance hierarchies, we prefer simple objects. We use prototypical
inheritance only when absolutely necessary.

