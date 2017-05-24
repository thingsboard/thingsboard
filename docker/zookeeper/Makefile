VERSION=1.2.4
PROJECT=thingsboard
APP=zk

build:
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} .

push: build
	docker push ${PROJECT}/${APP}:${VERSION}
