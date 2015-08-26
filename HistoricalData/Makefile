#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD
TICKER_LIST=QQQ-SPY-PGX
TICKER_YEAR=10

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
	cd tmp/fetch/mizuho; wget http://www.mizuhobank.co.jp/rate/market/csv/quote.csv

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
	cd tmp/fetch/nasdaq; wget -nv -r -l1 --no-parent -nH -nd -N -A 'nasdaqtraded.txt,otherlisted.txt' ftp://ftp.nasdaqtrader.com/SymbolDirectory/

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
fetch-etf: fetch-etf-google fetch-etf-yahoo

fetch-etf-google:
	mkdir -p tmp/fetch/etf/google
	rm -f tmp/fetch/etf/google/*
	awk '{printf("http://www.google.com/finance?q=%s:%s\n", $$2, $$1)}' tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-google.list
	cd tmp/fetch/etf/google; wget -nv -nH -nc -i ../wget-google.list
	
fetch-etf-yahoo-profile:
# name listed-exchange moningstar-category expense-ratio inception-date *net-assets fund-summary
	mkdir -p tmp/fetch/etf/yahoo
	rm -f tmp/fetch/etf/yahoo/*
	awk '{printf("http://finance.yahoo.com/q/pr?s=%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-yahoo-profile.list
	cd tmp/fetch/etf/yahoo; wget -nv -nH -nc -i ../wget-yahoo-profile.list

fetch-etf-yahoo-summary:
# name listed-exchange moningstar-category *net-asset *NAV fund-family fund-summary
	mkdir -p tmp/fetch/etf/yahoo
	rm -f tmp/fetch/etf/yahoo/*
	awk '{printf("http://finance.yahoo.com/q?s=%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-yahoo-summary.list
	cd tmp/fetch/etf/yahoo; wget -nv -nH -nc -i ../wget-yahoo-summary.list

fetch-etf-seekingalpha:
# name listed-exchange expense-ratio dividend-frequencey inception-date
	mkdir -p tmp/fetch/etf/seekingalpha
	rm -f tmp/fetch/etf/seekingalpha/*
	awk '{printf("http://seekingalpha.com/symbol/%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-seekingalpha.list
	cd tmp/fetch/etf/seekingalpha; wget -nv -nH -nc -i ../wget-seekingalpha.list

fetch-etf-msn:
# morningstar moningstar-category listed-exchange
	mkdir -p tmp/fetch/etf/msn
	rm -f tmp/fetch/etf/msn/*
	awk '{printf("http://www.msn.com/en-us/money/etfdetails?symbol=%s\n", $$1)}'         tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-msn.list
	cd tmp/fetch/etf/msn; wget -nv -nH -nc -i ../wget-msn.list
	

test:
	wget -O tmp/QQQ-yahoo-q.html    http://finance.yahoo.com/q?s=QQQ
	wget -O tmp/QQQ-yahoo-qpr.html  http://finance.yahoo.com/q/pr?s=QQQ
	wget -O tmp/QQQ-google.html     http://www.google.com/finance?q=QQQ
	wget -O tmp/QQQ-etfdb.html      http://etfdb.com/etf/QQQ
	wget -O tmp/QQQ-etf.html        http://www.etf.com/QQQ
	wget -O tmp/QQQ-xtf.html        http://www.xtf.com/ETF-Ratings/QQQ
	wget -O tmp/QQQ-barchart.html   http://www.barchart.com/quotes/etf/QQQ
	wget -O tmp/QQQ-wikiinvest.html http://www.wikinvest.com/wiki/QQQ
	wget -O tmp/QQQ-reuters.html    http://www.reuters.com/finance/stocks/overview?symbol=QQQ
	wget -O tmp/QQQ-stockencyclopedia.html http://etf.stock-encyclopedia.com/QQQ.html
	wget -O tmp/QQQ-msn.html        http://www.msn.com/en-us/money/etfdetails?symbol=QQQ
	wget -O tmp/QQQ-seekingalpha.html http://seekingalpha.com/symbol/QQQ
#
	wget -O tmp/SPY-yahoo-qpr.html  http://finance.yahoo.com/q/pr?s=SPY
	wget -O tmp/SPY-msn.html        http://www.msn.com/en-us/money/etfdetails?symbol=SPY
	