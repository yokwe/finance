#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
#  P = "-10 years"
#  P = "-10 days"

  CMD = "date '+%m,%d,%Y' -d '" P "'"
  CMD | getline
  A = sprintf("%02d", $1 - 1) # Need to minus one for month number
  B = $2
  C = $3
  
  "date '+%m,%d,%Y'" | getline
  D = sprintf("%02d", $1 - 1) # Need to minus one for month number
  E = $2
  F = $3
}

# http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  YAHOO_SYMBOL  = $4
  GOOGLE_SYMBOL = $5
  NASDAQ_SYMBOL = $6
  
  printf("%s/%-10s http://real-chart.finance.yahoo.com/table.csv?a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&g=%s&ignore=.csv&s=%s\n",
    DIR_OUTPUT, (SYMBOL ".csv"), A, B, C, D, E, F, G, YAHOO_SYMBOL)
}
