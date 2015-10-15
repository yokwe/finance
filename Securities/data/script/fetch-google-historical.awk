#! /usr/bin/awk -f

# http://www.google.com/finance/historical?q=NYSE:IBM&&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv

BEGIN {
  EXCH_NAME["A"] = "NYSEMKT"
  EXCH_NAME["N"] = "NYSE"
  EXCH_NAME["P"] = "NYSEARCA"
  EXCH_NAME["Q"] = "NASDAQ"
  EXCH_NAME["Z"] = "BATS"
  
  "date '+%Y'" | getline Y
}

{
  EXCH     = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5

  printf("%s/%-11s http://www.google.com/finance/historical?q=%s&startdate=Jan%%201,%%202000&enddate=Dec%%2031,%s&output=csv\n",
    DIR_OUTPUT, (SYMBOL ".csv"), (EXCH_NAME[EXCH] ":" SYMBOL), Y)
}
