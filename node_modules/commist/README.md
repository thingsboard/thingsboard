commist
=======

[![Build Status](https://travis-ci.com/mcollina/commist.svg?branch=master)](https://travis-ci.com/mcollina/commist)

Build command line application with multiple commands the easy way.
To be used with [minimist](http://npm.im/minimist).

```js
var program = require('commist')()
  , minimist = require('minimist')
  , result

result = program
  .register('abcd', function(args) {
    console.log('just do', args)
  })
  .register({ command: 'restore', equals: true }, function(args) {
    console.log('restore', args)
  })
  .register('args', function(args) {
    args = minimist(args)
    console.log('just do', args)
  })
  .register('abcde code', function(args) {
    console.log('doing something', args)
  })
  .register('another command', function(args) {
    console.log('anothering', args)
  })
  .parse(process.argv.splice(2))

if (result) {
  console.log('no command called, args', result)
}
```

When calling _commist_ programs, you can abbreviate down to three char
words. In the above example, these are valid commands:

```
node example.js abc
node example.js abc cod
node example.js anot comm
```

Moreover, little spelling mistakes are corrected too:

```
node example.js abcs cod
```

If you want that the command must be strict equals, you can register the
command with the json configuration:

```js
  program.register({ command: 'restore', strict: true }, function(args) {
    console.log('restore', args)
  })
```

Acknowledgements
----------------

This project was kindly sponsored by [nearForm](http://nearform.com).

License
-------

MIT
