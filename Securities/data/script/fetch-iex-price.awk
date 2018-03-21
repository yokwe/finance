#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
}

$1 == "symbol" { next }

# https://api.iextrading.com/1.0/stock/aapl/chart/5y?format=csv
{
# symbol,symbolGoogle,symbolNasdaq,symbolYahoo,exchange,etf,marketCap,country,sector,industry,name
  SYMBOL        = $1
  GOOGLE_SYMBOL = $2
  NASDAQ_SYMBOL = $3
  YAHOO_SYMBOL  = $4
  EXCH          = $5
  ETF           = $6
  
  printf("%s/%-11s https://api.iextrading.com/1.0/stock/%s/chart/%s?format=csv\n",
    DIR_OUTPUT, (SYMBOL ".csv"), SYMBOL, P)
}
