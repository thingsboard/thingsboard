VERSION=k8stest
PROJECT=thingsboard
APP=application

build:
	cp ../../application/target/thingsboard.deb .
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} .

push: build
	docker push ${PROJECT}/${APP}:${VERSION}
	rm thingsboard.deb