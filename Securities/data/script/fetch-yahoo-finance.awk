#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
 }

# See link below for Ticker Symbol Convention of NASDAQ_SYMBOL
#  http://www.nasdaqtrader.com/trader.aspx?id=CQSsymbolconvention
# NASDAQ_SYMBOL  BAC-Y should read as BAC-PY  in yahoo finance and BAC.PRY in nasdaq
# NASDAQ_SYMBOL  BAC+A should read as BAC-WTA in yahoo finance and BAC.WSA in nasdaq

# http://finance.yahoo.com/q?s=VCLT
{
  ETF      = $1
  EXCH     = $2
  SYMBOL   = $3
  
  YAHOO_SYMBOL = SYMBOL
  gsub(/\-/, "-P",  YAHOO_SYMBOL)
  gsub(/\+/, "-WT", YAHOO_SYMBOL)
  gsub(/\*/, "-CL", YAHOO_SYMBOL)
  gsub(/\=/, "-U",  YAHOO_SYMBOL)
  gsub(/\./, "-",   YAHOO_SYMBOL)
    
  printf("%s/%-10s http://finance.yahoo.com/q?s=%s\n",
    DIR_OUTPUT, (SYMBOL ".html"), YAHOO_SYMBOL)
}
