#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD
TICKER_LIST=QQQ-SPY-PGX
TICKER_YEAR=5

all: download update

clean:
	rm -rf tmp/*

prepare:
	mkdir -p tmp/mizuho tmp/google
	
download: prepare download-mizuho download-google

update: prepare update-mizuho update-google


#
# MIZUHO
#
download-mizuho:
	mkdir -p tmp/mizuho
	rm -f tmp/mizuho/quote.csv
	cd tmp/mizuho; wget http://www.mizuhobank.co.jp/rate/market/csv/quote.csv

update-mizuho:
	mkdir -p tmp/fx
	rm -f tmp/fx/mizuho-*
	awk -f data/script/update-mizuho.awk -v CURRENCY_LIST="${CURRENCY_LIST}" < tmp/mizuho/quote.csv

#
# GOOGLE
#
download-google:
	mkdir -p tmp/google
	awk -f data/script/download-google.awk -v TICKER_LIST="${TICKER_LIST}" -v TICKER_YEAR="${TICKER_YEAR}" >tmp/google/downlod-google.sh
	sh -f tmp/google/downlod-google.sh

update-google:
	mkdir -p tmp/security
	rm -f tmp/security/google-*
	awk -f data/script/update-google.awk -v TICKER_LIST="${TICKER_LIST}"
