#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
  "date '+%Y'" | getline Y
}

# http://www.google.com/finance/historical?q=NYSE:IBM&&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  YAHOO_SYMBOL  = $4
  GOOGLE_SYMBOL = $5
  NASDAQ_SYMBOL = $6
  
  printf("%s/%-11s http://www.google.com/finance/historical?q=%s&startdate=Jan%%201,%%202000&enddate=Dec%%2031,%s&output=csv\n",
    DIR_OUTPUT, (SYMBOL ".csv"), (EXCH ":" GOOGLE_SYMBOL), Y)
}
