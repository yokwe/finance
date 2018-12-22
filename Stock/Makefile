#
#
#

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
# ods
#
save-ods:
	cp ~/Dropbox/Trade/投資活動_monex.ods     ~/Dropbox/Trade/SAVE/投資活動_monex_$$(date +%Y%m%d).ods
	cp ~/Dropbox/Trade/投資活動_firstrade.ods ~/Dropbox/Trade/SAVE/投資活動_firstrade_$$(date +%Y%m%d).ods

#
# preferred
#
update-preferred:
	ant run-update-preferred
	cp tmp/data/preferred.csv ~/Dropbox/Trade

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
