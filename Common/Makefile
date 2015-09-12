#
#
#

LOGBACK_VERSION=1.1.3
SLF4J_VERSION=1.7.12
SQLITE_JDBC_VERSION=3.8.11.1
COMMONS_CSV_VERSION=1.2
HTTPCOMPONENTS_CLIENT_VERSION=4.5


all: fetch update

clean:
	rm -rf tmp/* jar/*

prepare:
	mkdir -p tmp jar

fetch: prepare fetch-logback fetch-slf4j fetch-sqlite-jdbc fetch-commons-csv fetch-httpcomponents-client

update: prepare update-logback update-slf4j update-sqlite-jdbc update-commons-csv update-httpcomponents-client


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
	    lib/httpcore-4.4.1.jar \
	    lib/commons-logging-1.2.jar \
	    ../../jar
