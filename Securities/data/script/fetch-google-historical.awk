#! /usr/bin/awk -f

# www.google.com/finance/historical?q=VCLT&startdate=Jan 1, 2015&enddate=Dec 31, 2050&output=csv

# A - NYSE MKT, N - NYSE, P - NYSE ARCA, Z - BATS

BEGIN {
  EXCH_NAME["A"] = "NYSEMKT"
  EXCH_NAME["N"] = "NYSE"
  EXCH_NAME["P"] = "NYSEARCA"
  EXCH_NAME["Q"] = "NASDAQ"
  EXCH_NAME["Z"] = "BATS"
}

{
  EXCHANGE = $1
  ETF      = $2
  CATEGORY = $3
  SIZE     = $4
  SYMBOL   = $5

  printf("%s/%-11s http://www.google.com/finance/historical?q=%s&startdate=Jan%%201,%%202000&enddate=Dec%%2031,2050&output=csv\n",
    DIR_OUTPUT, (SYMBOL ".csv"), (EXCH_NAME[EXCHANGE] ":" SYMBOL))
}
