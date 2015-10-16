#! /usr/bin/awk -f

BEGIN {
  "date '+%Y'" | getline Y
  
  # Data for at least last 10 years
  
  # From (Y - 11)-01-01
  A = "00"
  B = "1"
  C = "2000" # SPY was started at 1993-01-29
  # To Y-12-31
  D = "30"
  E = "12"
  F = Y
  # G = "d" for daily and "v" for dividend
 }

# See link below for Ticker Symbol Convention of NASDAQ_SYMBOL
#  http://www.nasdaqtrader.com/trader.aspx?id=CQSsymbolconvention
# NASDAQ_SYMBOL  BAC-Y should read as BAC-PY  in yahoo finance and BAC.PRY in nasdaq
# NASDAQ_SYMBOL  BAC+A should read as BAC-WTA in yahoo finance and BAC.WSA in nasdaq

# http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
{
  EXCHANGE = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5
  
  YAHOO_SYMBOL = SYMBOL
  gsub(/\-/, "-P",  YAHOO_SYMBOL)
  gsub(/\+/, "-WT", YAHOO_SYMBOL)
  gsub(/\*/, "-CL", YAHOO_SYMBOL)
  gsub(/\=/, "-U",  YAHOO_SYMBOL)
  gsub(/\./, "-",   YAHOO_SYMBOL)
    
  printf("%s/%-10s http://real-chart.finance.yahoo.com/table.csv?a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&g=%s&ignore=.csv&s=%s\n",
    DIR_OUTPUT, (SYMBOL ".csv"), A, B, C, D, E, F, G, YAHOO_SYMBOL)
}
