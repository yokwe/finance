#
#
#

COMMON_WGET_OPTIONS=-nv --wait=1 --random-wait

DIR_FETCH_NASDAQ        =tmp/fetch/nasdaq
DIR_FETCH_NASDAQ_SYMBOL =tmp/fetch/nasdaq-symbol
DIR_FETCH_ETF           =tmp/fetch/etf
DIR_FETCH_XTF           =tmp/fetch/xtf
DIR_FETCH_YAHOO_DAILY   =tmp/fetch/yahoo-daily
DIR_FETCH_YAHOO_DIVIDEND=tmp/fetch/yahoo-dividend
DIR_FETCH_YAHOO_PROFILE =tmp/fetch/yahoo-profile

all:
	@echo all

clean:
	rm -rf tmp/*

build-jar:
	ant build

run-fetch:
	ant run-fetch

load-data:
	sqlite3 < data/sqlite/create.sql


#
# NASDAQ
#
fetch-nasdaq:
	mkdir -p ${DIR_FETCH_NASDAQ}
	rm -f ${DIR_FETCH_NASDAQ}/*
	wget -nv -O ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqtraded.txt
	wget -nv -O ${DIR_FETCH_NASDAQ}/otherlisted.txt  ftp://ftp.nasdaqtrader.com/SymbolDirectory/otherlisted.txt

update-nasdaq:
	mkdir -p tmp/nasdaq
	rm -f tmp/nasdaq/*
	awk -f data/script/update-nasdaqtraded.awk ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt >tmp/nasdaq/nasdaq-all.txt
	awk '$$2 == "Y" {print}' tmp/nasdaq/nasdaq-all.txt >tmp/nasdaq/nasdaq-etf.txt
	awk '$$2 != "Y" {print}' tmp/nasdaq/nasdaq-all.txt >tmp/nasdaq/nasdaq-stock.txt

#
# Fetch html and csv file
#

fetch:
	@echo make fetch-etf fetch-nasdaq-symbol fetch-xtf fetch-yahoo-daily fetch-yahoo-dividend fetch-yahoo-profile

prepare-fetch:
	rm    -f ${DIR_FETCH_ETF}/*
#	rm    -f ${DIR_FETCH_NASDAQ_SYMBOL}/*
#	rm    -f ${DIR_FETCH_XTF}/*
	rm    -f ${DIR_FETCH_YAHOO_DAILY}/*
	rm    -f ${DIR_FETCH_YAHOO_DIVIDEND}/*
	rm    -f ${DIR_FETCH_YAHOO_PROFILE}/*
	
fetch-etf-etf:
	mkdir -p ${DIR_FETCH_ETF}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF}"            -f data/script/fetch-etf.awk \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-nasdaq-symbol:
	mkdir -p ${DIR_FETCH_NASDAQ_SYMBOL}
	awk -v DIR_OUTPUT="${DIR_FETCH_NASDAQ_SYMBOL}"  -f data/script/fetch-nasdaq-symbol.awk \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-xtf:
	mkdir -p ${DIR_FETCH_XTF}
	awk -v DIR_OUTPUT="${DIR_FETCH_XTF}"            -f data/script/fetch-xtf.awk \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-yahoo-profile:
	mkdir -p ${DIR_FETCH_YAHOO_PROFILE}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_PROFILE}"  -f data/script/fetch-yahoo-profile.awk \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-yahoo-daily:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"    -f data/script/fetch-yahoo-table.awk -v G="d" \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch

fetch-etf-yahoo-dividend:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}" -f data/script/fetch-yahoo-table.awk -v G="v" \
	  tmp/nasdaq/nasdaq-etf.txt >tmp/fetch.list
	ant run-fetch


#
# Update csv file
#

update:
	@echo make update-etf update-yahoo-daily update-yahoo-dividend update-yahoo-profile

update-etf:
	mkdir -p tmp/sqlite
	ant run-update-etf

update-xtf:
	mkdir -p tmp/sqlite
	ant run-update-xtf

update-yahoo-daily:
	mkdir -p tmp/sqlite
	ant run-update-yahoo-daily

update-yahoo-dividend:
	mkdir -p tmp/sqlite
	ant run-update-yahoo-dividend

update-yahoo-profile:
	mkdir -p tmp/sqlite
	ant run-update-yahoo-profile
