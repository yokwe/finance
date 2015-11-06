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

download-price:
	touch tmp/download-price.log
	ant run-download-price
	cat ${DIR_DATABASE}/price-????.??? >${DIR_DATABASE}/price-all.csv

#
# nasdaq
#

fetch-nasdaq:
	mkdir -p ${DIR_FETCH_NASDAQ}
	rm    -f ${DIR_FETCH_NASDAQ}/*
	wget -nv -O ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt     ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqtraded.txt
	wget -U Firefox -nv -O ${DIR_FETCH_NASDAQ}/companies-nasdaq.csv 'http://www.nasdaq.com/screening/companies-by-industry.aspx?render=download&exchange=NASDAQ'
	wget -U Firefox -nv -O ${DIR_FETCH_NASDAQ}/companies-nyse.csv   'http://www.nasdaq.com/screening/companies-by-industry.aspx?render=download&exchange=NYSE'
	wget -U Firefox -nv -O ${DIR_FETCH_NASDAQ}/companies-amex.csv   'http://www.nasdaq.com/screening/companies-by-industry.aspx?render=download&exchange=AMEX'

update-nasdaq:
	ant run-update-nasdaq
	ant run-update-nasdaq-company


#
# fetch-google-*
#

fetch-google-getprices-1m:
	mkdir -p ${DIR_FETCH_GOOGLE_GETPRICES}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_GETPRICES}"   -f ${DIR_SCRIPT}/fetch-google-getprices.awk  -v P="1M" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-google-getprices-40y:
	mkdir -p ${DIR_FETCH_GOOGLE_GETPRICES}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_GETPRICES}"   -f ${DIR_SCRIPT}/fetch-google-getprices.awk  -v P="40Y" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-google-historical-1m:
	mkdir -p ${DIR_FETCH_GOOGLE_HISTORICAL}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_HISTORICAL}"  -f ${DIR_SCRIPT}/fetch-google-historical.awk  -v P="-1 month" \
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

fetch-yahoo-daily-1w:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="d"  -v P="-1 week" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-daily-1m:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="d"  -v P="-1 month" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-daily-40y:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="d"  -v P="-40 years" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend-1w:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}"     -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="v"  -v P="-1 week" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend-1m:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}"     -f ${DIR_SCRIPT}/fetch-yahoo-table.awk -v G="v"  -v P="-1 month" \
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
	sort ${DIR_DATABASE}/google-getprices-update.csv >${DIR_DATABASE}/google-getprices-update-sorted.csv
	mv ${DIR_DATABASE}/google-getprices-update-sorted.csv ${DIR_DATABASE}/google-getprices-update.csv
	touch ${DIR_DATABASE}/google-getprices.csv
	sort -m ${DIR_DATABASE}/google-getprices-update.csv ${DIR_DATABASE}/google-getprices.csv | uniq >${DIR_DATABASE}/google-getprices-new.csv
	wc -l ${DIR_DATABASE}/google-getprices*.csv
	mv ${DIR_DATABASE}/google-getprices-new.csv ${DIR_DATABASE}/google-getprices.csv
	
update-google-historical: prepare-update
	ant run-update-google-historical
	sort ${DIR_DATABASE}/google-historical-update.csv >${DIR_DATABASE}/google-historical-update-sorted.csv
	mv ${DIR_DATABASE}/google-historical-update-sorted.csv ${DIR_DATABASE}/google-historical-update.csv
	touch ${DIR_DATABASE}/google-historical.csv
	sort -m ${DIR_DATABASE}/google-historical-update.csv ${DIR_DATABASE}/google-historical.csv | uniq >${DIR_DATABASE}/google-historical-new.csv
	wc -l ${DIR_DATABASE}/google-historical*.csv
	mv ${DIR_DATABASE}/google-historical-new.csv ${DIR_DATABASE}/google-historical.csv
	

update-yahoo-daily: prepare-update
	ant run-update-yahoo-daily
	sort ${DIR_DATABASE}/yahoo-daily-update.csv >${DIR_DATABASE}/yahoo-daily-update-sorted.csv
	mv ${DIR_DATABASE}/yahoo-daily-update-sorted.csv ${DIR_DATABASE}/yahoo-daily-update.csv
	touch ${DIR_DATABASE}/yahoo-daily.csv
	sort -m ${DIR_DATABASE}/yahoo-daily-update.csv ${DIR_DATABASE}/yahoo-daily.csv | uniq >${DIR_DATABASE}/yahoo-daily-new.csv
	wc -l ${DIR_DATABASE}/yahoo-daily*.csv
	mv ${DIR_DATABASE}/yahoo-daily-new.csv ${DIR_DATABASE}/yahoo-daily.csv

update-yahoo-dividend: prepare-update
	ant run-update-yahoo-dividend
	sort ${DIR_DATABASE}/yahoo-dividend-update.csv >${DIR_DATABASE}/yahoo-dividend-update-sorted.csv
	mv ${DIR_DATABASE}/yahoo-dividend-update-sorted.csv ${DIR_DATABASE}/yahoo-dividend-update.csv
	touch ${DIR_DATABASE}/yahoo-dividend.csv
	sort -m ${DIR_DATABASE}/yahoo-dividend-update.csv ${DIR_DATABASE}/yahoo-dividend.csv | uniq >${DIR_DATABASE}/yahoo-dividend-new.csv
	wc -l ${DIR_DATABASE}/yahoo-dividend*.csv
	mv ${DIR_DATABASE}/yahoo-dividend-new.csv ${DIR_DATABASE}/yahoo-dividend.csv
