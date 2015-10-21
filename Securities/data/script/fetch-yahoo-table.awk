#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
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
