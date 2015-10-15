#
#
#

# From etf.com
DIR_FETCH_ETF			=tmp/fetch/etf

# From google.com
DIR_FETCH_GOOGLE_GETPRICES	=tmp/fetch/google-getprices
DIR_FETCH_GOOGLE_HISTORICAL	=tmp/fetch/google-historical

# From nasdaq.com
DIR_FETCH_NASDAQ		=tmp/fetch/nasdaq
DIR_FETCH_NASDAQ_DIVIDEND	=tmp/fetch/nasdaq-dividend
DIR_FETCH_NASDAQ_SYMBOL		=tmp/fetch/nasdaq-symbol

# Form yahoo.com
DIR_FETCH_YAHOO_DAILY		=tmp/fetch/yahoo-daily
DIR_FETCH_YAHOO_DIVIDEND	=tmp/fetch/yahoo-dividend


DIR_NASDAQ		=tmp/nasdaq
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
	mkdir -p ${DIR_NASDAQ}
	rm    -f ${DIR_NASDAQ}/*
	awk -f ${DIR_SCRIPT}/update-nasdaqtraded.awk ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt >${DIR_NASDAQ}/nasdaq-all.txt
	awk '$$2 == "Y" {print}' ${DIR_NASDAQ}/nasdaq-all.txt >${DIR_NASDAQ}/nasdaq-etf.txt
	awk '$$2 != "Y" {print}' ${DIR_NASDAQ}/nasdaq-all.txt >${DIR_NASDAQ}/nasdaq-stock.txt
	awk -f ${DIR_SCRIPT}/update-nasdaq.awk ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt >tmp/sqlite/nasdaq.csv
	wc -l ${DIR_NASDAQ}/*


#
# fetch-etf-*
#

fetch-etf:
	mkdir -p ${DIR_FETCH_ETF}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF}"            -f data/script/fetch-etf.awk \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

#
# fetch-yahoo-*
#

fetch-yahoo-daily:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"        -f data/script/fetch-yahoo-table.awk -v G="d" \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}"     -f data/script/fetch-yahoo-table.awk -v G="v" \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch

#
# fetch-nasdaq-*
#

fetch-nasdaq-symbol:
	mkdir -p ${DIR_FETCH_NASDAQ_SYMBOL}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_SYMBOL}"      -f data/script/fetch-nasdaq.awk  -v SUFFIX="" \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch

fetch-nasdaq-dividend:
	mkdir -p ${DIR_FETCH_NASDAQ_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_DIVIDEND}"    -f data/script/fetch-nasdaq.awk  -v SUFFIX="/dividend-history" \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch

#
# fetch-google-*
#

fetch-google-historical:
	mkdir -p ${DIR_FETCH_GOOGLE_HISTORICAL}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_HISTORICAL}"  -f data/script/fetch-google-historical.awk \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch
	
fetch-google-getprices:
	mkdir -p ${DIR_FETCH_GOOGLE_GETPRICES}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_GETPRICES}"   -f data/script/fetch-google-getprices.awk \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch



#
# update-*
#

update:
	@echo make update-etf update-google-getprices update-google-historicla update-nasdaq-symbol update-nasdaq-dividend update-yahoo-daily update-yahoo-dividend

prepare-update:
	mkdir -p tmp/sqlite

update-etf: prepare-update
	mkdir -p tmp/sqlite
	ant run-update-etf

update-google-getprices: prepare-update
	ant run-update-google-prices

update-google-historical: prepare-update
	ant run-update-google-historical
	
update-nasdaq-symbol: prepare-update
	ant run-update-nasdaq-symbol
	
update-nasdaq-dividend: prepare-update
	ant run-update-nasdaq-dividend
	
update-yahoo-daily: prepare-update
	ant run-update-yahoo-daily

update-yahoo-dividend: prepare-update
	ant run-update-yahoo-dividend
