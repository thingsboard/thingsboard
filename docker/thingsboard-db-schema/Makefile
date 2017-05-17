VERSION=k8stest
PROJECT=thingsboard
APP=thingsboard-db-schema

build:
	cp ../../dao/src/main/resources/schema.cql .
	cp ../../dao/src/main/resources/demo-data.cql .
	cp ../../dao/src/main/resources/system-data.cql .
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} .

push: build
	docker push ${PROJECT}/${APP}:${VERSION}
	rm schema.cql demo-data.cql system-data.cql