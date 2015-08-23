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
	wget -O tmp/fetch/nasdaq/nasdaqlisted.txt         ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqlisted.txt
	wget -O tmp/fetch/nasdaq/otherlisted.txt          ftp://ftp.nasdaqtrader.com/SymbolDirectory/otherlisted.txt
	wget -O tmp/fetch/nasdaq/ETFList.csv -U 'Mozilla' http://www.nasdaq.com/investing/etfs/etf-finder-results.aspx?download=Yes

fetch-nasdaq-etfdata:
	mkdir -p tmp/fetch/nasdaq-etfdata
	cd tmp/fetch/nasdaq-etfdata; wget -r -l1 --no-parent -nH -nd -N -A 'nasdaq_etf_????????.txt' ftp://ftp.nasdaqtrader.com:21/ETFData/

store-nasdaq:
	mkdir -p tmp/nasdaq
	awk 'BEGIN{RS="\r\n";FS="[\",]+"};$$1=="Symbol"{next};{printf("%s\n", $$2)}'   tmp/fetch/nasdaq/ETFList.csv              | sort | uniq >tmp/nasdaq/etf-nasdaq.txt
	awk 'BEGIN{RS="\r\n";FS="|"};$$5=="Y"&&$$7=="N"{printf("%s\n", $$8)}'          tmp/fetch/nasdaq/otherlisted.txt          | sort | uniq >tmp/nasdaq/etf-other.txt
	awk 'BEGIN{FS="|";RS="\r\n"};$$2=="Symbol"||NF==1{next};{printf("%s\n", $$2)}' tmp/fetch/nasdaq-etfdata/nasdaq_etf_*.txt | sort | uniq >tmp/nasdaq/etf-trade.txt
	sort tmp/nasdaq/etf-nasdaq.txt tmp/nasdaq/etf-other.txt  tmp/nasdaq/etf-trade.txt | uniq | \
	awk 'BEGIN{RS="[\r\n]+";FS="|";X["P"]="NYSEARCA";X["Z"]="BATS"; while(0 < (getline <"tmp/fetch/nasdaq/otherlisted.txt")) {Y[$$8]=$$3}};{printf("%s %s\n", $$1, ((Y[$$1] in X) ? X[Y[$$1]] : "NASDAQ"))}' >tmp/nasdaq/etf-all.txt

#
# ETF
#
fetch-etf: fetch-etf-google fetch-etf-yahoo

fetch-etf-google:
	mkdir -p tmp/fetch/etf
	rm -f tmp/fetch/etf/finance*
	awk '{printf("http://www.google.com/finance?q=%s:%s\n", $$2, $$1)}' tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-list-google
	cd tmp/fetch/etf; wget -nv -nH -nc -i wget-list-google
	
fetch-etf-yahoo:
	mkdir -p tmp/fetch/etf
	rm -f tmp/fetch/etf/q*
	awk '{printf("http://finance.yahoo.com/q?s=%s\n", $$1)}'            tmp/nasdaq/etf-all.txt > tmp/fetch/etf/wget-list-yahoo
	cd tmp/fetch/etf; wget -nv -nH -nc -i wget-list-yahoo
	