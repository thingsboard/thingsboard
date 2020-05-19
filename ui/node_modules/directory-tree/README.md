[![Build Status](https://travis-ci.org/mihneadb/node-directory-tree.svg)](https://travis-ci.org/mihneadb/node-directory-tree)

# directory-tree

Creates a JavaScript object representing a directory tree.

## Install

```bash
$ npm install directory-tree
```

## Usage

```js
const dirTree = require("directory-tree");
const tree = dirTree("/some/path");
```

And you can also filter by an extensions regex:
This is useful for including only certain types of files.

```js
const dirTree = require("directory-tree");
const filteredTree = dirTree("/some/path", { extensions: /\.txt/ });
```

Example for filtering multiple extensions with Regex.

```js
const dirTree = require("directory-tree");
const filteredTree = dirTree("/some/path", {
  extensions: /\.(md|js|html|java|py|rb)$/
});
```

You can also exclude paths from the tree using a regex:

```js
const dirTree = require("directory-tree");
const filteredTree = dirTree("/some/path", { exclude: /some_path_to_exclude/ });
```

You can also specify which additional attributes you would like to be included about each file/directory:

```js
const dirTree = require('directory-tree');
const filteredTree = dirTree('/some/path', {attributes:['mode', 'mtime']});
```

The default attributes are `[name, size, extension, path]` for Files and `[name, size, path]` for Directories

A callback function can be executed with each file that matches the extensions provided:

```js
const PATH = require('path');
const dirTree = require('directory-tree');

const tree = dirTree('./test/test_data', {extensions:/\.txt$/}, (item, PATH, stats) => {
	console.log(item);
});
```

The callback function takes the directory item (has path, name, size, and extension) and an instance of [node path](https://nodejs.org/api/path.html) and an instance of [node FS.stats](https://nodejs.org/api/fs.html#fs_class_fs_stats).

You can also pass a callback function for directories:
```js
const PATH = require('path');
const dirTree = require('directory-tree');

const tree = dirTree('./test/test_data', {extensions:/\.txt$/}, null, (item, PATH, stats) => {
	console.log(item);
});
```

## Options

`exclude` : `RegExp|RegExp[]` - A RegExp or an array of RegExp to test for exlusion of directories.

`extensions` : `RegExp` - A RegExp to test for exclusion of files with the matching extension.

`attributes` : `string[]` - Array of [FS.stats](https://nodejs.org/api/fs.html#fs_class_fs_stats) attributes.

`normalizePath` : `Boolean` - If true, windows style paths will be normalized to unix style pathes (/ instead of \\).

## Result

Given a directory structured like this:

```
photos
├── summer
│   └── june
│       └── windsurf.jpg
└── winter
    └── january
        ├── ski.png
        └── snowboard.jpg
```

`directory-tree` will return this JS object:

```json
{
  "path": "photos",
  "name": "photos",
  "size": 600,
  "type": "directory",
  "children": [
    {
      "path": "photos/summer",
      "name": "summer",
      "size": 400,
      "type": "directory",
      "children": [
        {
          "path": "photos/summer/june",
          "name": "june",
          "size": 400,
          "type": "directory",
          "children": [
            {
              "path": "photos/summer/june/windsurf.jpg",
              "name": "windsurf.jpg",
              "size": 400,
              "type": "file",
              "extension": ".jpg"
            }
          ]
        }
      ]
    },
    {
      "path": "photos/winter",
      "name": "winter",
      "size": 200,
      "type": "directory",
      "children": [
        {
          "path": "photos/winter/january",
          "name": "january",
          "size": 200,
          "type": "directory",
          "children": [
            {
              "path": "photos/winter/january/ski.png",
              "name": "ski.png",
              "size": 100,
              "type": "file",
              "extension": ".png"
            },
            {
              "path": "photos/winter/january/snowboard.jpg",
              "name": "snowboard.jpg",
              "size": 100,
              "type": "file",
              "extension": ".jpg"
            }
          ]
        }
      ]
    }
  ]
}
```

## Note

Device, FIFO and socket files are ignored.

Files to which the user does not have permissions are included in the directory
tree, however, directories to which the user does not have permissions, along
with all of its contained files, are completely ignored.

## Dev

To run tests go the package root in your CLI and run,

```bash
$ npm test
```

Make sure you have the dev dependencies installed (e.g. `npm install .`)

## Node version

This project requires at least Node v4.2.
Check out version `0.1.1` if you need support for older versions of Node.
