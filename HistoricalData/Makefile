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

run-chorome:
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
