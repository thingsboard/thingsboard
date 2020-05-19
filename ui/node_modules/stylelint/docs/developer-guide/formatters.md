# Writing formatters

A formatter is a function with the following signature:

```js
function formatter(results, returnValue) {
    return 'a string of formatted results';
}
```

Where the first argument (`results`) is an array of stylelint result objects in the form:

```js
// A stylelint result object
{
    source: 'path/to/file.css', // The filepath or PostCSS identifier like <input css 1>
    errored: true, // This is `true` if at least one rule with an "error"-level severity triggered a warning
    warnings: [ // Array of rule violation warning objects, each like the following ...
        {
            line: 3,
            column: 12,
            rule: 'block-no-empty',
            severity: 'error',
            text: 'You should not have an empty block (block-no-empty)',
        },
    ],
    deprecations: [ // Array of deprecation warning objects, each like the following ...
        {
            text: 'Feature X has been deprecated and will be removed in the next major version.',
            reference: 'https://stylelint.io/docs/feature-x.md',
        },
    ],
    invalidOptionWarnings: [ // Array of invalid option warning objects, each like the following ...
        {
            text: 'Invalid option X for rule Y',
        },
    ],
    ignored: false, // This is `true` if the file's path matches a provided ignore pattern
}
```

And the second argument (`returnValue`) is an object with one or more of the following keys:

```js
{
    errored: false, // `true` if there were any warnings with "error" severity
    needlessDisables: [ // Present if stylelint was configured with `reportNeedlessDisables: true`
        {
            source: 'path/to/file.css',
            ranges: [
                {
                    start: 10,
                    unusedRule: 'indentation',
                },
            ],
        },
    ],
    invalidScopeDisables: [ // Present if stylelint was configured with `reportInvalidScopeDisables: true`
        {
            source: 'path/to/file.css',
            ranges: [
                {
                    start: 1,
                    unusedRule: 'color-named',
                },
            ],
        },
    ],
    maxWarningsExceeded: { // Present if stylelint was configured with a `maxWarnings` count
        maxWarnings: 10,
        foundWarnings: 15,
    },
}
```

## `stylelint.formatters`

stylelint's internal formatters are exposed publicly in `stylelint.formatters`.
