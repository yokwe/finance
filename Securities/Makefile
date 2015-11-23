#
#
#

# From google.com
DIR_FETCH_GOOGLE_GETPRICES	=tmp/fetch/google-getprices
DIR_FETCH_GOOGLE_HISTORICAL	=tmp/fetch/google-historical

# From nasdaq.com
DIR_FETCH_NASDAQ		=tmp/fetch/nasdaq

# Form yahoo.com
DIR_FETCH_YAHOO_DAILY		=tmp/fetch/yahoo-daily
DIR_FETCH_YAHOO_DIVIDEND	=tmp/fetch/yahoo-dividend

DIR_SCRIPT			=data/script
DIR_DATABASE			=tmp/database

all:
	@echo all

clean:
	echo rm -rf tmp/*

prepare-log:
	sudo chmod 777 /tmp/securities.log

prepare-sqlite:
	chmod 777 tmp/sqlite tmp/sqlite/*

build-jar:
	ant build

prepare-deploy-jar:
	sudo chmod 777 /var/lib/tomcat7/webapps/
	
deploy-jar: prepare-deploy-jar
	ant deploy

load-data:
	mkdir -p tmp/sqlite
	rm -f tmp/sqlite/securities.sqlite3
	sqlite3 < data/sqlite/table-create.sql

load-correlation:
	mkdir -p tmp/sqlite
	rm -f tmp/sqlite/correlation.sqlite3
	sqlite3 < data/sqlite/table-correlation.sql

update-price:
	mkdir -p tmp/update/price
	mkdir -p ${DIR_DATABASE}
	ant run-update-price
	cat data/price/price-????.??? ${DIR_DATABASE}/price-????.??? >${DIR_DATABASE}/price-all.csv

update-dividend:
	mkdir -p tmp/update/dividend
	mkdir -p ${DIR_DATABASE}
	ant run-update-dividend
	cat data/dividend/dividend-????.csv ${DIR_DATABASE}/dividend-????.csv >${DIR_DATABASE}/dividend-all.csv

update-correlation:
	ant run-update-correlation

tar:
	tar cfzv ~/Dropbox/Securities_data.taz tmp/fetch tmp/database

extract-tar:
	rm -rf tmp/fetch/* tmp/database/*
	tar xfzv ~/Dropbox/Securities_data.taz

#
# nasdaq
#

fetch-nasdaq:
	mkdir -p ${DIR_FETCH_NASDAQ}
	rm    -f ${DIR_FETCH_NASDAQ}/*
	wget            -nv -O ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt        ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqtraded.txt
	wget -U Firefox -nv -O ${DIR_FETCH_NASDAQ}/companies-by-region.csv 'http://www.nasdaq.com/screening/companies-by-region.aspx?&render=download'

update-nasdaq:
	ant run-update-nasdaq
	ant run-update-nasdaq-company


#
# fetch-google-*
#

fetch-google-getprices-40y:
	mkdir -p ${DIR_FETCH_GOOGLE_GETPRICES}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_GETPRICES}"   -f ${DIR_SCRIPT}/fetch-google-getprices.awk  -v P="40Y" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-google-historical-40y:
	mkdir -p ${DIR_FETCH_GOOGLE_HISTORICAL}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_HISTORICAL}"  -f ${DIR_SCRIPT}/fetch-google-historical.awk  -v P="-40 years" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch
	

#
# fetch-yahoo-*
#

fetch-yahoo-daily-40y:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="d"  -v P="-40 years" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend-40y:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}"     -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="v"  -v P="-40 years" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch


#
# update-*
#

update:
	@echo make update-google-getprices update-yahoo-dividend update-google-finance

prepare-update:
	mkdir -p ${DIR_DATABASE}

update-google-finance: prepare-update
	ant run-update-google-finance

update-google-getprices: prepare-update
	ant run-update-google-getprices
	
update-google-historical: prepare-update
	ant run-update-google-historical
	

update-yahoo-daily: prepare-update
	ant run-update-yahoo-daily

update-yahoo-dividend: prepare-update
	ant run-update-yahoo-dividend
