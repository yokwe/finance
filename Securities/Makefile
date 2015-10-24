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

DIR_SQLITE			=tmp/sqlite


DIR_SCRIPT		=data/script

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
	sqlite3 < data/sqlite/table-create.sql


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
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_FINANCE}"   -f data/script/fetch-google-finance.awk \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-google-getprices-1m:
	mkdir -p ${DIR_FETCH_GOOGLE_GETPRICES}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_GETPRICES}"   -f data/script/fetch-google-getprices.awk  -v P="1M" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-google-getprices-40y:
	mkdir -p ${DIR_FETCH_GOOGLE_GETPRICES}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_GETPRICES}"   -f data/script/fetch-google-getprices.awk  -v P="40Y" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-google-historical-1m:
	mkdir -p ${DIR_FETCH_GOOGLE_HISTORICAL}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_HISTORICAL}"  -f data/script/fetch-google-historical.awk  -v P="-1 month" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch
	
fetch-google-historical-40y:
	mkdir -p ${DIR_FETCH_GOOGLE_HISTORICAL}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_HISTORICAL}"  -f data/script/fetch-google-historical.awk  -v P="-40 years" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch
	

#
# fetch-yahoo-*
#

fetch-yahoo-daily-1m:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f data/script/fetch-yahoo-table.awk -v G="d"  -v P="-1 month" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-daily-40y:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f data/script/fetch-yahoo-table.awk -v G="d"  -v P="-40 days" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend-7d:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}"     -f data/script/fetch-yahoo-table.awk -v G="v"  -v P="-7 days" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend-40y:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}"     -f data/script/fetch-yahoo-table.awk -v G="v"  -v P="-40 years" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-yahoo-finance:
	mkdir -p ${DIR_FETCH_YAHOO_FINANCE}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_FINANCE}"      -f data/script/fetch-yahoo-finance.awk \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

#
# fetch-nasdaq-*
#

fetch-nasdaq-symbol:
	mkdir -p ${DIR_FETCH_NASDAQ_SYMBOL}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_SYMBOL}"      -f data/script/fetch-nasdaq.awk  -v SUFFIX="" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch

fetch-nasdaq-dividend:
	mkdir -p ${DIR_FETCH_NASDAQ_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_DIVIDEND}"    -f data/script/fetch-nasdaq.awk  -v SUFFIX="/dividend-history" \
	  tmp/sqlite/nasdaq.csv >tmp/fetch.list
	ant run-fetch


#
# update-*
#

update:
	@echo make update-google-getprices update-yahoo-dividend update-google-finance

prepare-update:
	mkdir -p tmp/sqlite
	mkdir -p tmp/database

update-google-finance: prepare-update
	ant run-update-google-finance

update-google-getprices: prepare-update
	ant run-update-google-getprices
	sort tmp/database/google-getprices-update.csv >tmp/database/google-getprices-update-sorted.csv
	mv tmp/database/google-getprices-update-sorted.csv tmp/database/google-getprices-update.csv
	touch tmp/database/google-getprices.csv
	sort -m tmp/database/google-getprices-update.csv tmp/database/google-getprices.csv >tmp/database/google-getprices-new.csv
	mv tmp/database/google-getprices-new.csv tmp/database/google-getprices.csv
	
update-google-historical: prepare-update
	ant run-update-google-historical
	sort tmp/database/google-historical-update.csv >tmp/database/google-historical-update-sorted.csv
	mv tmp/database/google-historical-update-sorted.csv tmp/database/google-historical-update.csv
	touch tmp/database/google-historical.csv
	sort -m tmp/database/google-historical-update.csv tmp/database/google-historical.csv >tmp/database/google-historical-new.csv
	mv tmp/database/google-historical-new.csv tmp/database/google-historical.csv
	

update-yahoo-daily: prepare-update
	ant run-update-yahoo-daily

update-yahoo-dividend: prepare-update
	ant run-update-yahoo-dividend
	sort tmp/database/yahoo-dividend-update.csv >tmp/database/yahoo-dividend-update-sorted.csv
	mv tmp/database/yahoo-dividend-update-sorted.csv tmp/database/yahoo-dividend-update.csv
	touch tmp/database/yahoo-dividend.csv
	sort -m tmp/database/yahoo-dividend-update.csv tmp/database/yahoo-dividend.csv >tmp/database/yahoo-dividend-new.csv
	mv tmp/database/yahoo-dividend-new.csv tmp/database/yahoo-dividend.csv

update-yahoo-finance: prepare-update
	ant run-update-yahoo-finance
