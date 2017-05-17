VERSION=k8stest
PROJECT=thingsboard
APP=cassandra

build:
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} .

push: build
	docker push ${PROJECT}/${APP}:${VERSION}