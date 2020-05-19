
# AngularJS multi-level accordion

  - Allows for a nested structure
  - Works with (or without) `ng-repeat`
  - Allows multiple sections to be open at once


## Examples

  - [GitHub](http://lukaszwatroba.github.io/v-accordion)
  - [CodePen](http://codepen.io/LukaszWatroba/pen/MwdaLo)
  - [Linksfridge](https://linksfridge.com/help)


## Usage

  - If you use [bower](http://bower.io/) or [npm](https://www.npmjs.com/), just `bower/npm install v-accordion`. If not, download files [from the github repo](./dist).

  - Include `angular.js`, `angular-animate.js`, `v-accordion.js`, and `v-accordion.css`:
  ```html
  <link href="v-accordion.css" rel="stylesheet" />

  <script src="angular.js"></script>
  <script src="angular-animate.js"></script>

  <script src="v-accordion.js"></script>
  ```

  - Add `vAccordion` and `ngAnimate` as dependencies to your application module:
  ```js
  angular.module('myApp', ['vAccordion', 'ngAnimate']);
  ```

  - Put the following markup in your template:
  ```html
  <!-- add `multiple` attribute to allow multiple sections to open at once -->
  <v-accordion class="vAccordion--default" multiple>

    <!-- add expanded attribute to open the section -->
    <v-pane expanded>
      <v-pane-header>
        Pane header #1
      </v-pane-header>

      <v-pane-content>
        Pane content #1
      </v-pane-content>
    </v-pane>

    <v-pane disabled>
      <v-pane-header>
        Pane header #2
      </v-pane-header>

      <v-pane-content>
        Pane content #2
      </v-pane-content>
    </v-pane>

  </v-accordion>
  ```

  - You can also use `v-accordion` with `ng-repeat`:
  ```html
  <v-accordion class="vAccordion--default">

    <v-pane ng-repeat="pane in panes" expanded="pane.isExpanded">
      <v-pane-header>
        {{ ::pane.header }}
      </v-pane-header>

      <v-pane-content>
        {{ ::pane.content }}

        <!-- accordions can be nested :) -->
        <v-accordion ng-if="pane.subpanes">
          <v-pane ng-repeat="subpane in pane.subpanes" ng-disabled="subpane.isDisabled">
            <v-pane-header>
              {{ ::subpane.header }}
            </v-pane-header>
            <v-pane-content>
              {{ ::subpane.content }}
            </v-pane-content>
          </v-pane>
        </v-accordion>
      </v-pane-content>
    </v-pane>

  </v-accordion>
  ```


## API

#### Control

Add `control` attribute and use the following methods to control vAccordion from it's parent scope:

  - `toggle(indexOrId)`
  - `expand(indexOrId)`
  - `collapse(indexOrId)`
  - `expandAll()`
  - `collapseAll()`
  - `hasExpandedPane()`

```html
<v-accordion id="my-accordion" multiple control="accordion">

  <v-pane id="{{ pane.id }}" ng-repeat="pane in panes">
    <v-pane-header>
      {{ ::pane.header }}
    </v-pane-header>

    <v-pane-content>
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

</v-accordion>

<button ng-click="accordion.toggle(0)">Toggle first pane</button>
<button ng-click="accordion.expandAll()">Expand all</button>
<button ng-click="accordion.collapseAll()">Collapse all</button>
```

```js
$scope.$on('my-accordion:onReady', function () {
  var firstPane = $scope.panes[0];
  $scope.accordion.toggle(firstPane.id);
});
```

#### Scope

`$accordion` and `$pane` properties allows you to control the directive from it's transcluded scope.

##### $accordion

  - `toggle(indexOrId)`
  - `expand(indexOrId)`
  - `collapse(indexOrId)`
  - `expandAll()`
  - `collapseAll()`
  - `hasExpandedPane()`
  - `id`

##### $pane

  - `toggle()`
  - `expand()`
  - `collapse()`
  - `isExpanded()`
  - `id`

```html
<v-accordion multiple>

  <v-pane ng-repeat="pane in panes">
    <!-- here's how you can create a custom toggle button -->
    <v-pane-header inactive>
      {{ ::pane.header }}
      <button ng-click="$pane.toggle()">Toggle me</button>
    </v-pane-header>

    <v-pane-content>
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

  <button ng-click="$accordion.expandAll()">Expand all</button>

</v-accordion>
```


#### Events

The directive emits the following events:

  - `vAccordion:onReady` or `yourAccordionId:onReady`
  - `vAccordion:onExpand` or `yourAccordionId:onExpand`
  - `vAccordion:onExpandAnimationEnd` or `yourAccordionId:onExpandAnimationEnd`
  - `vAccordion:onCollapse` or `yourAccordionId:onCollapse`
  - `vAccordion:onCollapseAnimationEnd` or `yourAccordionId:onCollapseAnimationEnd`


## Callbacks

Use these callbacks to get the expanded/collapsed pane index and id:

```html
<v-accordion onexpand="expandCallback(index, id)" oncollapse="collapseCallback(index, id)">

  <v-pane id="{{ ::pane.id }}" ng-repeat="pane in panes">
    <v-pane-header>
      {{ ::pane.header }}
    </v-pane-header>

    <v-pane-content>
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

</v-accordion>
```

```js
$scope.expandCallback = function (index, id) {
  console.log('expanded pane:', index, id);
};

$scope.collapseCallback = function (index, id) {
  console.log('collapsed pane:', index, id));
};
```


## Configuration

#### Module
To change the default animation duration, inject `accordionConfig` provider in your app config:

```javascript
angular
  .module('myApp', ['vAccordion'])
  .config(function (accordionConfig) {
    accordionConfig.expandAnimationDuration = 0.5;
  });
```

#### SCSS
If you are using SASS, you can import vAccordion.scss file and override the following variables:

```scss
$v-accordion-default-theme:         true !default;

$v-accordion-spacing:               20px !default;

$v-pane-border-color:               #D8D8D8 !default;
$v-pane-expanded-border-color:      #2196F3 !default;
$v-pane-icon-color:                 #2196F3 !default;
$v-pane-hover-color:                #2196F3 !default;

$v-pane-disabled-opacity:           0.6   !default;

$v-pane-expand-animation-duration:  0.5s  !default;
$v-pane-hover-animation-duration:   0.25s !default;
```


## Accessibility
vAccordion manages keyboard focus and adds some common aria-* attributes. BUT you should additionally place the `aria-controls` and `aria-labelledby` as follows:

```html
<v-accordion>

  <v-pane ng-repeat="pane in panes">
    <v-pane-header id="{{ ::pane.id }}-header" aria-controls="{{ ::pane.id }}-content">
      {{ ::pane.header }}
    </v-pane-header>

    <v-pane-content id="{{ ::pane.id }}-content" aria-labelledby="{{ ::pane.id }}-header">
      {{ ::pane.content }}
    </v-pane-content>
  </v-pane>

</v-accordion>
```
