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

load-data:
	mkdir -p tmp/sqlite
	rm -f tmp/sqlite/securities.sqlite3
	sqlite3 < data/sqlite/table-create.sql
	chmod 777 tmp/sqlite tmp/sqlite/*

load-dividend:
	sqlite3 < data/sqlite/load-dividend.sql

load-correlation:
	mkdir -p tmp/sqlite
	rm -f tmp/sqlite/correlation.sqlite3
	sqlite3 < data/sqlite/table-correlation.sql
	chmod 777 tmp/sqlite tmp/sqlite/*

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
	wget -U Firefox -nv -O ${DIR_FETCH_NASDAQ}/companies-by-region.csv 'https://www.nasdaq.com/screening/companies-by-region.aspx?&render=download'
	
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
	make fetch-mizuho update-mizuho update-price load-data update-equityStats update-correlation tar load-correlation

prepare-update:
	rm -rf tmp/update/*/*

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

#
# mizuno
#
fetch-mizuho:
	mkdir -p ${DIR_FETCH_MIZUHO}
	rm    -f ${DIR_FETCH_MIZUHO}/*
# OLD-URL
#	wget  -nv -O ${DIR_FETCH_MIZUHO}/quote.csv http://www.mizuhobank.co.jp/rate/market/csv/quote.csv
# NEW-URL
	wget  -nv -O ${DIR_FETCH_MIZUHO}/quote.csv http://www.mizuhobank.co.jp/rate_fee/market/csv/quote.csv
	
update-mizuho:
	echo "DATE,USD,GBP,EUR,AUD,NZD" >${DIR_DATABASE}/mizuho-header.csv
	nkf   -S -w -Lu ${DIR_FETCH_MIZUHO}/quote.csv | awk -F '[/,]' '2015<=$$1{printf("%s-%02d-%02d,%.2f,%.2f,%.2f,%.2f,%.2f\n",$$1,$$2,$$3,$$4,$$5,$$6,$$12,$$13)}' >>${DIR_DATABASE}/mizuho-header.csv
	tail -n +2 ${DIR_DATABASE}/mizuho-header.csv >${DIR_DATABASE}/mizuho.csv

#
# quantumonline
#
fetch-quantum:
	mkdir -p ${DIR_FETCH_QUANTUM}
	awk -v DIR_OUTPUT="${DIR_FETCH_QUANTUM}"        -f ${DIR_SCRIPT}/fetch-quantum.awk \
	  tmp/eod/stock.csv >tmp/fetch.list
	ant run-fetch

update-quantum:
	ant run-update-quantum
	tail -n +2 ${DIR_DATABASE}/quantum-header.csv >${DIR_DATABASE}/quantum.csv

#
# equityStats
#
update-equityStats:
	ant run-equityStats
	tail -n +2 ${DIR_DATABASE}/equityStats-header.csv >${DIR_DATABASE}/equityStats.csv

#
# eod
#
eod-stats:
	ant run-eod-update-stats
	cp tmp/eod/stats.csv ~/Dropbox/Trade/stats.csv

#
# sp500
#
fetch-sp500:
	mkdir -p tmp/fetch/sp500
	wget -nv -O tmp/fetch/sp500/constituents.csv            'https://github.com/datasets/s-and-p-500-companies/raw/master/data/constituents.csv'
	wget -nv -O tmp/fetch/sp500/constituents-financials.csv 'https://github.com/datasets/s-and-p-500-companies/raw/master/data/constituents-financials.csv'

#
# stock
#

fetch-stock:
	mkdir -p ${DIR_FETCH_NASDAQ}
#	rm    -f ${DIR_FETCH_NASDAQ}/*
	wget            -nv -O ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt        ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqtraded.txt
#	wget -U Firefox -nv -O ${DIR_FETCH_NASDAQ}/companies-by-region.csv 'https://www.nasdaq.com/screening/companies-by-region.aspx?&render=download'

update-stock:
	ant run-eod-update-stock
	cp tmp/eod/stock.csv ~/Dropbox/Trade

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
