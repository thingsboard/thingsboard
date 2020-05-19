
# classes

  Cross-browser element class manipulation, utilizing the native `.classList` when possible. This is not designed to be a `.classList` polyfill.

## Installation

```
$ component install component/classes
```

## Example

```js
var classes = require('classes');
classes(el)
  .add('foo')
  .toggle('bar')
  .remove(/^item-\d+/);
```

## API

### .add(class)

  Add `class`.

### .remove(class)

  Remove `class` name or all classes matching the given regular expression.

### .toggle(class)

  Toggle `class`.

### .has(class)

  Check if `class` is present.

### .array()

  Return an array of classes.

## Test

```sh
$ make test
```

## License

  MIT
