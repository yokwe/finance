#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
}

# http://www.google.com/finance?q=NASDAQ:AMZN
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  YAHOO_SYMBOL  = $4
  GOOGLE_SYMBOL = $5
  NASDAQ_SYMBOL = $6
  
  printf("%s/%-11s http://www.google.com/finance?q=%s\n",
    DIR_OUTPUT, (SYMBOL ".html"), (EXCH ":" GOOGLE_SYMBOL))
}
