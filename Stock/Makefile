#
#
#

# From quantumonline
DIR_FETCH_QUANTUM		=tmp/fetch/quantum

FETCH_LIST			=tmp/fetch/fetch.list

FILE_LOG			=/tmp/stock.log

all:
	@echo all

clean:
	echo rm -rf tmp/*

prepare-log:
	sudo touch ${FILE_LOG}
	sudo chmod 777 ${FILE_LOG}

prepare-deploy-jar:
	sudo chmod 777 /var/lib/tomcat8/webapps/
	
#
# quantumonline
#
fetch-quantum:
	mkdir -p ${DIR_FETCH_QUANTUM}
	awk -v DIR_OUTPUT="${DIR_FETCH_QUANTUM}"        -f ${DIR_SCRIPT}/fetch-quantum.awk \
	  tmp/eod/stock.csv >${FETCH_LIST}
	ant run-fetch

update-quantum:
	ant run-update-quantum
	tail -n +2 ${DIR_DATABASE}/quantum-header.csv >${DIR_DATABASE}/quantum.csv

#
# data
#
update-stock:
	ant run-update-stock
	cp tmp/data/stock.csv ~/Dropbox/Trade

update-stats:
	ant run-update-stats
	cp tmp/data/stats.csv ~/Dropbox/Trade

#
# firstrade
#
firstrade-yahoo-portfolio:
	ant run-firstrade-update-stock-history
	ant run-firstrade-update-yahoo-portfolio
	cp tmp/firstrade/yahoo-portfolio-firstrade.csv ~/Dropbox/Trade

#
# monex
#
monex-fx-tax:
	ant run-monex-update-fx-tax
	cp tmp/monex/monex-fx-tax.csv ~/Dropbox/Trade/

monex-stock-us:
	ant run-monex-update-stock-us
	cp tmp/monex/monex-stock-us.csv ~/Dropbox/Trade/

monex-yahoo-portfolio:
	ant run-monex-update-stock-history
	ant run-monex-update-yahoo-portfolio
	cp tmp/monex/yahoo-portfolio-monex.csv ~/Dropbox/Trade
