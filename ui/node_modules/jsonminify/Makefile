__jsdoc=node_modules/.bin/jsdoc
__plato=node_modules/plato/bin/plato
__mocha=node_modules/.bin/mocha
__jshint=node_modules/jshint/bin/jshint

__prog=minify.json.js

all: jshint test

release: init jshint test-doc jsdoc report

init:
	mkdir -p report
	mkdir -p docs

jshint:
	$(__jshint) --config .jshintrc $(__prog)

test:
	$(__mocha) -r should test/test-*.js

test-doc:
	$(__mocha) -r should test/test-*.js -R doc 2>&1 > TestDoc.html

jsdoc:
	$(__jsdoc) -c .jsdoc3.json -d docs -p -r -l $(__prog)

report:
	$(__plato) -d ./report -r $(__prog)


.PHONY: all jshint test jsdoc report
