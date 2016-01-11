#! /usr/bin/awk -f

BEGIN {
  FS = ","
  RS = "[\r\n]+"
}

# http://www.quantumonline.com/search.cfm?tickersymbol=NRF-A&sopt=symbol
{
  ETF           = $1
  EXCH          = $2
  SYMBOL        = $3
  BASE_SYMBOL   = $4
  YAHOO_SYMBOL  = $5
  GOOGLE_SYMBOL = $6
  NASDAQ_SYMBOL = $7
  
#  if (index(SYMBOL, "-") == 0) next;
  
  printf("%s/%-10s http://www.quantumonline.com/search.cfm?sopt=symbol&tickersymbol=%s\n",
    DIR_OUTPUT, (SYMBOL ".html"), SYMBOL)
}
