#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
}

# http://finance.yahoo.com/q?s=VCLT
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  YAHOO_SYMBOL  = $4
  GOOGLE_SYMBOL = $5
  NASDAQ_SYMBOL = $6
      
  printf("%s/%-10s http://finance.yahoo.com/q?s=%s\n",
    DIR_OUTPUT, (SYMBOL ".html"), YAHOO_SYMBOL)
}
