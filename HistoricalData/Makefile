#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD
TICKER_LIST=QQQ-SPY-PGX
TICKER_YEAR=10

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
# GOOGLE
#
fetch-google:
	mkdir -p tmp/fetch/google
	awk -f data/script/fetch-google.awk -v TICKER_LIST="${TICKER_LIST}" -v TICKER_YEAR="${TICKER_YEAR}" >tmp/fetch/google/fetch-google.sh
	sh -f tmp/fetch/google/fetch-google.sh

update-google:
	mkdir -p tmp/security
	rm -f tmp/security/google-*
	awk -f data/script/update-google.awk -v TICKER_LIST="${TICKER_LIST}"

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
	@echo make fech-etf-etf fetch-etf-yahoo-profile fetch-etf-yahoo-daily fetch-etf-yahoo-dividend

fetch-etf-etf:
	mkdir -p ${DIR_ETF_ETF}
	rm -f ${DIR_ETF_ETF}/*
	awk -v DIR_OUTPUT="${DIR_ETF_ETF}"           -f data/script/fetch-etf-etf.awk           tmp/nasdaq/etf-all.txt | bash 

fetch-etf-yahoo-daily:
	mkdir -p ${DIR_ETF_YAHOO_DAILY}
	rm -f ${DIR_ETF_YAHOO_DAILY}/*
	awk -v DIR_OUTPUT="${DIR_ETF_YAHOO_DAILY}"    -v G="d" -f data/script/fetch-etf-yahoo-table.awk        tmp/nasdaq/etf-all.txt | bash 

fetch-etf-yahoo-dividend:
	mkdir -p ${DIR_ETF_YAHOO_DIVIDEND}
	rm -f ${DIR_ETF_YAHOO_DIVIDEND}/*
	awk -v DIR_OUTPUT="${DIR_ETF_YAHOO_DIVIDEND}" -v G="v" -f data/script/fetch-etf-yahoo-table.awk        tmp/nasdaq/etf-all.txt | bash 

fetch-etf-yahoo-profile:
	mkdir -p ${DIR_ETF_YAHOO_PROFILE}
	rm -f ${DIR_ETF_YAHOO_PROFILE}/*
	awk -v DIR_OUTPUT="${DIR_ETF_YAHOO_PROFILE}" -f data/script/fetch-etf-yahoo-profile.awk tmp/nasdaq/etf-all.txt | bash 



#fetch-etf-google:
#	mkdir -p tmp/fetch/etf/google
#	rm -f tmp/fetch/etf/google/*
#	awk '{printf("http://www.google.com/finance?q=%s:%s\n", $$2, $$1)}' tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-google.list
#	cd tmp/fetch/etf/google; wget ${COMMON_WGET_OPTIONS} -nH -nc -i ../wget-google.list

#fetch-etf-yahoo-summary:
## name listed-exchange moningstar-category *net-asset *NAV fund-family fund-summary
#	mkdir -p tmp/fetch/etf/yahoo-summary
#	rm -f tmp/fetch/etf/yahoo-summary/*
#	awk '{printf("http://finance.yahoo.com/q?s=%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-yahoo-summary.list
#	cd tmp/fetch/etf/yahoo-summary; wget ${COMMON_WGET_OPTIONS} -nH -nc -i ../wget-yahoo-summary.list

#fetch-etf-seekingalpha:
## name listed-exchange expense-ratio dividend-frequencey inception-date
#	mkdir -p tmp/fetch/etf/seekingalpha
#	rm -f tmp/fetch/etf/seekingalpha/*
#	awk '{printf("http://seekingalpha.com/symbol/%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-seekingalpha.list
#	cd tmp/fetch/etf/seekingalpha; wget ${COMMON_WGET_OPTIONS} -nH -nc -i ../wget-seekingalpha.list

#fetch-etf-msn:
## morningstar moningstar-category listed-exchange
#	mkdir -p tmp/fetch/etf/msn
#	rm -f tmp/fetch/etf/msn/*
#	awk '{printf("http://www.msn.com/en-us/money/etfdetails?symbol=%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-msn.list
#	cd tmp/fetch/etf/msn; wget ${COMMON_WGET_OPTIONS} -nH -nc -i ../wget-msn.list

#fetch-etf-etfdb:
#	mkdir -p tmp/fetch/etf/etfdb
#	rm -f tmp/fetch/etf/etfdb/*
#	awk '{printf("http://etfdb.com/etf/%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-etfdb.list
#	cd tmp/fetch/etf/etfdb; wget ${COMMON_WGET_OPTIONS} -nH -nc -i ../wget-etfdb.list
	
#fetch-etf-xtf:
#	mkdir -p tmp/fetch/etf/xtf
#	rm -f tmp/fetch/etf/xtf/*
#	awk '{printf("http://www.xtf.com/ETF-Ratings/%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-xtf.list
#	cd tmp/fetch/etf/xtf; wget ${COMMON_WGET_OPTIONS} -nH -nc -i ../wget-xtf.list


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
