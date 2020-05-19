
BIN := node_modules/.bin

build: node_modules build/build.js
build/build.js: index.js components
	@mkdir -p $(dir $@)
	@$(BIN)/component-build --dev

components: node_modules component.json
	@$(BIN)/component-install --dev

test: build/build.js
	$(BIN)/component-test browser

clean:
	rm -fr build components

node_modules: package.json
	@npm install
	@touch $@

.PHONY: clean test
