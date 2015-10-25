#
#
#

# From google.com
DIR_FETCH_GOOGLE_FINANCE	=tmp/fetch/google-finance
DIR_FETCH_GOOGLE_GETPRICES	=tmp/fetch/google-getprices
DIR_FETCH_GOOGLE_HISTORICAL	=tmp/fetch/google-historical

# From nasdaq.com
DIR_FETCH_NASDAQ		=tmp/fetch/nasdaq
DIR_FETCH_NASDAQ_DIVIDEND	=tmp/fetch/nasdaq-dividend
DIR_FETCH_NASDAQ_SYMBOL		=tmp/fetch/nasdaq-symbol

# Form yahoo.com
DIR_FETCH_YAHOO_DAILY		=tmp/fetch/yahoo-daily
DIR_FETCH_YAHOO_DIVIDEND	=tmp/fetch/yahoo-dividend
DIR_FETCH_YAHOO_FINANCE		=tmp/fetch/yahoo-finance

DIR_SCRIPT			=data/script
DIR_SQLITE			=data/sqlite
DIR_DATABASE			=tmp/database

all:
	@echo all

clean:
	echo rm -rf tmp/*

prepare-log:
	sudo chmod 777 /tmp/securities.log

build-jar:
	ant build

load-data:
	rm -f tmp/sqlite/securities.sqlite3
	sqlite3 < ${DIR_SQLITE}/table-create.sql


#
# nasdaq
#

fetch-nasdaq:
	mkdir -p ${DIR_FETCH_NASDAQ}
	rm    -f ${DIR_FETCH_NASDAQ}/*
	wget -nv -O ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqtraded.txt
	
update-nasdaq:
	ant run-update-nasdaq


#
# fetch-google-*
#

fetch-google-finance:
	mkdir -p ${DIR_FETCH_GOOGLE_FINANCE}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_FINANCE}"   -f ${DIR_SCRIPT}/fetch-google-finance.awk \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

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

fetch-yahoo-finance:
	mkdir -p ${DIR_FETCH_YAHOO_FINANCE}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_FINANCE}"      -f ${DIR_SCRIPT}/fetch-yahoo-finance.awk \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

#
# fetch-nasdaq-*
#

fetch-nasdaq-symbol:
	mkdir -p ${DIR_FETCH_NASDAQ_SYMBOL}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_SYMBOL}"      -f ${DIR_SCRIPT}/fetch-nasdaq.awk  -v SUFFIX="" \
	  ${DIR_DATABASE}/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-nasdaq-dividend:
	mkdir -p ${DIR_FETCH_NASDAQ_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_DIVIDEND}"    -f ${DIR_SCRIPT}/fetch-nasdaq.awk  -v SUFFIX="/dividend-history" \
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
	sort -m ${DIR_DATABASE}/google-getprices-update.csv ${DIR_DATABASE}/google-getprices.csv >${DIR_DATABASE}/google-getprices-new.csv
	mv ${DIR_DATABASE}/google-getprices-new.csv ${DIR_DATABASE}/google-getprices.csv
	
update-google-historical: prepare-update
	ant run-update-google-historical
	sort ${DIR_DATABASE}/google-historical-update.csv >${DIR_DATABASE}/google-historical-update-sorted.csv
	mv ${DIR_DATABASE}/google-historical-update-sorted.csv ${DIR_DATABASE}/google-historical-update.csv
	touch ${DIR_DATABASE}/google-historical.csv
	sort -m ${DIR_DATABASE}/google-historical-update.csv ${DIR_DATABASE}/google-historical.csv >${DIR_DATABASE}/google-historical-new.csv
	mv ${DIR_DATABASE}/google-historical-new.csv ${DIR_DATABASE}/google-historical.csv
	

update-yahoo-daily: prepare-update
	ant run-update-yahoo-daily
	sort ${DIR_DATABASE}/yahoo-daily-update.csv >${DIR_DATABASE}/yahoo-daily-update-sorted.csv
	mv ${DIR_DATABASE}/yahoo-daily-update-sorted.csv ${DIR_DATABASE}/yahoo-daily-update.csv
	touch ${DIR_DATABASE}/yahoo-daily.csv
	sort -m ${DIR_DATABASE}/yahoo-daily-update.csv ${DIR_DATABASE}/yahoo-daily.csv >${DIR_DATABASE}/yahoo-daily-new.csv
	mv ${DIR_DATABASE}/yahoo-daily-new.csv ${DIR_DATABASE}/yahoo-daily.csv

update-yahoo-dividend: prepare-update
	ant run-update-yahoo-dividend
	sort ${DIR_DATABASE}/yahoo-dividend-update.csv >${DIR_DATABASE}/yahoo-dividend-update-sorted.csv
	mv ${DIR_DATABASE}/yahoo-dividend-update-sorted.csv ${DIR_DATABASE}/yahoo-dividend-update.csv
	touch ${DIR_DATABASE}/yahoo-dividend.csv
	sort -m ${DIR_DATABASE}/yahoo-dividend-update.csv ${DIR_DATABASE}/yahoo-dividend.csv >${DIR_DATABASE}/yahoo-dividend-new.csv
	mv ${DIR_DATABASE}/yahoo-dividend-new.csv ${DIR_DATABASE}/yahoo-dividend.csv

update-yahoo-finance: prepare-update
	ant run-update-yahoo-finance
