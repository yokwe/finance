#
#
#

CURRENCY_LIST=USD-EUR-GBP-AUD

clean:
	rm -rf tmp/*

download: download-mizuho

update: update-mizuho

download-mizuho:
	mkdir -p tmp/fx/mizuho
	cd tmp/fx/mizuho; wget http://www.mizuhobank.co.jp/rate/market/csv/quote.csv

update-mizuho:
	mkdir -p data/fx/
	awk -f data/script/mizuho.awk -v CURRENCY_LIST="${CURRENCY_LIST}" < tmp/fx/mizuho/quote.csv >data/fx/mizuho-${CURRENCY_LIST}.csv
