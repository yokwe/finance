#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
  
  "date '+%Y'" | getline Y
}

# http://www.google.com/finance/getprices?q=IBM&x=NYSE&i=86400&p=15Y&f=d,c,v,o,h,l
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  YAHOO_SYMBOL  = $4
  GOOGLE_SYMBOL = $5
  NASDAQ_SYMBOL = $6
  
  printf("%s/%-11s http://www.google.com/finance/getprices?q=%s&x=%s&i=86400&p=%dY&f=d,c,v,o,h,l\n",
    DIR_OUTPUT, (SYMBOL ".csv"), GOOGLE_SYMBOL, EXCH, (Y - 1999))
}
