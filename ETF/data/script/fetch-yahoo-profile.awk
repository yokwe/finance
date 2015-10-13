#! /usr/bin/awk -f

# See link below for Ticker Symbol Convention of NASDAQ_SYMBOL
#  http://www.nasdaqtrader.com/trader.aspx?id=CQSsymbolconvention
# NASDAQ_SYMBOL  BAC-Y should read as BAC-PY  in yahoo finance and BAC.PRY in nasdaq
# NASDAQ_SYMBOL  BAC+A should read as BAC-WTA in yahoo finance and BAC.WSA in nasdaq

# http://finance.yahoo.com/q/pr?s=SPY
{
  EXCHANGE = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5
  
  YAHOO_SYMBOL = SYMBOL
  gsub(/-/, "-P",  YAHOO_SYMBOL)
  gsub(/+/, "-WT", YAHOO_SYMBOL)
    
  
  printf("%s/%-11s http://finance.yahoo.com/q/pr?s=%s\n", DIR_OUTPUT, (SYMBOL ".html"), YAHOO_SYMBOL)
}
