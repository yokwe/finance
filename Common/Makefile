#
#
#

LOGBACK_VERSION=1.1.3
SLF4J_VERSION=1.7.12
SQLITE_JDBC_VERSION=3.8.11.1
COMMONS_CSV_VERSION=1.5
HTTPCOMPONENTS_CLIENT_VERSION=4.5.5
COMMONS_MATH3_VERSION=3.6.1
JAVAX_JSON_VERSION=1.1


all: fetch update

clean:
	rm -rf tmp/* jar/*

prepare:
	mkdir -p tmp jar

fetch: prepare fetch-logback fetch-slf4j fetch-sqlite-jdbc fetch-commons-csv fetch-httpcomponents-client fetch-commons-math3 fetch-javax-json

update: prepare update-logback update-slf4j update-sqlite-jdbc update-commons-csv update-httpcomponents-client update-commons-math3 update-javax-json


fetch-logback:
	rm -f tmp/logback-${LOGBACK_VERSION}.zip
	cd tmp; wget http://logback.qos.ch/dist/logback-${LOGBACK_VERSION}.zip

fetch-slf4j:
	rm -f tmp/slf4j-${SLF4J_VERSION}.zip
	cd tmp; wget http://www.slf4j.org/dist/slf4j-${SLF4J_VERSION}.zip

fetch-sqlite-jdbc:
	rm -f tmp/sqlite-jdbc-${SQLITE_JDBC_VERSION}.jar
	cd tmp; wget https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc-${SQLITE_JDBC_VERSION}.jar

fetch-commons-csv:
	rm -f tmp/commons-csv-${COMMONS_CSV_VERSION}-bin.zip
	cd tmp; wget http://ftp.riken.jp/net/apache//commons/csv/binaries/commons-csv-${COMMONS_CSV_VERSION}-bin.zip

fetch-httpcomponents-client:
	rm -f tmp/http-httpcomponents-${HTTPCOMPONENTS_CLIENT_VERSION}-bin.zip
	cd tmp; wget http://ftp.riken.jp/net/apache//httpcomponents/httpclient/binary/httpcomponents-client-${HTTPCOMPONENTS_CLIENT_VERSION}-bin.zip

fetch-commons-math3:
	rm -f tmp/commons-math3-${COMMONS_MATH3_VERSION}-bin.zip
	cd tmp; wget http://ftp.riken.jp/net/apache//commons/math/binaries/commons-math3-${COMMONS_MATH3_VERSION}-bin.zip

fetch-javax-json:
	rm -f tmp/javax.json-${JAVAX_JSON_VERSION}.jar
	cd tmp; wget http://repo1.maven.org/maven2/org/glassfish/javax.json/${JAVAX_JSON_VERSION}/javax.json-${JAVAX_JSON_VERSION}.jar

update-logback:
	rm -rf tmp/logback-${LOGBACK_VERSION}
	rm -rf jar/logback-*
	cd tmp; unzip logback-${LOGBACK_VERSION}.zip
	cd tmp/logback-${LOGBACK_VERSION}; cp -p logback-classic-${LOGBACK_VERSION}*.jar logback-core-${LOGBACK_VERSION}*.jar ../../jar

update-slf4j:
	rm -rf tmp/slf4j-${SLF4J_VERSION}
	rm -rf jar/slf4j-*
	cd tmp; unzip slf4j-${SLF4J_VERSION}.zip
	cd tmp/slf4j-${SLF4J_VERSION}; cp -p slf4j-api-${SLF4J_VERSION}*.jar ../../jar/

update-sqlite-jdbc:
	rm -rf jar/sqlite-jdbc-*
	cp tmp/sqlite-jdbc-* jar

update-commons-csv:
	rm -rf tmp/commons-csv-${COMMONS_CSV_VERSION}
	rm -rf jar/commons-csv-*
	cd tmp; unzip commons-csv-${COMMONS_CSV_VERSION}-bin.zip
	cd tmp/commons-csv-${COMMONS_CSV_VERSION}; cp -p commons-csv-${COMMONS_CSV_VERSION}*.jar ../../jar

update-httpcomponents-client:
	rm -rf tmp/httpcomponents-client-${HTTPCOMPONENTS_CLIENT_VERSION}
	rm -rf jar/httpcomponents-client-*
	cd tmp; unzip httpcomponents-client-${HTTPCOMPONENTS_CLIENT_VERSION}-bin.zip
	cd tmp/httpcomponents-client-${HTTPCOMPONENTS_CLIENT_VERSION}; \
	  cp -p \
	    lib/httpclient-${HTTPCOMPONENTS_CLIENT_VERSION}.jar \
	    lib/httpcore-4.4.9.jar \
	    lib/commons-logging-1.2.jar \
	    ../../jar

update-commons-math3:
	rm -rf tmp/commons-math3-${COMMONS_MATH3_VERSION}
	rm -rf jar/commons-math3-*
	cd tmp; unzip commons-math3-${COMMONS_MATH3_VERSION}-bin.zip
	cd tmp/commons-math3-${COMMONS_MATH3_VERSION}; cp -p commons-math3-${COMMONS_MATH3_VERSION}.jar ../../jar

update-javax-json:
	cp -p tmp/javax.json-${JAVAX_JSON_VERSION}.jar jar
