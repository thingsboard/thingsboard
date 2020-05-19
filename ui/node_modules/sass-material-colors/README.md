# Sass Material Colors [![Gem Version](https://badge.fury.io/rb/sass-material-colors.svg)](http://badge.fury.io/rb/sass-material-colors)

An easy way to use Google's [Material Design color palette](http://www.google.com/design/spec/style/color.html#color-color-palette) on your Sass project.

## Installation

Sass Material Colors can be included as a [Ruby Gem](#ruby-gem), a [Bower component](#bower-component), or a [Node Packaged Module (npm)](#npm).

### Ruby Gem

Add this line to your application's Gemfile:

```bash
$ gem 'sass-material-colors'
```

And then execute:

```bash
$ bundle
```

Or install it yourself as:

```bash
$ gem install sass-material-colors
```

### Bower Component

Install `sass-material-colors` as a development dependency:

```bash
$ bower install --save-dev sass-material-colors
```

### Node Packaged Module (npm)

Install `sass-material-colors` as a development dependency:

```bash
$ npm install --save-dev sass-material-colors
```

## Usage

Import the colors map + function to your project:

```sass
// Sass
@import 'sass-material-colors'
```

If you're using Bower or npm, you may need to use the relative path to the main file, e.g.:

```sass
// Sass

// Bower
@import 'bower_components/sass-material-colors/sass/sass-material-colors'

// npm
@import 'node_modules/sass-material-colors/sass/sass-material-colors'
```

By importing this file, a `$material-colors` [Sass map](http://sass-lang.com/documentation/file.SASS_REFERENCE.html#maps) will be added to your Sass project, with all the colors from Google's [palette](http://www.google.com/design/spec/style/color.html#color-color-palette), as well as a [`material-color` function](#the-material-color-function), making it easy for you to reference any color in the spec from your stylesheets.

Optionally, you can import a list of [placeholder selectors](#predefined-sass-placeholder-selectors) and/or [classes](#predefined-classes).

### The `material-color` Function

The `material-color` function allows you to easily reference any color in the `_sass-material-colors-map.scss` file in your styles:

```sass
// Sass
.my-cool-element
  color: material-color('cyan', '400')
  background: material-color('blue-grey', '600')
```

The `material-color` function takes 2 parameters:

##### `$color-name` String (quoted), Required
> Lower-case, dasherized color name from Google's [palette](http://www.google.com/design/spec/style/color.html#color-color-palette) (e.g. `'pink'`, `'amber'`, `'blue-grey'`, `'deep-orange'`, etc.)  

##### `$color-variant` String (quoted), Optional [Default value: `500`]
> Lower-case color variant number/code from Google's [palette](http://www.google.com/design/spec/style/color.html#color-color-palette) (e.g. `'300'`, `'200'`, `'a100'`, `'a400'`, etc.)

It's important for these parameters to be quoted strings, in order to maintain compatibility with [Libsass](https://github.com/sass/libsass).

### Predefined Sass Placeholder Selectors

You can include a list of [extendable](http://sass-lang.com/documentation/file.SASS_REFERENCE.html#extend) Sass [placeholder selectors](http://sass-lang.com/documentation/file.SASS_REFERENCE.html#placeholder_selectors_) in your project by importing the `sass-material-colors-placeholders` [file](sass/_sass-material-colors-placeholders.scss) into your Sass/Scss:

```sass
// Sass
@import 'sass-material-colors-placeholders'
```

This will add a `%color-...` and `%bg-color-...` [placeholder selector](http://sass-lang.com/documentation/file.SASS_REFERENCE.html#placeholder_selectors_) for each color name and variant found in Google's [palette](http://www.google.com/design/spec/style/color.html#color-color-palette) to your project, which you can then extend in your stylesheets like so:

```sass
// Sass
.my-cool-element
  @extend %color-cyan-400
  @extend %bg-color-blue-grey-600
```

### Predefined Classes

You can include a list of predefined classes in your project by importing the `sass-material-colors-classes` [file](sass/_sass-material-colors-classes.scss) into your Sass/Scss:

```sass
// Sass
@import 'sass-material-colors-classes'
```

This will add a `.color-...` and `.bg-color-...` class for each color name and variant found in Google's [palette](http://www.google.com/design/spec/style/color.html#color-color-palette)  to your stylesheets, which you can then use directly in your markup like so:

```html
<!-- HTML -->
<div class='my-cool-element color-cyan-400 bg-color-blue-grey-600'></div>
```

## TO-DO
- [x] ~~Make it bower friendly~~
- [x] ~~Make it npm friendly~~
- [ ] Create ember-cli addon
- [ ] Pre-compile `-placeholders` and `-classes` files
- [ ] Separate color (text) and background classes
- [ ] Add tests
- [ ] Add changelog

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md).

## License

See [LICENSE](LICENSE.md).

## Special Thanks

To [nilskaspersson/Google-Material-UI-Color-Palette](https://github.com/nilskaspersson/Google-Material-UI-Color-Palette) for the inspiration on using a Sass map for the colors, and a map function to retrieve them.

To [twbs/bootstrap-sass](https://github.com/twbs/bootstrap-sass) as a reference for this gem.

And to Google for their [Material Design spec](http://www.google.com/design/spec/material-design/introduction.html).
