#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
  
#  P = "-10 years"
#  P = "-10 month"
#  P = "-10 days"

  CMD = "date '+%b %e, %Y' -d '" P "'"
  CMD | getline START_DATE
  
  "date '+%b %e, %Y'" | getline END_DATE
  
  gsub(" ", "%20", START_DATE)
  gsub(" ", "%20", END_DATE)
}

# http://www.google.com/finance/historical?q=NYSE:IBM&&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  BASE_SYMBOL   = $4
  YAHOO_SYMBOL  = $5
  GOOGLE_SYMBOL = $6
  NASDAQ_SYMBOL = $7
  
  printf("%s/%-11s http://www.google.com/finance/historical?startdate=%s&enddate=%s&output=csv&q=%s\n",
    DIR_OUTPUT, (SYMBOL ".csv"), START_DATE, END_DATE, (EXCH ":" GOOGLE_SYMBOL))
}
