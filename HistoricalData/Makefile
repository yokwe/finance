#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD

COMMON_WGET_OPTIONS=-nv --wait=1 --random-wait

DIR_ETF_ETF           =tmp/fetch/etf/etf
DIR_ETF_YAHOO_DAILY   =tmp/fetch/etf/yahoo-daily
DIR_ETF_YAHOO_DIVIDEND=tmp/fetch/etf/yahoo-dividend
DIR_ETF_YAHOO_PROFILE =tmp/fetch/etf/yahoo-profile

all: fetch update

clean:
	rm -rf tmp/*

fetch: fetch-mizuho fetch-google

update: update-mizuho update-google

run-chrome:
	google-chrome --allow-file-access-from-files

build-jar:
	ant build

load-etf-data:
	sqlite3 < data/sql/etf-create.sql

#
# MIZUHO
#
fetch-mizuho:
	mkdir -p tmp/fetch/mizuho
	rm -f tmp/fetch/mizuho/quote.csv
	cd tmp/fetch/mizuho; wget ${COMMON_WGET_OPTIONS} http://www.mizuhobank.co.jp/rate/market/csv/quote.csv

update-mizuho:
	mkdir -p tmp/fx
	rm -f tmp/fx/mizuho-*
	awk -f data/script/update-mizuho.awk -v CURRENCY_LIST="${CURRENCY_LIST}" < tmp/fetch/mizuho/quote.csv


#
# NASDAQ
#
fetch-nasdaq:
	mkdir -p tmp/fetch/nasdaq
	rm -f tmp/fetch/nasdaq/*
	cd tmp/fetch/nasdaq; wget ${COMMON_WGET_OPTIONS} -r -l1 --no-parent -nH -nd -N -A 'nasdaqtraded.txt,otherlisted.txt' ftp://ftp.nasdaqtrader.com/SymbolDirectory/

store-nasdaq:
	mkdir -p tmp/nasdaq
	rm -f tmp/nasdaq/*
	awk 'BEGIN{RS="\r\n";FS="|"};$$1=="Y"&&$$6=="Y"&&$$8=="N"&&$$9=="N"{printf("%-8s %s\n", $$11, $$4)}' tmp/fetch/nasdaq/nasdaqtraded.txt | sort | uniq >tmp/nasdaq/etf-nasdaq.txt
	awk 'BEGIN{RS="\r\n";FS="|"};$$5=="Y"&&$$7=="N"{printf("%-8s %s\n", $$8, $$3)}'                      tmp/fetch/nasdaq/otherlisted.txt  | sort | uniq >tmp/nasdaq/etf-other.txt
	sort tmp/nasdaq/etf-nasdaq.txt tmp/nasdaq/etf-other.txt | sort | uniq | \
	  awk 'BEGIN{X["A"]="NYSEMKT";X["N"]="NYSE";X["P"]="NYSEARCA";X["Q"]="NASDAQ";X["Z"]="BATS"}; {printf("%-8s %s\n", $$1, X[$$2])}'      | sort | uniq >tmp/nasdaq/etf-all.txt


#
# ETF
#
fetch-etf:
	@echo make fech-etf-etf fetch-etf-yahoo-daily fetch-etf-yahoo-dividend fetch-etf-yahoo-profile

prepare-fetch-etf:
	rm    -f ${DIR_ETF_ETF}/*
	rm    -f ${DIR_ETF_YAHOO_DAILY}/*
	rm    -f ${DIR_ETF_YAHOO_DIVIDEND}/*
	rm    -f ${DIR_ETF_YAHOO_PROFILE}/*
	
fetch-etf-etf:
	mkdir -p ${DIR_ETF_ETF}
#	rm    -f ${DIR_ETF_ETF}/*
	awk -v DIR_OUTPUT="${DIR_ETF_ETF}" \
	  -f data/script/fetch-etf-etf.awk \
	  tmp/nasdaq/etf-all.txt >tmp/downloader.list
	ant run-downloader

fetch-etf-yahoo-daily:
	mkdir -p ${DIR_ETF_YAHOO_DAILY}
#	rm    -f ${DIR_ETF_YAHOO_DAILY}/*
	awk -v DIR_OUTPUT="${DIR_ETF_YAHOO_DAILY}"    -v G="d" \
	  -f data/script/fetch-etf-yahoo-table.awk \
	  tmp/nasdaq/etf-all.txt >tmp/downloader.list
	ant run-downloader

fetch-etf-yahoo-dividend:
	mkdir -p ${DIR_ETF_YAHOO_DIVIDEND}
#	rm    -f ${DIR_ETF_YAHOO_DIVIDEND}/*
	awk -v DIR_OUTPUT="${DIR_ETF_YAHOO_DIVIDEND}" -v G="v" \
	  -f data/script/fetch-etf-yahoo-table.awk \
	  tmp/nasdaq/etf-all.txt >tmp/downloader.list
	ant run-downloader

fetch-etf-yahoo-profile:
	mkdir -p ${DIR_ETF_YAHOO_PROFILE}
#	rm    -f ${DIR_ETF_YAHOO_PROFILE}/*
	awk -v DIR_OUTPUT="${DIR_ETF_YAHOO_PROFILE}" \
	  -f data/script/fetch-etf-yahoo-profile.awk \
	  tmp/nasdaq/etf-all.txt >tmp/downloader.list
	ant run-downloader


update-etf:
	@echo make run-etf-etf run-etf-yahoo-daily run-etf-yahoo-dividend run-etf-yahoo-profile

run-etf-etf:
	ant run-etf-etf

run-etf-yahoo-daily:
	ant run-etf-yahoo-daily

run-etf-yahoo-dividend:
	ant run-etf-yahoo-dividend

run-etf-yahoo-profile:
	ant run-etf-yahoo-profile


fetch-test:
	mkidr -p tmp/fetch/test
	wget -O tmp/fetch/test/QQQ-yahoo-q.html    http://finance.yahoo.com/q?s=QQQ
	wget -O tmp/fetch/test/QQQ-yahoo-qpr.html  http://finance.yahoo.com/q/pr?s=QQQ
	wget -O tmp/fetch/test/QQQ-google.html     http://www.google.com/finance?q=QQQ
	wget -O tmp/fetch/test/QQQ-etfdb.html      http://etfdb.com/etf/QQQ
	wget -O tmp/fetch/test/QQQ-etf.html        http://www.etf.com/QQQ
	wget -O tmp/fetch/test/QQQ-xtf.html        http://www.xtf.com/ETF-Ratings/QQQ
	wget -O tmp/fetch/test/QQQ-barchart.html   http://www.barchart.com/quotes/etf/QQQ
	wget -O tmp/fetch/test/QQQ-wikiinvest.html http://www.wikinvest.com/wiki/QQQ
	wget -O tmp/fetch/test/QQQ-reuters.html    http://www.reuters.com/finance/stocks/overview?symbol=QQQ
	wget -O tmp/fetch/test/QQQ-stockencyclopedia.html http://etf.stock-encyclopedia.com/QQQ.html
	wget -O tmp/fetch/test/QQQ-msn.html        http://www.msn.com/en-us/money/etfdetails?symbol=QQQ
	wget -O tmp/fetch/test/QQQ-seekingalpha.html http://seekingalpha.com/symbol/QQQ
#
	wget -O tmp/fetch/test/SPY-yahoo-qpr.html  http://finance.yahoo.com/q/pr?s=SPY
	wget -O tmp/fetch/test/SPY-msn.html        http://www.msn.com/en-us/money/etfdetails?symbol=SPY
