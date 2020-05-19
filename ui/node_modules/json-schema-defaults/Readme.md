# JSON Schema Defaults  [![Build Status](https://travis-ci.org/chute/json-schema-defaults.svg?branch=master)](https://travis-ci.org/chute/json-schema-defaults)

> Generate JSON object from default values in JSON Schema

Works both in node and browser.

## Installation

- npm

  ```sh
  npm install json-schema-defaults
  ```

- bower

  ```sh
  bower install json-schema-defaults
  ```

- manual

  Download [lib/defaults.js](lib/defaults.js)

## Usage

- CommonJS (node.js)

  ```js
  var defaults = require('./path/to/index.js');
  defaults({ ... });
  ```

- RequireJS

  ```js
  // in require.js config
  paths: {
    'defaults': './path/to/lib/defaults.js'
  }

  // in a file
  define(['defaults'], function(defaults) {
    defaults({ ... });
  });
  ```

- standalone

  ```js
  window.jsonSchemaDefaults({ ... });
  ```

  If the standalone version causes any conflict with existing `jsonSchemaDefaults` global variable,
  you can return back the original variable:

  ```js
  var defaults = window.jsonSchemaDefaults.noConflict();
  // `window.jsonSchemaDefaults` now points to the original variable
  // `defaults` points to this script
  defaults({ ... });
  ```

## Documentation

Call `defaults` with JSON Schema. The default values will be extracted as a JSON.

```js
var json = defaults({
  "title": "Album Options",
  "type": "object",
  "properties": {
    "sort": {
      "type": "string",
      "default": "id"
    },
    "per_page": {
      "default": 30,
      "type": "integer"
    }
  }
});

// would return
{
  sort: 'id',
  per_page: 30
}
```

For more examples, see the tests.


## Development

Run tests

```sh
npm test
```

Or individually

```sh
# in node
./node_modules/.bin/jasmine-node test/

# in browser
./node_modules/karma/bin/karma start
```


## Contributors

* Eugene Tsypkin @jhony-chikens


## License

(c) 2015 Chute Corporation. Released under the terms of the MIT License.
