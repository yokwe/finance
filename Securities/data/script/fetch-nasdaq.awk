#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
}

# http://www.nasdaq.com/symbol/vclt/dividend-history
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  YAHOO_SYMBOL  = $4
  GOOGLE_SYMBOL = $5
  NASDAQ_SYMBOL = $6
  
  gsub(/\^/, "%5E", NASDAQ_SYMBOL)
  
  printf("%s/%-11s http://www.nasdaq.com/symbol/%s%s\n", DIR_OUTPUT, (SYMBOL ".html"), tolower(NASDAQ_SYMBOL), SUFFIX)
}
