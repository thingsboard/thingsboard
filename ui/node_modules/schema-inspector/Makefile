MOCHA	=	./node_modules/mocha/bin/mocha

TIMEOUT	=	30000

test:
	@$(MOCHA) -u tdd -t $(TIMEOUT) -R spec

debug:
	@$(MOCHA) debug -u tdd -t $(TIMEOUT) -R spec

.PHONY: test debug
