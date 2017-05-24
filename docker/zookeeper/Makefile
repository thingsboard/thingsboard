VERSION=k8stest
PROJECT=thingsboard
APP=zk

build:
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} .

push: build
	docker push ${PROJECT}/${APP}:${VERSION}
