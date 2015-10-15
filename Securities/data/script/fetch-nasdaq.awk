#! /usr/bin/awk -f

# See link below for Ticker Symbol Convention of NASDAQ_SYMBOL
#  http://www.nasdaqtrader.com/trader.aspx?id=CQSsymbolconvention
# NASDAQ_SYMBOL  BAC-Y should read as BAC-PY  in yahoo finance and BAC.PRY in nasdaq
# NASDAQ_SYMBOL  BAC+A should read as BAC-WTA in yahoo finance and BAC.WSA in nasdaq

# http://www.nasdaq.com/symbol/vclt/dividend-history
{
  EXCHANGE = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5
  
  NASDAQ_SYMBOL = SYMBOL
  gsub(/\-/, ".PR", NASDAQ_SYMBOL)
  gsub(/\+/, ".WS", NASDAQ_SYMBOL)
  gsub(/\*/, ".CL", NASDAQ_SYMBOL)
  gsub(/\=/, ".U",  NASDAQ_SYMBOL)
 
  printf("%s/%-11s http://www.nasdaq.com/symbol/%s%s\n", DIR_OUTPUT, (SYMBOL ".html"), tolower(NASDAQ_SYMBOL), SUFFIX)
}
