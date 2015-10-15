#! /usr/bin/awk -f

# http://www.google.com/finance/getprices?q=IBM&x=NYSE&i=86400&p=15Y&f=d,c,v,o,h,l

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

  printf("%s/%-11s http://www.google.com/finance/getprices?q=%s&x=%s&i=86400&p=%dY&f=d,c,v,o,h,l\n",
    DIR_OUTPUT, (SYMBOL ".csv"), SYMBOL, EXCH_NAME[EXCH], (Y - 1999))
}
