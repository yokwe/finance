#
#
#

DIR_FETCH_NASDAQ		=tmp/fetch/nasdaq

DIR_FETCH_ETF_ETF		=tmp/fetch/etf-etf
DIR_FETCH_ETF_NASDAQ_SYMBOL	=tmp/fetch/etf-nasdaq-symbol
DIR_FETCH_ETF_YAHOO_DAILY	=tmp/fetch/etf-yahoo-daily
DIR_FETCH_ETF_YAHOO_DIVIDEND	=tmp/fetch/etf-yahoo-dividen

DIR_FETCH_STOCK_NASDAQ_SYMBOL	=tmp/fetch/stock-nasdaq-symbol
DIR_FETCH_STOCK_YAHOO_DAILY	=tmp/fetch/stock-yahoo-daily
DIR_FETCH_STOCK_YAHOO_DIVIDEND	=tmp/fetch/stock-yahoo-dividen

DIR_FETCH_NASDAQ_DIVIDEND	=tmp/fetch/nasdaq-dividend
DIR_FETCH_GOOGLE_HISTORICAL	=tmp/fetch/google-historical

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

load-data-etf:
	sqlite3 < data/sqlite/etf-create.sql


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
	wc -l ${DIR_NASDAQ}/*


#
# fetch-etf-*
#

prepare-fetch-etf:
	mkdir -p tmp/fetch
	rm    -f tmp/fetch/etf-*

fetch-etf-etf:
	mkdir -p ${DIR_FETCH_ETF_ETF}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF_ETF}"            -f data/script/fetch-etf.awk \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-nasdaq-symbol:
	mkdir -p ${DIR_FETCH_ETF_NASDAQ_SYMBOL}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF_NASDAQ_SYMBOL}"  -f data/script/fetch-nasdaq.awk -v SUFFIX=""  \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-yahoo-daily:
	mkdir -p ${DIR_FETCH_ETF_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF_YAHOO_DAILY}"    -f data/script/fetch-yahoo-table.awk -v G="d" \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-yahoo-dividend:
	mkdir -p ${DIR_FETCH_ETF_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF_YAHOO_DIVIDEND}" -f data/script/fetch-yahoo-table.awk -v G="v" \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-nasdaq-dividend:
	mkdir -p ${DIR_FETCH_NASDAQ_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_DIVIDEND}"    -f data/script/fetch-nasdaq.awk  -v SUFFIX="dividend-history" \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch

fetch-google-historical:
	mkdir -p ${DIR_FETCH_GOOGLE_HISTORICAL}
	awk -v DIR_OUTPUT="${DIR_FETCH_GOOGLE_HISTORICAL}"    -f data/script/fetch-google-historical.awk \
	  tmp/nasdaq/nasdaq-all.txt >tmp/fetch.list
	ant run-fetch


#
# fetch-stock-*
#

prepare-fetch-stock:
	mkdir -p tmp/fetch
	rm    -f tmp/fetch/stock-*

fetch-stock-nasdaq-symbol:
	mkdir -p ${DIR_FETCH_STOCK_NASDAQ_SYMBOL}
	awk -v DIR_OUTPUT="${DIR_FETCH_STOCK_NASDAQ_SYMBOL}"    -f data/script/fetch-nasdaq-symbol.awk -v G="d" \
	  tmp/nasdaq/nasdaq-stock.txt >tmp/fetch.list
	ant run-fetch

fetch-stock-yahoo-daily:
	mkdir -p ${DIR_FETCH_STOCK_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_STOCK_YAHOO_DAILY}"    -f data/script/fetch-yahoo-table.awk -v G="d" \
	  tmp/nasdaq/nasdaq-stock.txt >tmp/fetch.list
	ant run-fetch

fetch-stock-yahoo-dividend:
	mkdir -p ${DIR_FETCH_STOCK_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_STOCK_YAHOO_DIVIDEND}" -f data/script/fetch-yahoo-table.awk -v G="v" \
	  tmp/nasdaq/nasdaq-stock.txt >tmp/fetch.list
	ant run-fetch

#
# update-etf-*
#

update-etf:
	@echo make update-etf-etf update-etf-yahoo-daily update-etf-yahoo-dividend

update-etf-etf:
	mkdir -p tmp/sqlite
	ant run-update-etf-etf

update-etf-yahoo-daily:
	mkdir -p tmp/sqlite
	ant run-update-etf-yahoo-daily

update-etf-yahoo-dividend:
	mkdir -p tmp/sqlite
	ant run-update-etf-yahoo-dividend


#
# update-stock-*
#

update-stock:
	@echo make update-stock-yahoo-daily update-stock-yahoo-dividend

update-stock-yahoo-daily:
	mkdir -p tmp/sqlite
	ant run-update-stock-yahoo-daily

update-stock-yahoo-dividend:
	mkdir -p tmp/sqlite
	ant run-update-stock-yahoo-dividend
