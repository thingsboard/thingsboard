VERSION=k8test
PROJECT=thingsboard

all: build

build:
	docker build --pull -t ${PROJECT}/k8szk:${VERSION} .

push: build
	docker push ${PROJECT}/k8szk:${VERSION}

.PHONY: all build push
