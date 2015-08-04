#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD
TICKER_LIST=QQQ-SPY-PGX
TICKER_YEAR=5

all: download update

clean:
	rm -rf tmp/*

download: download-mizuho download-google

update: update-mizuho update-google


#
# MIZUHO
#
download-mizuho:
	mkdir -p tmp/download/mizuho
	rm -f tmp/download/mizuho/quote.csv
	cd tmp/download/mizuho; wget http://www.mizuhobank.co.jp/rate/market/csv/quote.csv

update-mizuho:
	mkdir -p tmp/fx
	rm -f tmp/fx/mizuho-*
	awk -f data/script/update-mizuho.awk -v CURRENCY_LIST="${CURRENCY_LIST}" < tmp/download/mizuho/quote.csv

#
# GOOGLE
#
download-google:
	mkdir -p tmp/download/google
	awk -f data/script/download-google.awk -v TICKER_LIST="${TICKER_LIST}" -v TICKER_YEAR="${TICKER_YEAR}" >tmp/download/google/download-google.sh
	sh -f tmp/download/google/download-google.sh

update-google:
	mkdir -p tmp/security
	rm -f tmp/security/google-*
	awk -f data/script/update-google.awk -v TICKER_LIST="${TICKER_LIST}"
