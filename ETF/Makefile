#
#
#

COMMON_WGET_OPTIONS=-nv --wait=1 --random-wait

DIR_FETCH_NASDAQ        =tmp/fetch/nasdaq
DIR_FETCH_ETF           =tmp/fetch/etf
DIR_FETCH_XTF           =tmp/fetch/xtf
DIR_FETCH_YAHOO_DAILY   =tmp/fetch/yahoo-daily
DIR_FETCH_YAHOO_DIVIDEND=tmp/fetch/yahoo-dividend
DIR_FETCH_YAHOO_PROFILE =tmp/fetch/yahoo-profile

all:
	@echo all

clean:
	rm -rf tmp/*

build-jar:
	ant build

run-fetch:
	ant run-fetch

load-data:
	sqlite3 < data/sqlite/create.sql


#
# NASDAQ
#
fetch-nasdaq:
	mkdir -p ${DIR_FETCH_NASDAQ}
	rm -f ${DIR_FETCH_NASDAQ}/*
	wget -nv -O ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqtraded.txt
	wget -nv -O ${DIR_FETCH_NASDAQ}/otherlisted.txt  ftp://ftp.nasdaqtrader.com/SymbolDirectory/otherlisted.txt

update-nasdaq:
	mkdir -p tmp/nasdaq
	rm -f tmp/nasdaq/*
	awk 'BEGIN{RS="\r\n";FS="|"};$$1=="Y"&&$$6=="Y"&&$$8=="N"&&$$9=="N"{printf("%-8s %s\n", $$11, $$4)}' ${DIR_FETCH_NASDAQ}/nasdaqtraded.txt | sort | uniq >tmp/nasdaq/etf-nasdaq.txt
	awk 'BEGIN{RS="\r\n";FS="|"};$$5=="Y"&&$$7=="N"{printf("%-8s %s\n", $$8, $$3)}'                      ${DIR_FETCH_NASDAQ}/otherlisted.txt  | sort | uniq >tmp/nasdaq/etf-other.txt
	sort tmp/nasdaq/etf-nasdaq.txt tmp/nasdaq/etf-other.txt | sort | uniq | \
	  awk 'BEGIN{X["A"]="NYSEMKT";X["N"]="NYSE";X["P"]="NYSEARCA";X["Q"]="NASDAQ";X["Z"]="BATS"}; {printf("%-8s %s\n", $$1, X[$$2])}'      | sort | uniq >tmp/nasdaq/etf-all.txt


#
# Fetch html and csv file
#

fetch:
	@echo make fetch-etf fetch-xtf fetch-yahoo-daily fetch-yahoo-dividend fetch-yahoo-profile

prepare-fetch:
	rm    -f ${DIR_FETCH_ETF}/*
	rm    -f ${DIR_FETCH_XTF}/*
	rm    -f ${DIR_FETCH_YAHOO_DAILY}/*
	rm    -f ${DIR_FETCH_YAHOO_DIVIDEND}/*
	rm    -f ${DIR_FETCH_YAHOO_PROFILE}/*
	
fetch-etf:
	mkdir -p ${DIR_FETCH_ETF}
	awk -v DIR_OUTPUT="${DIR_FETCH_ETF}"            -f data/script/fetch-etf.awk \
	  tmp/nasdaq/etf-all.txt >tmp/fetch.list
	ant run-fetch

fetch-xtf:
	mkdir -p ${DIR_FETCH_XTF}
	awk -v DIR_OUTPUT="${DIR_FETCH_XTF}"            -f data/script/fetch-xtf.awk \
	  tmp/nasdaq/etf-all.txt >tmp/fetch.list
	ant run-fetch

fetch-yahoo-profile:
	mkdir -p ${DIR_FETCH_YAHOO_PROFILE}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_PROFILE}"  -f data/script/fetch-yahoo-profile.awk \
	  tmp/nasdaq/etf-all.txt >tmp/fetch.list
	ant run-fetch

fetch-yahoo-daily:
	mkdir -p ${DIR_FETCH_YAHOO_DAILY}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DAILY}"    -f data/script/fetch-yahoo-table.awk -v G="d" \
	  tmp/nasdaq/etf-all.txt >tmp/fetch.list
	ant run-fetch

fetch-yahoo-dividend:
	mkdir -p ${DIR_FETCH_YAHOO_DIVIDEND}
	awk -v DIR_OUTPUT="${DIR_FETCH_YAHOO_DIVIDEND}" -f data/script/fetch-yahoo-table.awk -v G="v" \
	  tmp/nasdaq/etf-all.txt >tmp/fetch.list
	ant run-fetch


#
# Update csv file
#

update:
	@echo make update-etf update-yahoo-daily update-yahoo-dividend update-yahoo-profile

update-etf:
	mkdir -p tmp/sqlite
	ant run-update-etf

update-yahoo-daily:
	mkdir -p tmp/sqlite
	ant run-update-yahoo-daily

update-yahoo-dividend:
	mkdir -p tmp/sqlite
	ant run-update-yahoo-dividend

update-yahoo-profile:
	mkdir -p tmp/sqlite
	ant run-update-yahoo-profile
