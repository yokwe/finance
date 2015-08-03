#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD
TICKER_LIST=QQQ-SPY-PGX
TICKER_YEAR=5

clean:
	rm -rf tmp/*

download: download-mizuho download-google

update: update-mizuho update-google


#
# MIZUHO
#
download-mizuho:
	mkdir -p tmp/fx/mizuho
	cd tmp/fx/mizuho; wget http://www.mizuhobank.co.jp/rate/market/csv/quote.csv

update-mizuho:
	mkdir -p data/fx/
	awk -f data/script/mizuho.awk -v CURRENCY_LIST="${CURRENCY_LIST}" < tmp/fx/mizuho/quote.csv >data/fx/mizuho-${CURRENCY_LIST}.csv

#
# GOOGLE
#
download-google:
	mkdir -p tmp/google
	awk -f data/script/google.awk -v TICKER_LIST="${TICKER_LIST}" -v TICKER_YEAR="${TICKER_YEAR}" >tmp/google/downlod.google.sh
	sh -f tmp/google/downlod.google.sh

