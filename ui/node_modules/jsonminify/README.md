# About

![Travis CI](https://travis-ci.org/fkei/JSON.minify.png?branch=master)


# Feature

/*! JSON.minify()
	v0.1 (c) Kyle Simpson
	MIT License
*/

JSON.minify() minifies blocks of JSON-like content into valid JSON by removing all 
whitespace *and* comments.

JSON parsers (like JavaScript's JSON.parse() parser) generally don't consider JSON
with comments to be valid and parseable. So, the intended usage is to minify 
development-friendly JSON (with comments) to valid JSON before parsing, such as:

JSON.parse(JSON.minify(str));

Now you can maintain development-friendly JSON documents, but minify them before
parsing or before transmitting them over-the-wire.

Though comments are not officially part of the JSON standard, this post from
Douglas Crockford back in late 2005 helps explain the motivation behind this project.

http://tech.groups.yahoo.com/group/json/message/152

"A JSON encoder MUST NOT output comments. A JSON decoder MAY accept and ignore comments."

Basically, comments are not in the JSON *generation* standard, but that doesn't mean
that a parser can't be taught to ignore them. Which is exactly what JSON.minify()
is for.

The first implementation of JSON.minify() is in JavaScript, but the intent is to
port the implementation to as many other environments as possible/practical.

NOTE: As transmitting bloated (ie, with comments/whitespace) JSON would be wasteful
and silly, this JSON.minify() is intended for use in server-side processing
environments where you can strip comments/whitespace from JSON before parsing
a JSON document, or before transmitting such over-the-wire from server to browser.

# install 

## npm repo

```
$ npm install jsonminify
```

##  npm source

```
$ npm install https://github.com/fkei/JSON.minify.git
```

# example


```javascript
var jsonminify = require("jsonminify");

jsonminify('{"key":"value"/** comment **/}')
>> '{"key":"value"}'

JSON.minify('{"key":"value"/** comment **/}')
>> '{"key":"value"}'
```

# command-line

Please use here. Use JSON.minify internally.

**node-mjson** [https://github.com/fkei/node-mjson](https://github.com/fkei/node-mjson)


# build

```
$ make
```

# release

```
$ make release
```

# test

```
$ make test
```

# jshint

```
$ make jshint
```

# Document

- [JSDoc - API Document](http://fkei.github.io/JSON.minify/docs/index.html)
- [Plato - Report](http://fkei.github.io/JSON.minify/report/index.html)
- [Mocha - Test result (HTML)](http://fkei.github.io/JSON.minify/TestDoc.html)

# Web-Site

**[Github pages - JSON.minify Home Page](http://fkei.github.io/JSON.minify/)**

# LICENSE

forked from [getify/JSON.minify](https://github.com/getify/JSON.minify)

```
The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/fkei/json.minify/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

