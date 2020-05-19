# Angular Fixed Table Header

This module will allow you to scroll a table vertically while the header remains visible.

* [License](#license)
* [Demo](#demo)
* [Installation](#installation)
* [Usage](#usage)
* [Change Log](#change-log)
* [How It Works](#how-it-works)
* [Restrictions](#restrictions)

## License

This software is provided free of charge and without restriction under the [MIT License](LICENSE.md)

## Demo

[Codepen](http://codepen.io/anon/pen/qbLaMb?editors=101)

## Installation

#### Using Bower

This package is installable through the Bower package manager.

```
bower install angular-fixed-table-header --save
```

In your `index.html` file, include the source file.

```html
<script type="text/javascript" src="bower_components/angular-fixed-table-header/src/fixed-table-header.min.js"></script>
```

Include the `fixed.table.header` module as a dependency in your application.

```javascript
angular.module('myApp', ['fixed.table.header']);
```

#### Using npm and Browserify (or JSPM)

In addition, this package may be installed using npm.

```
npm install angular-fixed-table-header --save
```

You may use Browserify to inject this module into your application.

```javascript
angular.module('myApp', [require('angular-fixed-table-header')]);
```

## Usage

Wrap the table in a element that will scroll vertically. Use the `fix-head` attribute on a `<thead>` element to prevent it from scrolling with the rest of the table.

A clone of the original `<thead>` element will be moved outside the scroll container. To ensure valid HTML, the cloned `<thead>` element will be wrapped in a copy of the `<table>` element. The new `<table>` element will be given the `clone` class.

```html
<div style="overflow: auto; max-height: 300px;">
  <table>
    <thead fix-head>
      <tr>
        <th>Name</th>
        <th>City</th>
        <th>State</th>
        <th>Zip</th>
        <th>Email</th>
        <th>Phone</th>
      </tr>
    </thead>
    <tbody>
      <tr ng-repeat="contact in contacts">
        <td>{{contact.name}}</td>
        <td>{{contact.city}}</td>
        <td>{{contact.state}}</td>
        <td>{{contact.zip}}</td>
        <td>{{contact.emial}}</td>
        <td>{{contact.phone}}</td>
      </tr>
    </tbody>
  </table>
</div>
```

## Change Log

#### Version 0.2.1
###### March 15, 2016

* Set the max width of the header cell as well.
* Fix bower.json `main` property.

#### Version 0.2.0
###### March 4, 2016

* You may now use `ng-repeat` within the table header.

## How It Works

1. Clone the original `<table>` element and empty its contents, then move it outside the scroll container and compile it.
2. Clone the original `<thead>` element and append it to the original `<table>` element and compile it.
3. Detach the cloned `<thead>` element and append it to the cloned `<table>` element.
4. For each `<th>` in the cloned `<thead>`, set its width equal to the width of the original `<th>` in the original `<thead>`.
5. Set the top margin of the original `<table>` element equal to negative the height of the original `<thead>` element.
6. When the scroll container is scrolled horizontally, use css transforms to translate the cloned `<thead>` element.

The advantage of this solution is the functionality of HTML tables is preserved.

## Restrictions
 
* Your table must be wrapped in a div that determines the vertical scroll of your table (you may use flex box).
* You may only have one `thead` element; however, your `thead` element may have multiple rows.

#### Using With The Data Table Module

If you are using this directive with my data table module then be aware that the progress indicator will still scroll with the rest of the table.

Use the following CSS to correct the borders.

```css
table.clone thead tr:last-child th {
  border-bottom: 1px rgba(0, 0, 0, 0.12) solid;
}

table.clone + md-table-container table tbody tr:first-child td {
  border-top: none;
}
```

#### Why Not?

> Why not reposition the original header instead of making a clone?

I'm taking advantage of the browsers ability to calculate the width of the columns. Otherwise the developer would have to manually set the width of each column like many other solutions.

> Why not use a pure CSS solution?

CSS solutions often defeat the purpose of using a table in the first place. In addition, the solutions I've seen remove functionality from the table and require the developer to manually set the width of each column.
