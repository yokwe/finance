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
# From mizuho
DIR_FETCH_MIZUHO		=tmp/fetch/mizuho
# From quantumonline
DIR_FETCH_QUANTUM		=tmp/fetch/quantum

DIR_SCRIPT			=data/script
DIR_DATABASE			=tmp/database

FETCH_LIST			=tmp/fetch/fetch.list


all:
	@echo all

clean:
	echo rm -rf tmp/*

prepare-log:
	sudo touch /tmp/securities.log
	sudo chmod 777 /tmp/securities.log

prepare-sqlite:
	chmod 777 tmp/sqlite tmp/sqlite/*

build-jar:
	ant build

prepare-deploy-jar:
	sudo chmod 777 /var/lib/tomcat8/webapps/
	
deploy-jar: prepare-deploy-jar
	ant deploy

update-correlation:
	ant run-update-correlation

tar:
	tar cfzv ~/Dropbox/Securities_data.taz tmp/fetch tmp/database

extract-tar:
	rm -rf tmp/fetch/* tmp/database/*
	tar xfzv ~/Dropbox/Securities_data.taz

#
# quantumonline
#
fetch-quantum:
	mkdir -p ${DIR_FETCH_QUANTUM}
	awk -v DIR_OUTPUT="${DIR_FETCH_QUANTUM}"        -f ${DIR_SCRIPT}/fetch-quantum.awk \
	  tmp/eod/stock.csv >${FETCH_LIST}
	ant run-fetch

update-quantum:
	ant run-update-quantum
	tail -n +2 ${DIR_DATABASE}/quantum-header.csv >${DIR_DATABASE}/quantum.csv

#
# eod
#
eod-stats:
	ant run-eod-update-stats
	cp tmp/eod/stats.csv ~/Dropbox/Trade/stats.csv

eod-yahoo-portfolio:
	ant run-eod-update-stock-history
	ant run-eod-update-yahoo-portfolio
	cp tmp/eod/yahoo-portfolio.csv ~/Dropbox/Trade

eod-update-stock:
	ant run-eod-update-stock
	cp tmp/eod/stock.csv ~/Dropbox/Trade

#
# sp500
#
fetch-sp500:
	mkdir -p tmp/fetch/sp500
	wget -nv -O tmp/fetch/sp500/constituents.csv            'https://github.com/datasets/s-and-p-500-companies/raw/master/data/constituents.csv'
	wget -nv -O tmp/fetch/sp500/constituents-financials.csv 'https://github.com/datasets/s-and-p-500-companies/raw/master/data/constituents-financials.csv'

#
# forex
#
fetch-forex:
	mkdir -p ${DIR_FETCH_MIZUHO}
#	rm    -f ${DIR_FETCH_MIZUHO}/*
# From https://www.mizuhobank.co.jp/market/historical.html at 2018-06-19
# Daily data from 2002-04-01
	wget  -nv -O ${DIR_FETCH_MIZUHO}/quote.csv https://www.mizuhobank.co.jp/market/csv/quote.csv

update-forex:
	ant run-eod-update-forex
	cp tmp/eod/forex.csv ~/Dropbox/Trade
