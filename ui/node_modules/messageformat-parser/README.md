# messageformat-parser  <a href="http://travis-ci.org/messageformat/parser"><img align="right" alt="Build Status" src="https://secure.travis-ci.org/messageformat/parser.png"></a>

A [PEG.js] parser for [ICU MessageFormat] strings – part of [messageformat.js].
Outputs an AST defined by [parser.pegjs].

The generated parser function takes two parameters, first the string to be
parsed, and a second optional parameter `options`, an object with the following
possible keys:

- `cardinal` and `ordinal` – Arrays of valid plural categories for the current
  locale, used to validate plural and selectordinal keys. If these are missing
  or set to false, the full set of valid [Unicode CLDR] keys is used: `'zero',
  'one', 'two', 'few', 'many', 'other'`. To disable this check, pass in an empty
  array.

- `strictFunctionParams` – By default, function parameters are split on commas
  and trimmed, so the parameters in `{x,fn,   a,   b   }` are parsed as
  `['a','b']`. Setting `strictFunctionParams` to true will make the parser
  follow the ICU MessageFormat spec more closely, and result in a params array
  with a single element: `['   a,   b   ']`.

[ICU MessageFormat]: https://messageformat.github.io/guide/
[messageformat.js]: https://messageformat.github.io/
[parser.pegjs]: ./parser.pegjs
[PEG.js]: http://pegjs.org/
[Unicode CLDR]: http://cldr.unicode.org/index/cldr-spec/plural-rules


#### Installation

```sh
npm install messageformat-parser
```


#### Usage

```js
> var parse = require('messageformat-parser').parse;

> parse('So {wow}.')
[ 'So ', { type: 'argument', arg: 'wow' }, '.' ]

> parse('Such { thing }. { count, selectordinal, one {First} two {Second}' +
        '                  few {Third} other {#th} } word.')
[ 'Such ',
  { type: 'argument', arg: 'thing' },
  '. ',
  { type: 'selectordinal',
    arg: 'count',
    offset: 0,
    cases:
     [ { key: 'one', tokens: [ 'First' ] },
       { key: 'two', tokens: [ 'Second' ] },
       { key: 'few', tokens: [ 'Third' ] },
       { key: 'other', tokens: [ { type: 'octothorpe' }, 'th' ] } ] },
  ' word.' ]

> parse('Many{type,select,plural{ numbers}selectordinal{ counting}' +
                         'select{ choices}other{ some {type}}}.')
[ 'Many',
  { type: 'select',
    arg: 'type',
    cases:
     [ { key: 'plural', tokens: [ ' numbers' ] },
       { key: 'selectordinal', tokens: [ ' counting' ] },
       { key: 'select', tokens: [ ' choices' ] },
       { key: 'other', tokens: [ ' some',
                                 { type: 'argument', arg: 'type' } ] } ] },
  '.' ]

> parse('{Such compliance')
// SyntaxError: Expected ",", "}" or [ \t\n\r] but "c" found.

> var msg = '{words, plural, zero{No words} one{One word} other{# words}}';
> var englishKeys = { cardinal: [ 'one', 'other' ],
                      ordinal: [ 'one', 'two', 'few', 'other' ] };
> parse(msg)
[ { type: 'plural',
    arg: 'words',
    offset: 0,
    cases:
     [ { key: 'zero', tokens: [ 'No words' ] },
       { key: 'one', tokens: [ 'One word' ] },
       { key: 'other', tokens: [ { type: 'octothorpe' }, ' words' ] } ] } ]

> parse(msg, englishKeys)
// Error: Invalid key `zero` for argument `words`. Valid plural keys for this
//        locale are `one`, `other`, and explicit keys like `=0`.
```

#### Contributor License Agreement
See the [messageformat.js README][CLA] for details.

[CLA]: https://github.com/messageformat/messageformat.js#contributor-license-agreement
