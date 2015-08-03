#
#
#

LOGBACK_VERSION=1.1.3
SLF4J_VERSION=1.7.12

all: fetch update

clean:
	rm -rf tmp/*

prepare:
	mkdir -p tmp jar

fetch: prepare fetch-logback fetch-slf4j

update: prepare update-logback update-slf4j


fetch-logback:
	cd tmp; wget http://logback.qos.ch/dist/logback-${LOGBACK_VERSION}.zip

fetch-slf4j:
	cd tmp; wget http://www.slf4j.org/dist/slf4j-${SLF4J_VERSION}.zip
	
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
