# w3c-blob

export a node or browserify compatible w3c blob interface.
polyfills old versions that rely on `BlobBuilder`.

```javascript
var Blob = require('w3c-blob')

var blob = new Blob([parts], {type: 'text/plain'})
```

## api

[See MDN's Blob documentation](https://developer.mozilla.org/en-US/docs/DOM/Blob).

# license

MIT
