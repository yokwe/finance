#
#
#

DIR_FETCH_NASDAQ		=tmp/fetch/nasdaq

DIR_FETCH_ETF_ETF		=tmp/fetch/etf-etf
DIR_FETCH_ETF_YAHOO_DAILY	=tmp/fetch/etf-yahoo-daily
DIR_FETCH_ETF_YAHOO_DIVIDEN	=tmp/fetch/etf-yahoo-dividen
DIR_FETCH_ETF_NASDAQ_DIVIDEN	=tmp/fetch/etf-nasdaq-dividen
DIR_FETCH_ETF_NASDAQ_SYMBOL	=tmp/fetch/etf-nasdaq-symbol

DIR_FETCH_STOCK_YAHOO

DIR_NASDAQ		=tmp/nasdaq
DIR_SCRIPT		=data/script

all:
	@echo all

clean:
	rm -rf tmp/*

prepare-log:
	sudo chmod 777 /tmp/securities.log

build-jar:
	ant build

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

