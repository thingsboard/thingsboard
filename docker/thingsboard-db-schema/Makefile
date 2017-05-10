VERSION=k8stest
PROJECT=thingsboard
APP=thingsboard-db-schema

all: build

build:
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} .

push: build
	docker push ${PROJECT}/${APP}:${VERSION}

.PHONY: all build push