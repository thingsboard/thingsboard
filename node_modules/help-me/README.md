help-me
=======

Help command for node, to use with [minimist](http://npm.im/minimist) and [commist](http://npm.im/commist).

Example
-------

```js
'use strict'

var helpMe = require('help-me')
var help = helpMe({
  // the default
  dir: path.join(path.dirname(require.main.filename), 'doc'),
  // the default
  ext: '.txt'
})

help
  .createStream(['hello']) // can support also strings
  .pipe(process.stdout)

// little helper to do the same
help.toStdout(['hello']
```

Usage with commist
------------------

[Commist](http://npm.im/commist) provide a command system for node.

```js
var commist = require('commist')()
var help = require('help-me')()

commist.register('help', help.toStdout)

commist.parse(process.argv.splice(2))
```

Acknowledgements
----------------

This project was kindly sponsored by [nearForm](http://nearform.com).

License
-------

MIT
