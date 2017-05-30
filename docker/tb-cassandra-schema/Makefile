VERSION=1.2.4
PROJECT=thingsboard
APP=tb-cassandra-schema

build:
	cp ../../dao/src/main/resources/schema.cql .
	cp ../../dao/src/main/resources/demo-data.cql .
	cp ../../dao/src/main/resources/system-data.cql .
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} -t ${PROJECT}/${APP}:latest .
	rm schema.cql demo-data.cql system-data.cql

push: build
	docker push ${PROJECT}/${APP}:${VERSION}
    docker push ${PROJECT}/${APP}:latest