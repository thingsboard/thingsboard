VERSION=1.2.4
PROJECT=thingsboard
APP=tb-postgres-schema

build:
	cp ../../dao/src/main/resources/postgres/schema.sql .
	cp ../../dao/src/main/resources/postgres/demo-data.sql .
	cp ../../dao/src/main/resources/postgres/system-data.sql .
	docker build --pull -t ${PROJECT}/${APP}:${VERSION} -t ${PROJECT}/${APP}:latest .
	rm schema.sql demo-data.sql system-data.sql

push: build
	docker push ${PROJECT}/${APP}:${VERSION}
	docker push ${PROJECT}/${APP}:latest
