# unist-util-find-all-after

[![Build][build-badge]][build]
[![Coverage][coverage-badge]][coverage]
[![Downloads][downloads-badge]][downloads]
[![Sponsors][sponsors-badge]][collective]
[![Backers][backers-badge]][collective]
[![Chat][chat-badge]][chat]
[![Size][size-badge]][size]

[**unist**][unist] utility to find nodes after another node.

## Installation

[npm][]:

```bash
npm install unist-util-find-all-after
```

## Usage

```js
var u = require('unist-builder')
var findAllAfter = require('unist-util-find-all-after')

var tree = u('tree', [
  u('leaf', 'leaf 1'),
  u('node', [u('leaf', 'leaf 2'), u('leaf', 'leaf 3')]),
  u('leaf', 'leaf 4'),
  u('node', [u('leaf', 'leaf 5')]),
  u('leaf', 'leaf 6'),
  u('void'),
  u('leaf', 'leaf 7')
])

console.log(findAllAfter(tree, 1, 'leaf'))
```

Yields:

```js
[
  { type: 'leaf', value: 'leaf 4' },
  { type: 'leaf', value: 'leaf 6' },
  { type: 'leaf', value: 'leaf 7' }
]
```

## API

### `findAllAfter(parent, node|index[, test])`

Find all children after `index` (or `node`) in `parent`, that passes `test`
(when given).

###### Parameters

*   `parent` ([`Node`][node]) — [Parent][] node
*   `node` ([`Node`][node]) — [Child][] of `parent`
*   `index` (`number`, optional) — [Index][] in `parent`
*   `test` (`Function`, `string`, `Object`, `Array`, optional)
    — See [`unist-util-is`][is]

###### Returns

[`Array.<Node>`][node] — [Child][]ren of `parent` passing `test`.

## Related

*   [`unist-util-find-after`](https://github.com/syntax-tree/unist-util-find-after)
    — Find a node after another node
*   [`unist-util-find-before`](https://github.com/syntax-tree/unist-util-find-before)
    — Find a node before another node
*   [`unist-util-find-all-before`](https://github.com/syntax-tree/unist-util-find-all-before)
    — Find all nodes before another node
*   [`unist-util-find-all-between`](https://github.com/mrzmmr/unist-util-find-all-between)
    — Find all nodes between two nodes
*   [`unist-util-find`](https://github.com/blahah/unist-util-find)
    — Find nodes matching a predicate

## Contribute

See [`contributing.md` in `syntax-tree/.github`][contributing] for ways to get
started.
See [`support.md`][support] for ways to get help.

This project has a [Code of Conduct][coc].
By interacting with this repository, organisation, or community you agree to
abide by its terms.

## License

[MIT][license] © [Titus Wormer][author]

<!-- Definitions -->

[build-badge]: https://img.shields.io/travis/syntax-tree/unist-util-find-all-after.svg

[build]: https://travis-ci.org/syntax-tree/unist-util-find-all-after

[coverage-badge]: https://img.shields.io/codecov/c/github/syntax-tree/unist-util-find-all-after.svg

[coverage]: https://codecov.io/github/syntax-tree/unist-util-find-all-after

[downloads-badge]: https://img.shields.io/npm/dm/unist-util-find-all-after.svg

[downloads]: https://www.npmjs.com/package/unist-util-find-all-after

[size-badge]: https://img.shields.io/bundlephobia/minzip/unist-util-find-all-after.svg

[size]: https://bundlephobia.com/result?p=unist-util-find-all-after

[sponsors-badge]: https://opencollective.com/unified/sponsors/badge.svg

[backers-badge]: https://opencollective.com/unified/backers/badge.svg

[collective]: https://opencollective.com/unified

[chat-badge]: https://img.shields.io/badge/join%20the%20community-on%20spectrum-7b16ff.svg

[chat]: https://spectrum.chat/unified/syntax-tree

[npm]: https://docs.npmjs.com/cli/install

[license]: license

[author]: https://wooorm.com

[unist]: https://github.com/syntax-tree/unist

[node]: https://github.com/syntax-tree/unist#node

[parent]: https://github.com/syntax-tree/unist#parent-1

[child]: https://github.com/syntax-tree/unist#child

[index]: https://github.com/syntax-tree/unist#index

[is]: https://github.com/syntax-tree/unist-util-is

[contributing]: https://github.com/syntax-tree/.github/blob/master/contributing.md

[support]: https://github.com/syntax-tree/.github/blob/master/support.md

[coc]: https://github.com/syntax-tree/.github/blob/master/code-of-conduct.md
